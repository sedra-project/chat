package com.chat.adapter.websocket;


import com.chat.adapter.websocket.dto.MessageEventDto;
import com.chat.application.port.out.MessageEventPublisherPort;
import com.chat.domain.model.ChatMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class MessageEventPublisher implements MessageEventPublisherPort {

    private final SimpMessagingTemplate messaging;

    public MessageEventPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void messageUpdated(ChatMessage message) {
        messaging.convertAndSend(eventsDest(message.getConversationId()), MessageEventDto.updated(message));
    }

    @Override
    public void messageDeleted(ChatMessage message) {
        messaging.convertAndSend(eventsDest(message.getConversationId()), MessageEventDto.deleted(message));
    }

    private String eventsDest(String conversationId) {
        return "public".equals(conversationId)
                ? "/topic/public.events"
                : "/topic/conv." + conversationId + ".events";
    }
}