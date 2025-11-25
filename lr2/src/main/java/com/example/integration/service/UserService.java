package com.example.integration.service;

import com.example.integration.entity.User;
import com.example.integration.entity.Profile;
import com.example.integration.entity.Settings;
import com.example.integration.repository.UserRepository;
import com.example.integration.repository.ProfileRepository;
import com.example.integration.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SettingsRepository settingsRepository;
    
    @Transactional
    public User createUserWithDefaults(@Valid User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + user.getEmail());
        }
        
        User savedUser = userRepository.save(user);
        
        Profile defaultProfile = Profile.builder()
            .user(savedUser)
            .isPublic(true)
            .build();
        Profile savedProfile = profileRepository.save(defaultProfile);
        
        Settings defaultSettings = Settings.builder()
            .user(savedUser)
            .build();
        Settings savedSettings = settingsRepository.save(defaultSettings);
        
        savedUser.setProfile(savedProfile);
        savedUser.setSettings(savedSettings);
        
        return savedUser;
    }
    
    @Transactional(readOnly = true)
    public Optional<User> findUserWithDetails(String usernameOrEmail) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(usernameOrEmail);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getProfile() != null) {
                user.getProfile().getFullName();
            }
            if (user.getSettings() != null) {
                user.getSettings().getTheme();
            }
        }
        
        return userOpt;
    }
    
    @Transactional
    public User updateUserStatus(Long userId, User.UserStatus newStatus) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        user.setStatus(newStatus);
        user.setUpdatedAt(LocalDateTime.now());
        
        if (newStatus == User.UserStatus.SUSPENDED || newStatus == User.UserStatus.DELETED) {
            Profile profile = user.getProfile();
            if (profile != null && profile.getIsPublic()) {
                profile.setIsPublic(false);
                profile.setUpdatedAt(LocalDateTime.now());
                profileRepository.save(profile);
            }
        }
        
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        userRepository.delete(user);
    }
    
    @Transactional(readOnly = true)
    public List<User> findActiveUsersWithPublicProfiles() {
        return userRepository.findActiveUsersWithProfiles().stream()
            .filter(user -> user.getProfile() != null && user.getProfile().getIsPublic())
            .toList();
    }
    
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(User.UserStatus.SUSPENDED);
        long deletedUsers = userRepository.countByStatus(User.UserStatus.DELETED);
        
        long publicProfiles = profileRepository.countByIsPublic(true);
        long privateProfiles = profileRepository.countByIsPublic(false);
        
        long usersWithEmailNotifications = settingsRepository.findByEmailNotificationsTrue().size();
        long usersWithTwoFactor = settingsRepository.findByTwoFactorEnabledTrue().size();
        
        return UserStatistics.builder()
            .totalUsers(totalUsers)
            .activeUsers(activeUsers)
            .suspendedUsers(suspendedUsers)
            .deletedUsers(deletedUsers)
            .publicProfiles(publicProfiles)
            .privateProfiles(privateProfiles)
            .usersWithEmailNotifications(usersWithEmailNotifications)
            .usersWithTwoFactor(usersWithTwoFactor)
            .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class UserStatistics {
        private long totalUsers;
        private long activeUsers;
        private long suspendedUsers;
        private long deletedUsers;
        private long publicProfiles;
        private long privateProfiles;
        private long usersWithEmailNotifications;
        private long usersWithTwoFactor;
    }
}