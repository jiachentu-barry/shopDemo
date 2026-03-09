package com.example.demo4.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo4.entity.Users;
import com.example.demo4.repository.UsersRepository;
import com.example.demo4.service.UsersService;

@Service
public class UsersServiceImpl implements UsersService {
    private final UsersRepository usersRepository;

    public UsersServiceImpl(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public Users addUser(Users user) {
        return usersRepository.save(user);
    }

    @Override
    public Users getUserById(Long id) {
        return usersRepository.findById(id).orElse(null);
    }

    @Override
    public List<Users> getAllUsers() {
        return usersRepository.findAll();
    }

    @Override
    public Users login(String username, String password) {
        Users user = usersRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}
