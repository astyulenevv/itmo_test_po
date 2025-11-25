package com.example.integration.integration;

import com.example.integration.entity.Profile;
import com.example.integration.entity.Settings;
import com.example.integration.entity.User;
import com.example.integration.repository.ProfileRepository;
import com.example.integration.repository.SettingsRepository;
import com.example.integration.repository.UserRepository;
import com.example.integration.service.UserService;
import com.example.integration.service.ProfileService;
import com.example.integration.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserProfileSettingsIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private SettingsService settingsService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProfileRepository profileRepository;
    
    @Autowired
    private SettingsRepository settingsRepository;
    
    @BeforeEach
    void setUp() {
        settingsRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    void shouldCreateUserWithDefaultProfileAndSettings() {
        User newUser = User.builder()
            .username("testuser")
            .email("test@example.com")
            .password("password123")
            .build();
        
        User createdUser = userService.createUserWithDefaults(newUser);
        
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals("testuser", createdUser.getUsername());
        assertEquals("test@example.com", createdUser.getEmail());
        assertEquals(User.UserStatus.ACTIVE, createdUser.getStatus());
        
        Optional<User> dbUser = userRepository.findById(createdUser.getId());
        assertTrue(dbUser.isPresent());
        
        Optional<Profile> dbProfile = profileRepository.findByUserId(createdUser.getId());
        assertTrue(dbProfile.isPresent());
        assertEquals(createdUser.getId(), dbProfile.get().getUser().getId());
        assertTrue(dbProfile.get().getIsPublic());
        
        Optional<Settings> dbSettings = settingsRepository.findByUserId(createdUser.getId());
        assertTrue(dbSettings.isPresent());
        assertEquals(createdUser.getId(), dbSettings.get().getUser().getId());
        assertEquals(Settings.Theme.LIGHT, dbSettings.get().getTheme());
        assertEquals("en", dbSettings.get().getLanguageCode());
    }
    
    @Test
    void shouldValidateDuplicateUserCreation() {
        User firstUser = User.builder()
            .username("duplicate")
            .email("duplicate@example.com")
            .password("password123")
            .build();
        
        userService.createUserWithDefaults(firstUser);
        
        User duplicateUsername = User.builder()
            .username("duplicate")
            .email("different@example.com")
            .password("password123")
            .build();
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.createUserWithDefaults(duplicateUsername));
        
        User duplicateEmail = User.builder()
            .username("different")
            .email("duplicate@example.com")
            .password("password123")
            .build();
        
        assertThrows(IllegalArgumentException.class, 
            () -> userService.createUserWithDefaults(duplicateEmail));
    }
    
    @Test
    void shouldUpdateUserStatusWithCascadeEffects() {
        User user = User.builder()
            .username("statustest")
            .email("status@example.com")
            .password("password123")
            .build();
        
        User createdUser = userService.createUserWithDefaults(user);
        Long userId = createdUser.getId();
        
        Optional<Profile> initialProfile = profileRepository.findByUserId(userId);
        assertTrue(initialProfile.isPresent());
        assertTrue(initialProfile.get().getIsPublic());
        
        User suspendedUser = userService.updateUserStatus(userId, User.UserStatus.SUSPENDED);
        
        assertEquals(User.UserStatus.SUSPENDED, suspendedUser.getStatus());
        assertNotNull(suspendedUser.getUpdatedAt());
        
        Optional<Profile> updatedProfile = profileRepository.findByUserId(userId);
        assertTrue(updatedProfile.isPresent());
        assertFalse(updatedProfile.get().getIsPublic());
        assertNotNull(updatedProfile.get().getUpdatedAt());
    }
    
    @Test
    void shouldPerformComplexCrossEntityQueries() {
        User user1 = createTestUser("user1", "user1@example.com");
        User user2 = createTestUser("user2", "user2@example.com");
        User user3 = createTestUser("user3", "user3@example.com");
        
        Settings settings2 = user2.getSettings();
        settings2.setTheme(Settings.Theme.DARK);
        settingsRepository.save(settings2);
        
        Settings settings3 = user3.getSettings();
        settings3.setTheme(Settings.Theme.DARK);
        settingsRepository.save(settings3);
        
        List<User> darkThemeUsers = settingsService.getUsersByTheme(Settings.Theme.DARK);
        List<User> lightThemeUsers = settingsService.getUsersByTheme(Settings.Theme.LIGHT);
        
        assertEquals(2, darkThemeUsers.size());
        assertEquals(1, lightThemeUsers.size());
        
        assertTrue(darkThemeUsers.stream().anyMatch(u -> "user2".equals(u.getUsername())));
        assertTrue(darkThemeUsers.stream().anyMatch(u -> "user3".equals(u.getUsername())));
        assertTrue(lightThemeUsers.stream().anyMatch(u -> "user1".equals(u.getUsername())));
    }
    
    @Test
    void shouldHandleCascadingDeleteOperations() {
        User user = createTestUser("deletetest", "delete@example.com");
        Long userId = user.getId();
        
        assertTrue(userRepository.findById(userId).isPresent());
        assertTrue(profileRepository.findByUserId(userId).isPresent());
        assertTrue(settingsRepository.findByUserId(userId).isPresent());
        
        userService.deleteUser(userId);
        
        assertFalse(userRepository.findById(userId).isPresent());
        assertFalse(profileRepository.findByUserId(userId).isPresent());
        assertFalse(settingsRepository.findByUserId(userId).isPresent());
    }
    
    @Test
    void shouldGenerateAccurateStatisticsAcrossAllEntities() {
        User active1 = createTestUser("active1", "active1@example.com");
        User active2 = createTestUser("active2", "active2@example.com");
        User suspended = createTestUser("suspended", "suspended@example.com");
        
        userService.updateUserStatus(suspended.getId(), User.UserStatus.SUSPENDED);
        
        Settings settings1 = active1.getSettings();
        settings1.setTwoFactorEnabled(true);
        settings1.setEmailNotifications(true);
        settingsRepository.save(settings1);
        
        Settings settings2 = active2.getSettings();
        settings2.setEmailNotifications(true);
        settingsRepository.save(settings2);
        
        UserService.UserStatistics stats = userService.getUserStatistics();
        
        assertEquals(3, stats.getTotalUsers());
        assertEquals(2, stats.getActiveUsers());
        assertEquals(1, stats.getSuspendedUsers());
        assertEquals(0, stats.getDeletedUsers());
        
        assertEquals(2, stats.getPublicProfiles());
        assertEquals(1, stats.getPrivateProfiles());
        
        assertEquals(2, stats.getUsersWithEmailNotifications());
        assertEquals(1, stats.getUsersWithTwoFactor());
    }
    
    private User createTestUser(String username, String email) {
        User user = User.builder()
            .username(username)
            .email(email)
            .password("password123")
            .build();
        
        return userService.createUserWithDefaults(user);
    }
}