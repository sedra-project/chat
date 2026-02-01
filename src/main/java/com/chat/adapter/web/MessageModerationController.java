package com.chat.adapter.web;

import com.chat.adapter.websocket.dto.ChatMessageDto;
import com.chat.application.port.in.DeleteMessageUseCase;
import com.chat.application.port.in.EditMessageUseCase;
import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageModerationController {

    private final EditMessageUseCase edit;
    private final DeleteMessageUseCase delete;
    private final SessionTokenRepositoryPort tokens;
    private final UserRepositoryPort users;

    public MessageModerationController(EditMessageUseCase edit,
                                       DeleteMessageUseCase delete,
                                       SessionTokenRepositoryPort tokens,
                                       UserRepositoryPort users) {
        this.edit = edit;
        this.delete = delete;
        this.tokens = tokens;
        this.users = users;
    }

    @PatchMapping("/{conversationId}/{messageId}")
    public ChatMessageDto edit(@RequestHeader("Authorization") String auth,
                               @PathVariable String conversationId,
                               @PathVariable String messageId,
                               @RequestBody Map<String, String> body) {

        String userId = userIdFromAuth(auth);
        String content = body.get("content");
        return ChatMessageDto.from(conversationId, edit.edit(conversationId, messageId, userId, content));
    }

    @DeleteMapping("/{conversationId}/{messageId}")
    public void delete(@RequestHeader("Authorization") String auth,
                       @PathVariable String conversationId,
                       @PathVariable String messageId) {
        String userId = userIdFromAuth(auth);
        delete.delete(conversationId, messageId, userId);
    }

    private String userIdFromAuth(String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String token = auth.substring(7);
        return tokens.findUserIdByToken(token).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
