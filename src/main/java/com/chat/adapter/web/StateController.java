package com.chat.adapter.web;

import com.chat.application.port.out.ActiveUserRepositoryPort;
import com.chat.application.port.out.ConversationMessageRepositoryPort;
import com.chat.domain.model.ChatMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/state")
public class StateController {

    private final ConversationMessageRepositoryPort messageRepository;
    private final ActiveUserRepositoryPort activeUsers;

    public StateController(ConversationMessageRepositoryPort messageRepository,
                           ActiveUserRepositoryPort activeUsers) {
        this.messageRepository = messageRepository;
        this.activeUsers = activeUsers;
    }

    @GetMapping
    public Map<String, Object> getState() {
        List<ChatMessage> messages = messageRepository.recent("public", 50);
        List<String> users = activeUsers.list();

        Map<String, Object> state = new HashMap<>();
        state.put("messages", messages);
        state.put("users", users);
        return state;
    }
}