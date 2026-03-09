package com.example.demo4.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo4.entity.Users;
import com.example.demo4.service.UsersService;

@Controller
public class LoginController {
    private final UsersService usersService;

    public LoginController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping("/login")
    public String showLogin() {
        return "redirect:/login.html";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        RedirectAttributes ra) {
        Users user = usersService.login(username, password);
        if (user != null) {
            return "redirect:/login-success.html";
        }
        ra.addFlashAttribute("error", "用户名或密码错误");
        return "redirect:/login.html?error";
    }
}
