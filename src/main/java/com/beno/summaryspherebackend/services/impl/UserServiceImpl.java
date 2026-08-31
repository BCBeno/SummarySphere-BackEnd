package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.repositories.AgentConversationRepository;
import com.beno.summaryspherebackend.repositories.UserRepository;
import com.beno.summaryspherebackend.services.DocumentService;
import com.beno.summaryspherebackend.services.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final DocumentService documentService;
    private final AgentConversationRepository agentConversationRepository;

    public UserServiceImpl(UserRepository userRepository, DocumentService documentService,
            AgentConversationRepository agentConversationRepository) {
        this.userRepository = userRepository;
        this.documentService = documentService;
        this.agentConversationRepository = agentConversationRepository;
    }

    @Override
    @Transactional
    public void deleteUserWithFiles(User user) {
        var conversations = agentConversationRepository.findAllByUser(user);
        agentConversationRepository.deleteAll(conversations);
        agentConversationRepository.flush();

        documentService.deleteFilesByUser(user);
        userRepository.delete(user);
    }

}
