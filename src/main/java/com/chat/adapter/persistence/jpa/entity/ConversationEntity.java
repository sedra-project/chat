package com.chat.adapter.persistence.jpa.entity;

import com.chat.domain.model.ConversationType;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conversations")
public class ConversationEntity {

    @Id
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private ConversationType type;

    // On stocke les participants (Set de String) dans une table jointe
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "conversation_participants", joinColumns = @JoinColumn(name = "conversation_id"))
    @Column(name = "username")
    private Set<String> participants = new HashSet<>();

    public ConversationEntity() {
    }


    public ConversationEntity(String id, String name, ConversationType type, Set<String> participants) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.participants = participants;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConversationType getType() {
        return type;
    }

    public void setType(ConversationType type) {
        this.type = type;
    }

    public Set<String> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<String> participants) {
        this.participants = participants;
    }
}
