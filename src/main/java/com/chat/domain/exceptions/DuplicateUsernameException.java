package com.chat.domain.exceptions;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("Username déjà utilisé: " + username);
    }
}