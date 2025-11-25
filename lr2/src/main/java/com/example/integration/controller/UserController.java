package com.example.integration.controller;

import com.example.integration.entity.User;
import com.example.integration.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        try {
            User createdUser = userService.createUserWithDefaults(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{usernameOrEmail}")
    public ResponseEntity<User> getUser(@PathVariable String usernameOrEmail) {
        Optional<User> userOpt = userService.findUserWithDetails(usernameOrEmail);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{userId}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam User.UserStatus status) {
        try {
            User updatedUser = userService.updateUserStatus(userId, status);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        try {
            userService.deleteUser(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/active-public")
    public ResponseEntity<List<User>> getActiveUsersWithPublicProfiles() {
        List<User> users = userService.findActiveUsersWithPublicProfiles();
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<UserService.UserStatistics> getUserStatistics() {
        UserService.UserStatistics stats = userService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }
}