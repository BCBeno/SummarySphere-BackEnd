package com.beno.summaryspherebackend.repositories;

import com.beno.summaryspherebackend.entities.AgentConversation;
import com.beno.summaryspherebackend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentConversationRepository extends JpaRepository<AgentConversation, String> {
    Optional<AgentConversation> findByUser(User user);
    Optional<AgentConversation> findByIdAndUser(String id, User user);
}
