package com.beno.summaryspherebackend.repositories;

import com.beno.summaryspherebackend.entities.ChatMessage;
import com.beno.summaryspherebackend.entities.Document;
import com.beno.summaryspherebackend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByDocumentAndUserOrderByCreatedAtAsc(Document document, User user);
    void deleteAllByDocumentAndUser(Document document, User user);
}
