
package com.chithra.resumebuilder.controller;

import com.chithra.resumebuilder.model.User;
import com.chithra.resumebuilder.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository repository,
                          PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String saveUser(
            @RequestParam String username,
            @RequestParam String password,
            Model model) {

        System.out.println("REGISTRATION REQUEST RECEIVED");

        if (username.isBlank() || password.length() < 8) {
            model.addAttribute("error",
                    "Enter a username and password of at least 8 characters.");
            return "register";
        }

        if (repository.findByUsername(username).isPresent()) {
            model.addAttribute("error", "Username already exists.");
            return "register";
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(encoder.encode(password));

        repository.saveAndFlush(user);

        System.out.println("REGISTRATION SUCCESSFUL: " + username);

        return "redirect:/login?registered";
    }
}
