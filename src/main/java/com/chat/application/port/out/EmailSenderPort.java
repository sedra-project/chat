package com.chat.application.port.out;

public interface EmailSenderPort {

    void sendEmail(String to, String subject, String content);
}