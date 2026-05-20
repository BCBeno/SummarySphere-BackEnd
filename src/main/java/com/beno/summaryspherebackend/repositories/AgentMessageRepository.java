package com.beno.summaryspherebackend.repositories;

import com.beno.summaryspherebackend.entities.AgentConversation;
import com.beno.summaryspherebackend.entities.AgentMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {
    List<AgentMessage> findAllByConversationOrderByCreatedAtAsc(AgentConversation conversation);
    List<AgentMessage> findTop10ByConversationOrderByCreatedAtDesc(AgentConversation conversation);
    void deleteAllByConversation(AgentConversation conversation);
}
