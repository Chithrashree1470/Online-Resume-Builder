
package com.chithra.resumebuilder.controller;

import com.chithra.resumebuilder.model.User;
import com.chithra.resumebuilder.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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

        username = username.trim();

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
        user.setUsername(username);
        user.setPassword(encoder.encode(password));

        repository.saveAndFlush(user);

        return "redirect:/login?registered";
    }

    // Show password confirmation page.
    @GetMapping("/logout/confirm")
    public String confirmLogout() {
        return "logout-confirm";
    }

    // Verify the current password before logging out.
    @PostMapping("/logout/confirm")
    public String logoutWithPassword(
            @RequestParam String password,
            Principal principal,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        User user = repository.findByUsername(principal.getName())
                .orElseThrow();

        if (!encoder.matches(password, user.getPassword())) {
            model.addAttribute("error",
                    "Incorrect password. You are still logged in.");

            return "logout-confirm";
        }

        new SecurityContextLogoutHandler().logout(
                request,
                response,
                SecurityContextHolder.getContext().getAuthentication()
        );

        return "redirect:/";
    }
}
