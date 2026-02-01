package com.chat.adapter.web;

import com.chat.adapter.web.dto.*;
import com.chat.application.port.in.RequestPasswordResetUseCase;
import com.chat.application.port.in.ResetPasswordUseCase;
import com.chat.application.port.out.UserRepositoryPort;
import com.chat.application.usecase.AuthenticationService;
import com.chat.domain.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authService;
    private final UserRepositoryPort users;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public AuthenticationController(AuthenticationService authService,
                                    UserRepositoryPort users,
                                    RequestPasswordResetUseCase requestPasswordResetUseCase,
                                    ResetPasswordUseCase resetPasswordUseCase) {
        this.authService = authService;
        this.users = users;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid AuthenticationRequest req) {
        try {
            String token = authService.register(req.getUsername(), req.getEmail(), req.getPassword());

            User user = users.findByUsername(req.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable après inscription."));

            return ResponseEntity.ok(new SessionResponse(token, user.getUsername().value(), user.getId()));
        } catch (RuntimeException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthenticationRequest req) {
        try {
            String token = authService.login(req.getUsername(), req.getPassword());

            User user = users.findByUsername(req.getUsername())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable."));

            return ResponseEntity.ok(new SessionResponse(token, user.getUsername().value(), user.getId()));
        } catch (RuntimeException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.status(401).body(err);
        }
    }

    @PostMapping("/reset-password/code")
    public ResponseEntity<?> sendResetCode(@RequestBody ResetCodeRequest request) {
        requestPasswordResetUseCase.requestReset(request.getEmail());
        return ResponseEntity.ok().body(new SimpleMessageResponse(
                "Si un compte existe avec cet email, un code a été envoyé."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok().body(new SimpleMessageResponse("Mot de passe mis à jour."));
    }
}