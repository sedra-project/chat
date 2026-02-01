package com.chat.application.dto;

import com.chat.domain.model.ConversationType;

public class NotificationDto {
    public String type; // CONV_CREATED | DM_MESSAGE
    public String conversationId;
    public String from; // emetteur
    public String preview; // pour DM_MESSAGE
    public ConversationType conversationType; // "DIRECT"
    public String name; // label côté destinataire (ex: "alice_01")
    public long timestamp;

    public static NotificationDto convCreated(String convId, String from, String nameLabel) {
        NotificationDto n = new NotificationDto();
        n.type = "CONV_CREATED";
        n.conversationId = convId;
        n.from = from;
        n.name = nameLabel;
        n.conversationType = ConversationType.DIRECT;
        n.timestamp = System.currentTimeMillis();
        return n;
    }

    public static NotificationDto dmMessage(String convId, String from, String preview) {
        NotificationDto n = new NotificationDto();
        n.type = "DM_MESSAGE";
        n.conversationId = convId;
        n.from = from;
        n.preview = preview;
        n.timestamp = System.currentTimeMillis();
        return n;
    }
}
