package com.example.demo4.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo4.entity.Users;
import com.example.demo4.service.UsersService;

import jakarta.servlet.http.HttpSession;

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
                        HttpSession session,
                        RedirectAttributes ra) {
        Users user = usersService.login(username, password);
        if (user != null) {
            session.setAttribute("loginUser", user);
            return "redirect:/";
        }
        ra.addFlashAttribute("error", "用户名或密码错误");
        return "redirect:/login.html?error";
    }

    @GetMapping("/api/currentUser")
    @ResponseBody
    public Map<String, Object> currentUser(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = (Users) session.getAttribute("loginUser");
        if (user != null) {
            result.put("loggedIn", true);
            result.put("username", user.getUsername());
            result.put("id", user.getId());
            result.put("role", user.getRole() != null ? user.getRole() : "USER");
        } else {
            result.put("loggedIn", false);
        }
        return result;
    }

    @GetMapping("/api/profile")
    @ResponseBody
    public Map<String, Object> getProfile(HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = (Users) session.getAttribute("loginUser");
        if (user == null) {
            result.put("success", false);
            result.put("msg", "未登录");
            return result;
        }
        result.put("success", true);
        result.put("username", user.getUsername());
        result.put("email", user.getEmail() != null ? user.getEmail() : "");
        result.put("role", user.getRole() != null ? user.getRole() : "USER");
        result.put("createTime", user.getCreateTime() != null ? user.getCreateTime().toString() : "");
        return result;
    }

    @PostMapping("/api/updateProfile")
    @ResponseBody
    public Map<String, Object> updateProfile(@RequestParam String username,
                                              @RequestParam String email,
                                              HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = (Users) session.getAttribute("loginUser");
        if (user == null) {
            result.put("success", false);
            result.put("msg", "未登录");
            return result;
        }
        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("msg", "用户名不能为空");
            return result;
        }
        user.setUsername(username.trim());
        user.setEmail(email != null ? email.trim() : "");
        usersService.addUser(user);
        session.setAttribute("loginUser", user);
        result.put("success", true);
        result.put("msg", "个人信息修改成功");
        return result;
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @PostMapping("/api/changePassword")
    @ResponseBody
    public Map<String, Object> changePassword(@RequestParam String oldPassword,
                                               @RequestParam String newPassword,
                                               HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Users user = (Users) session.getAttribute("loginUser");
        if (user == null) {
            result.put("success", false);
            result.put("msg", "未登录");
            return result;
        }
        if (!user.getPassword().equals(oldPassword)) {
            result.put("success", false);
            result.put("msg", "原密码错误");
            return result;
        }
        user.setPassword(newPassword);
        usersService.addUser(user);
        session.setAttribute("loginUser", user);
        result.put("success", true);
        result.put("msg", "密码修改成功");
        return result;
    }
}
