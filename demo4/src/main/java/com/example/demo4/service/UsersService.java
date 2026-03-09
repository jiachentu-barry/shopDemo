package com.example.demo4.service;

import java.util.List;

import com.example.demo4.entity.Users;

public interface UsersService {
    Users addUser(Users user);

    Users getUserById(Long id);

    List<Users> getAllUsers();

    Users login(String username, String password);
}
