package com.chat.application.usecase;

import com.chat.application.port.out.EmailSenderPort;
import org.slf4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("inDB")
public class ConsoleEmailSenderAdapter implements EmailSenderPort {
    Logger log = org.slf4j.LoggerFactory.getLogger(ConsoleEmailSenderAdapter.class);
    @Override
    public void sendEmail(String to, String subject, String content) {
        log.info("EMAIL SIMULÉ To={} Subject={} Content={}", to, subject, content);
    }
}
