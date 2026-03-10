package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.repositories.UserRepository;
import com.beno.summaryspherebackend.services.DocumentService;
import com.beno.summaryspherebackend.services.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final DocumentService documentService;

    public UserServiceImpl(UserRepository userRepository, DocumentService documentService) {
        this.userRepository = userRepository;
        this.documentService = documentService;
    }

    @Override
    @Transactional
    public void deleteUserWithFiles(User user) {
        documentService.deleteFilesByUser(user);
        userRepository.delete(user);
    }

}
