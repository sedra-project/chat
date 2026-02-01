package com.chat.application.dto;

import com.chat.domain.model.ConversationType;
import java.util.List;

public class ConversationSummary {
    private String id;
    private ConversationType type;
    private String name; // pour GROUP; pour DIRECT on peut mettre le nom du “peer”
    private List<String> members;

    public ConversationSummary() {}
    public ConversationSummary(String id, ConversationType type, String name, List<String> members) {
        this.id = id; this.type = type; this.name = name; this.members = members;
    }
    public String getId() { return id; }
    public ConversationType getType() { return type; }
    public String getName() { return name; }
    public List<String> getMembers() { return members; }
    public void setId(String id) { this.id = id; }
    public void setType(ConversationType type) { this.type = type; }
    public void setName(String name) { this.name = name; }
    public void setMembers(List<String> members) { this.members = members; }
}