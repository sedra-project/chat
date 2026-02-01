package com.chat.application.port.in;

public interface RequestPasswordResetUseCase {

    /**
     * Demande un code de réinit pour un email.
     * Si l'email n'existe pas, on ne le révèle pas (silencieux).
     */
    void requestReset(String email);
}