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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ConversationMemoryService conversationMemoryService;
    private final LLMService llmService;

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

        // Add user message
        Message userMessage = new Message(chatRequest.getContent(), "user");
        conversationMemoryService.addMessageToConversation(conversationId, userMessage);

        // Get updated conversation
        Optional<Conversation> updatedConversation = conversationMemoryService.getConversation(conversationId);
        
        if (updatedConversation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Generate AI response
        String aiResponse = llmService.generateResponse(updatedConversation.get());

        // Add AI response to conversation
        Message assistantMessage = new Message(aiResponse, "assistant");
        conversationMemoryService.addMessageToConversation(conversationId, assistantMessage);

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
}
