package com.beno.summaryspherebackend.services;

import com.beno.summaryspherebackend.entities.User;

public interface UserService {
    void deleteUserWithFiles(User user);
}
