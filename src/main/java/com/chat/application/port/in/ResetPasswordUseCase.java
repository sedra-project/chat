package com.chat.application.port.in;

public interface ResetPasswordUseCase {

    /**
     * Vérifie le code et met à jour le mot de passe.
     */
    void resetPassword(String email, String code, String newPassword);
}