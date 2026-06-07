package com.langchain4j.memory.service;

import com.langchain4j.memory.model.Conversation;
import com.langchain4j.memory.model.Message;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConversationMemoryService {
    
    private final MongoClient mongoClient;
    private static final String DATABASE_NAME = "langchain4j";
    private static final String COLLECTION_NAME = "conversations";

    @Autowired
    public ConversationMemoryService(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
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
        MongoCollection<Document> collection = getConversationCollection();
        Document messageDoc = convertMessageToDocument(message);
        
        collection.updateOne(
            new Document("_id", conversationId),
            new Document("$push", new Document("messages", messageDoc))
        );
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

    // Delete conversation
    public void deleteConversation(String conversationId) {
        MongoCollection<Document> collection = getConversationCollection();
        collection.deleteOne(new Document("_id", conversationId));
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