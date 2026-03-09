package com.example.demo4.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo4.entity.Users;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Users findByUsername(String username);
}
