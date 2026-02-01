package com.chat.adapter.web;

import com.chat.adapter.websocket.dto.ChatMessageDto;
import com.chat.adapter.websocket.dto.requests.DirectRequest;
import com.chat.application.dto.ConversationSummary;
import com.chat.application.port.in.GetConversationHistoryQuery;
import com.chat.application.port.in.StartDirectConversationUseCase;
import com.chat.application.port.out.ActiveUserRepositoryPort;
import com.chat.application.port.out.ConversationRepositoryPort;
import com.chat.application.port.out.SessionTokenRepositoryPort;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.domain.exceptions.DomainException;
import com.chat.domain.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private static final Logger log = LoggerFactory.getLogger(ConversationController.class);

    private final SessionTokenRepositoryPort tokens;
    private final StartDirectConversationUseCase startDirect;
    private final GetConversationHistoryQuery history;
    private final ActiveUserRepositoryPort activeUsers; // Pour vérifier que le peer est en ligne
    private final ConversationRepositoryPort conversationRepository;
    private final UserRepositoryPort users;

    public ConversationController(SessionTokenRepositoryPort tokens,
                                  StartDirectConversationUseCase startDirect,
                                  GetConversationHistoryQuery history,
                                  ActiveUserRepositoryPort activeUsers,
                                  ConversationRepositoryPort conversationRepository,
                                  UserRepositoryPort users) {
        this.tokens = tokens;
        this.startDirect = startDirect;
        this.history = history;
        this.activeUsers = activeUsers;
        this.conversationRepository = conversationRepository;
        this.users = users;
    }

    @PostMapping("/direct")
    public ResponseEntity<ConversationSummary> startDirect(@RequestHeader("Authorization") String auth,
                                                           @RequestBody DirectRequest body) {

        String me;
        try {
            me = usernameFromAuth(auth);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalide");
        }

        if (body == null || body.getPeer() == null || body.getPeer().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Peer requis");
        }

        String peer = body.getPeer().trim();

        log.info("DM start request by={} peer={}", me, peer);

        if (me.equals(peer)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de discuter avec vous-même");
        }

        if (activeUsers != null && !activeUsers.contains(peer)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non connecté");
        }

        ConversationSummary summary;
        try {
            summary = startDirect.start(me, peer);
        } catch (Exception e) {
            log.error("Erreur DM me={} peer={}", me, peer, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur création DM");
        }

        return ResponseEntity.ok(summary);
    }

    @GetMapping
    public List<ConversationSummary> list(Principal principal) {
        return conversationRepository.listForUser(principal.getName());
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ChatMessageDto>> getHistory(@RequestHeader("Authorization") String auth,
                                                           @PathVariable("id") String id,
                                                           @RequestParam(value = "max", defaultValue = "100") int max) {
        String me = usernameFromAuth(auth);
        try {
            List<ChatMessage> messages = history.history(me, id, max);
            List<ChatMessageDto> out = messages.stream()
                    .map(m -> ChatMessageDto.from(id, m))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(out);
        } catch (DomainException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    private String usernameFromAuth(String auth) {
        if (auth == null || !auth.startsWith("Bearer "))
            throw new IllegalArgumentException("Unauthorized");

        String token = auth.substring(7);

        String userId = tokens.findUserIdByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Unauthorized"));

        return users.findById(userId)
                .map(u -> u.getUsername().value())
                .orElseThrow(() -> new IllegalArgumentException("Unauthorized"));
    }
}