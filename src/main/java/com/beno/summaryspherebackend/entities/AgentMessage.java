package com.beno.summaryspherebackend.entities;

import com.beno.summaryspherebackend.enums.AgentMessageRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_messages")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AgentMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private AgentConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentMessageRole role;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt;

    public AgentMessage(AgentConversation conversation, AgentMessageRole role, String content) {
        this.conversation = conversation;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
