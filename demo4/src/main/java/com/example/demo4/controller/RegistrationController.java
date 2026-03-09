package com.example.demo4.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo4.entity.Users;
import com.example.demo4.service.UsersService;

@Controller
public class RegistrationController {
    private final UsersService usersService;

    public RegistrationController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping("/register")
    public String showRegister() {
        return "redirect:/register.html";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam(required = false) String email) {
        Users user = new Users();
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        usersService.addUser(user);
        return "redirect:/register-success.html";
    }
}
