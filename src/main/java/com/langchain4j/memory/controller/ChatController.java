package com.langchain4j.memory.controller;

import com.langchain4j.memory.model.Conversation;
import com.langchain4j.memory.model.Message;
import com.langchain4j.memory.service.ConversationMemoryService;
import com.langchain4j.memory.service.LLMService;
import com.langchain4j.memory.dto.ChatRequest;
import com.langchain4j.memory.dto.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ConversationMemoryService conversationMemoryService;
    private final LLMService llmService;
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    public ChatController(ConversationMemoryService conversationMemoryService, LLMService llmService) {
        this.conversationMemoryService = conversationMemoryService;
        this.llmService = llmService;
    }

    /**
     * Create a new conversation
     * GET /api/chat/start
     */
    @GetMapping("/start")
    public ResponseEntity<Conversation> startConversation() {
        String conversationId = UUID.randomUUID().toString();
        Conversation conversation = conversationMemoryService.createConversation(conversationId);
        return ResponseEntity.ok(conversation);
    }

    /**
     * Get conversation by ID
     * GET /api/chat/{conversationId}
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<Conversation> getConversation(@PathVariable String conversationId) {
        Optional<Conversation> conversation = conversationMemoryService.getConversation(conversationId);
        return conversation.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Add a message to conversation
     * POST /api/chat/{conversationId}/message
     */
    @PostMapping("/{conversationId}/message")
    public ResponseEntity<Conversation> addMessage(
            @PathVariable String conversationId,
            @RequestBody Message message) {
        
        Optional<Conversation> conversation = conversationMemoryService.getConversation(conversationId);
        
        if (conversation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        conversationMemoryService.addMessageToConversation(conversationId, message);
        Optional<Conversation> updatedConversation = conversationMemoryService.getConversation(conversationId);
        
        return updatedConversation.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Chat with AI (sends message and gets AI response)
     * POST /api/chat/{conversationId}/ai-chat
     */
    @PostMapping("/{conversationId}/ai-chat")
    public ResponseEntity<ChatResponse> aiChat(
            @PathVariable String conversationId,
            @RequestBody ChatRequest chatRequest) {
        
        Optional<Conversation> conversation = conversationMemoryService.getConversation(conversationId);
        
        if (conversation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        log.debug("aiChat called for conversationId='{}' content='{}'", conversationId, chatRequest.getContent());
        // Add user message
        Message userMessage = new Message(chatRequest.getContent(), "user");
        conversationMemoryService.addMessageToConversation(conversationId, userMessage);

        // Get updated conversation
        Optional<Conversation> updatedConversation = conversationMemoryService.getConversation(conversationId);
        
        if (updatedConversation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // First: try vector-based lookup (approximate semantic match)
        java.util.Optional<String> cachedResponse = conversationMemoryService.findNearestResponseForQuery(chatRequest.getContent(), 0.80);
        // Fallback: exact-match lookup
        if (cachedResponse.isEmpty()) {
            cachedResponse = conversationMemoryService.findResponseForQuery(chatRequest.getContent());
        }

        String aiResponse;
        if (cachedResponse.isPresent()) {
            aiResponse = cachedResponse.get();
            log.debug("Using cached response for conversationId='{}'", conversationId);
        } else {
            log.debug("No cached response; calling LLM for conversationId='{}'", conversationId);
            // Generate AI response
            aiResponse = llmService.generateResponse(updatedConversation.get());
        }

        // Add AI response to conversation
        Message assistantMessage = new Message(aiResponse, "assistant");
        conversationMemoryService.addMessageToConversation(conversationId, assistantMessage);
        log.debug("Appended assistant message to conversationId='{}' ({} chars)", conversationId, aiResponse == null ? 0 : aiResponse.length());

        // Get final conversation with both messages
        Optional<Conversation> finalConversation = conversationMemoryService.getConversation(conversationId);

        ChatResponse response = new ChatResponse(
            conversationId,
            chatRequest.getContent(),
            aiResponse,
            finalConversation.orElse(null)
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get all conversations
     * GET /api/chat/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<Conversation>> getAllConversations() {
        List<Conversation> conversations = conversationMemoryService.getAllConversations();
        return ResponseEntity.ok(conversations);
    }

    /**
     * Delete a conversation
     * DELETE /api/chat/{conversationId}
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(@PathVariable String conversationId) {
        conversationMemoryService.deleteConversation(conversationId);
        return ResponseEntity.ok().build();
    }

    /**
     * Health check endpoint
     * GET /api/chat/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat service is running");
    }

    /**
     * Reindex embeddings for existing assistant messages
     * POST /api/chat/reindex
     */
    @PostMapping("/reindex")
    public ResponseEntity<String> reindexEmbeddings() {
        try {
            conversationMemoryService.reindexEmbeddings();
            return ResponseEntity.ok("Reindex started/completed");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Reindex failed: " + e.getMessage());
        }
    }
}
