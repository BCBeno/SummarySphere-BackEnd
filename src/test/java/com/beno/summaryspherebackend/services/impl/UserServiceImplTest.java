package com.beno.summaryspherebackend.services.impl;

import com.beno.summaryspherebackend.entities.User;
import com.beno.summaryspherebackend.enums.Role;
import com.beno.summaryspherebackend.repositories.UserRepository;
import com.beno.summaryspherebackend.services.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    DocumentService documentService;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    void deleteUserWithFiles_deletesFilesThenUser_inOrder() {
        User user = User.builder()
                .email("test@test.com")
                .password("pw")
                .fullName("Test")
                .role(Role.USER)
                .build();

        userService.deleteUserWithFiles(user);

        InOrder inOrder = inOrder(documentService, userRepository);
        inOrder.verify(documentService).deleteFilesByUser(user);
        inOrder.verify(userRepository).delete(user);
    }

    @Test
    void deleteUserWithFiles_whenDeleteFilesThrows_doesNotDeleteUser() {
        User user = User.builder()
                .email("test@test.com")
                .password("pw")
                .fullName("Test")
                .role(Role.USER)
                .build();

        doThrow(new RuntimeException("boom")).when(documentService).deleteFilesByUser(user);

        assertThrows(RuntimeException.class, () -> userService.deleteUserWithFiles(user));
        verify(userRepository, never()).delete(any());
    }
}

