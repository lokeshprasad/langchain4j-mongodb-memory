package com.langchain4j.memory.service;

import com.langchain4j.memory.model.Conversation;
import com.langchain4j.memory.model.Message;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Collections;

@Service
public class ConversationMemoryService {
    
    private final MongoClient mongoClient;
    private final EmbeddingService embeddingService;
    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);
    private static final String DATABASE_NAME = "langchain4j";
    private static final String COLLECTION_NAME = "conversations";

    @Autowired
    public ConversationMemoryService(MongoClient mongoClient, EmbeddingService embeddingService) {
        this.mongoClient = mongoClient;
        this.embeddingService = embeddingService;
    }

    private MongoCollection<Document> getConversationCollection() {
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);
        return database.getCollection(COLLECTION_NAME);
    }

    // Create a new conversation
    public Conversation createConversation(String conversationId) {
        Conversation conversation = new Conversation(conversationId);
        saveConversation(conversation);
        return conversation;
    }

    // Save or update conversation
    public void saveConversation(Conversation conversation) {
        MongoCollection<Document> collection = getConversationCollection();
        Document doc = convertConversationToDocument(conversation);
        collection.insertOne(doc);
    }

    // Get conversation by ID
    public Optional<Conversation> getConversation(String conversationId) {
        MongoCollection<Document> collection = getConversationCollection();
        Document doc = collection.find(new Document("_id", conversationId)).first();
        
        if (doc != null) {
            return Optional.of(convertDocumentToConversation(doc));
        }
        return Optional.empty();
    }

    // Add message to conversation
    public void addMessageToConversation(String conversationId, Message message) {
        log.debug("addMessageToConversation: conversationId='{}' sender='{}' content='{}'", conversationId, message.getSender(), message.getContent());
        MongoCollection<Document> collection = getConversationCollection();
        Document messageDoc = convertMessageToDocument(message);
        
        collection.updateOne(
            new Document("_id", conversationId),
            new Document("$push", new Document("messages", messageDoc))
        );
    }

    /**
     * Find the best-matching assistant response for the given query using vector similarity.
     * Uses embeddings stored with assistant messages and a simple cosine similarity.
     */
    public Optional<String> findNearestResponseForQuery(String query, double similarityThreshold) {
        log.debug("findNearestResponseForQuery called for query='{}' threshold={}", query, similarityThreshold);
        if (query == null) return Optional.empty();

        double[] queryEmbedding = embeddingService.embed(query);
        MongoCollection<Document> collection = getConversationCollection();

        String bestResponse = null;
        double bestScore = -1.0;

        for (Document doc : collection.find()) {
            @SuppressWarnings("unchecked")
            List<Document> messagesDocs = (List<Document>) doc.get("messages");
            if (messagesDocs == null) continue;

            for (Document msgDoc : messagesDocs) {
                if (msgDoc == null) continue;
                String sender = msgDoc.getString("sender");
                if (!"assistant".equalsIgnoreCase(sender)) continue;

                @SuppressWarnings("unchecked")
                List<Double> embList = (List<Double>) msgDoc.get("embedding");
                if (embList == null || embList.isEmpty()) continue;

                double[] emb = new double[embList.size()];
                for (int i = 0; i < embList.size(); i++) emb[i] = embList.get(i);

                double score = EmbeddingUtil.cosineSimilarity(queryEmbedding, emb);
                log.debug("Scored assistant message in conversation {} index {} => score={}", doc.getString("_id"), 0, score);
                if (score > bestScore) {
                    bestScore = score;
                    bestResponse = msgDoc.getString("content");
                }
            }
        }

        log.debug("findNearestResponseForQuery bestScore={} bestResponsePresent={}", bestScore, bestResponse != null);
        if (bestResponse != null && bestScore >= similarityThreshold) {
            return Optional.of(bestResponse);
        }

        return Optional.empty();
    }

    // Get all conversations
    public List<Conversation> getAllConversations() {
        MongoCollection<Document> collection = getConversationCollection();
        List<Conversation> conversations = new ArrayList<>();
        
        collection.find().forEach(doc -> 
            conversations.add(convertDocumentToConversation(doc))
        );
        
        return conversations;
    }

    /**
     * Find a previously-assistant response for an identical user query stored in memory.
     * Searches all conversations for a user message that matches the given query (trimmed,
     * case-insensitive) and returns the next assistant message in that conversation if any.
     */
    public Optional<String> findResponseForQuery(String query) {
        log.debug("findResponseForQuery called for query='{}'", query);
        if (query == null) return Optional.empty();

        String normalizedQuery = query.trim().toLowerCase();

        MongoCollection<Document> collection = getConversationCollection();
        for (Document doc : collection.find()) {
            @SuppressWarnings("unchecked")
            List<Document> messagesDocs = (List<Document>) doc.get("messages");
            if (messagesDocs == null) continue;

            for (int i = 0; i < messagesDocs.size(); i++) {
                Document msgDoc = messagesDocs.get(i);
                String sender = msgDoc.getString("sender");
                String content = msgDoc.getString("content");
                if (sender == null || content == null) continue;

                if ("user".equalsIgnoreCase(sender) && content.trim().toLowerCase().equals(normalizedQuery)) {
                    // look for the next assistant message in the same conversation
                    for (int j = i + 1; j < messagesDocs.size(); j++) {
                        Document next = messagesDocs.get(j);
                        if (next == null) continue;
                        String nextSender = next.getString("sender");
                        if ("assistant".equalsIgnoreCase(nextSender)) {
                            String resp = next.getString("content");
                            if (resp != null) {
                                log.debug("Exact-match cached response found in conversation {}", doc.getString("_id"));
                                return Optional.of(resp);
                            }
                        }
                    }
                }
            }
        }
        log.debug("No exact-match cached response found for query='{}'", query);
        return Optional.empty();
    }

    // Delete conversation
    public void deleteConversation(String conversationId) {
        MongoCollection<Document> collection = getConversationCollection();
        collection.deleteOne(new Document("_id", conversationId));
    }

    /**
     * Recompute and store embeddings for assistant messages that lack them.
     * Useful to backfill embeddings for existing conversations.
     */
    public void reindexEmbeddings() {
        MongoCollection<Document> collection = getConversationCollection();

        for (Document doc : collection.find()) {
            @SuppressWarnings("unchecked")
            List<Document> messagesDocs = (List<Document>) doc.get("messages");
            if (messagesDocs == null) continue;

            boolean updated = false;
            log.debug("Reindexing conversation {} ({} messages)", doc.getString("_id"), messagesDocs.size());
            for (Document msgDoc : messagesDocs) {
                if (msgDoc == null) continue;
                String sender = msgDoc.getString("sender");
                if (!"assistant".equalsIgnoreCase(sender)) continue;
                if (msgDoc.containsKey("embedding") && msgDoc.get("embedding") != null) continue;

                String content = msgDoc.getString("content");
                double[] embedding = embeddingService.embed(content);
                List<Double> embList = new ArrayList<>(embedding.length);
                for (double v : embedding) embList.add(v);
                msgDoc.append("embedding", embList);
                updated = true;
            }

            if (updated) {
                log.debug("Updating embeddings for conversation {}", doc.getString("_id"));
                collection.updateOne(new Document("_id", doc.getString("_id")), new Document("$set", new Document("messages", messagesDocs)));
            }
        }
    }

    // Helper methods for document conversion
    private Document convertConversationToDocument(Conversation conversation) {
        Document doc = new Document();
        doc.append("_id", conversation.getId());
        doc.append("messages", conversation.getMessages().stream()
            .map(this::convertMessageToDocument)
            .toList());
        doc.append("createdAt", conversation.getCreatedAt());
        return doc;
    }

    private Document convertMessageToDocument(Message message) {
        Document doc = new Document();
        doc.append("content", message.getContent());
        doc.append("sender", message.getSender());
        doc.append("timestamp", message.getTimestamp());
        // if this is an assistant message, compute and store an embedding for vector search
        if (message.getSender() != null && message.getSender().equalsIgnoreCase("assistant")) {
            double[] embedding = embeddingService.embed(message.getContent());
            List<Double> embList = new ArrayList<>(embedding.length);
            for (double v : embedding) embList.add(v);
            doc.append("embedding", embList);
        }
        return doc;
    }

    private Conversation convertDocumentToConversation(Document doc) {
        Conversation conversation = new Conversation(doc.getString("_id"));
        
        @SuppressWarnings("unchecked")
        List<Document> messagesDocs = (List<Document>) doc.get("messages");
        if (messagesDocs != null) {
            messagesDocs.forEach(msgDoc -> {
                Message message = new Message();
                message.setContent(msgDoc.getString("content"));
                message.setSender(msgDoc.getString("sender"));
                message.setTimestamp(msgDoc.getLong("timestamp"));
                conversation.addMessage(message);
            });
        }
        
        if (doc.containsKey("createdAt")) {
            conversation.setCreatedAt(doc.getLong("createdAt"));
        }
        
        return conversation;
    }
}