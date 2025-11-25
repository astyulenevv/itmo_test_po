package com.example.integration.integration;

import com.example.integration.entity.Settings;
import com.example.integration.entity.User;
import com.example.integration.repository.SettingsRepository;
import com.example.integration.repository.UserRepository;
import com.example.integration.service.SettingsService;
import com.example.integration.service.UserService;
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
public class SettingsServiceIntegrationTest {
    
    @Autowired
    private SettingsService settingsService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SettingsRepository settingsRepository;
    
    @BeforeEach
    void setUp() {
        settingsRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    void shouldUpdateSettingsAndSynchronizeWithUser() {
        User user = createTestUser("settingsuser", "settings@example.com");
        Long userId = user.getId();
        
        User originalUser = userRepository.findById(userId).orElseThrow();
        Settings originalSettings = originalUser.getSettings();
        
        Settings settingsUpdate = Settings.builder()
            .theme(Settings.Theme.DARK)
            .languageCode("es")
            .timeZone("America/New_York")
            .dateFormat(Settings.DateFormat.MM_DD_YYYY)
            .itemsPerPage(50)
            .autoSave(false)
            .build();
        
        Settings updatedSettings = settingsService.updateSettings(userId, settingsUpdate);
        
        assertNotNull(updatedSettings);
        assertEquals(Settings.Theme.DARK, updatedSettings.getTheme());
        assertEquals("es", updatedSettings.getLanguageCode());
        assertEquals("America/New_York", updatedSettings.getTimeZone());
        assertEquals(Settings.DateFormat.MM_DD_YYYY, updatedSettings.getDateFormat());
        assertEquals(50, updatedSettings.getItemsPerPage());
        assertFalse(updatedSettings.getAutoSave());
        
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertTrue(updatedUser.getUpdatedAt().isAfter(originalUser.getCreatedAt()));
        
        assertTrue(updatedSettings.getUpdatedAt().isAfter(originalSettings.getCreatedAt()));
    }
    
    @Test
    void shouldUpdateNotificationPreferencesIndependently() {
        User user = createTestUser("notificationuser", "notification@example.com");
        Long userId = user.getId();
        
        SettingsService.NotificationPreferences preferences = 
            SettingsService.NotificationPreferences.builder()
                .emailNotifications(false)
                .pushNotifications(true)
                .smsNotifications(true)
                .notificationFrequency(Settings.NotificationFrequency.DAILY)
                .build();
        
        Settings updatedSettings = settingsService.updateNotificationSettings(userId, preferences);
        
        assertFalse(updatedSettings.getEmailNotifications());
        assertTrue(updatedSettings.getPushNotifications());
        assertTrue(updatedSettings.getSmsNotifications());
        assertEquals(Settings.NotificationFrequency.DAILY, updatedSettings.getNotificationFrequency());
        
        assertEquals(Settings.Theme.LIGHT, updatedSettings.getTheme());
        assertEquals("en", updatedSettings.getLanguageCode());
    }
    
    @Test
    void shouldUpdatePrivacyPreferencesWithBusinessValidation() {
        User user = createTestUser("privacyuser", "privacy@example.com");
        Long userId = user.getId();
        
        SettingsService.PrivacyPreferences preferences = 
            SettingsService.PrivacyPreferences.builder()
                .profileVisibility(false)
                .allowMessages(false)
                .showOnlineStatus(false)
                .build();
        
        Settings updatedSettings = settingsService.updatePrivacySettings(userId, preferences);
        
        assertFalse(updatedSettings.getProfileVisibility());
        assertFalse(updatedSettings.getAllowMessages());
        assertFalse(updatedSettings.getShowOnlineStatus());
        
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertNotNull(updatedUser.getUpdatedAt());
    }
    
    @Test
    void shouldUpdateSecuritySettingsWithValidationRules() {
        User user = createTestUser("securityuser", "security@example.com");
        Long userId = user.getId();
        
        SettingsService.SecurityPreferences validPreferences = 
            SettingsService.SecurityPreferences.builder()
                .twoFactorEnabled(true)
                .sessionTimeout(720)
                .build();
        
        Settings updatedSettings = settingsService.updateSecuritySettings(userId, validPreferences);
        
        assertTrue(updatedSettings.getTwoFactorEnabled());
        assertEquals(720, updatedSettings.getSessionTimeout());
        
        SettingsService.SecurityPreferences invalidPreferences = 
            SettingsService.SecurityPreferences.builder()
                .sessionTimeout(15)
                .build();
        
        assertThrows(IllegalArgumentException.class, 
            () -> settingsService.updateSecuritySettings(userId, invalidPreferences));
        
        SettingsService.SecurityPreferences tooHighPreferences = 
            SettingsService.SecurityPreferences.builder()
                .sessionTimeout(15000)
                .build();
        
        assertThrows(IllegalArgumentException.class, 
            () -> settingsService.updateSecuritySettings(userId, tooHighPreferences));
    }
    
    @Test
    void shouldSegmentUsersBySettingsPreferences() {
        User lightUser1 = createTestUser("light1", "light1@example.com");
        User lightUser2 = createTestUser("light2", "light2@example.com");
        User darkUser1 = createTestUser("dark1", "dark1@example.com");
        User darkUser2 = createTestUser("dark2", "dark2@example.com");
        User autoUser = createTestUser("auto", "auto@example.com");
        
        settingsService.updateSettings(darkUser1.getId(), Settings.builder()
            .theme(Settings.Theme.DARK)
            .build());
        
        settingsService.updateSettings(darkUser2.getId(), Settings.builder()
            .theme(Settings.Theme.DARK)
            .build());
        
        settingsService.updateSettings(autoUser.getId(), Settings.builder()
            .theme(Settings.Theme.AUTO)
            .build());
        
        userService.updateUserStatus(darkUser2.getId(), User.UserStatus.SUSPENDED);
        
        List<User> lightUsers = settingsService.getUsersByTheme(Settings.Theme.LIGHT);
        List<User> darkUsers = settingsService.getUsersByTheme(Settings.Theme.DARK);
        List<User> autoUsers = settingsService.getUsersByTheme(Settings.Theme.AUTO);
        
        assertEquals(2, lightUsers.size());
        assertEquals(1, darkUsers.size());
        assertEquals(1, autoUsers.size());
        
        assertTrue(lightUsers.stream().anyMatch(u -> "light1".equals(u.getUsername())));
        assertTrue(lightUsers.stream().anyMatch(u -> "light2".equals(u.getUsername())));
        assertTrue(darkUsers.stream().anyMatch(u -> "dark1".equals(u.getUsername())));
        assertTrue(autoUsers.stream().anyMatch(u -> "auto".equals(u.getUsername())));
    }
    
    @Test
    void shouldGenerateComprehensiveSettingsAnalytics() {
        User user1 = createTestUser("analytics1", "analytics1@example.com");
        User user2 = createTestUser("analytics2", "analytics2@example.com");
        User user3 = createTestUser("analytics3", "analytics3@example.com");
        
        settingsService.updateSettings(user1.getId(), Settings.builder()
            .theme(Settings.Theme.DARK)
            .emailNotifications(true)
            .twoFactorEnabled(true)
            .build());
        
        settingsService.updateSettings(user2.getId(), Settings.builder()
            .theme(Settings.Theme.DARK)
            .emailNotifications(true)
            .twoFactorEnabled(false)
            .build());
        
        settingsService.updateSettings(user3.getId(), Settings.builder()
            .theme(Settings.Theme.AUTO)
            .emailNotifications(false)
            .twoFactorEnabled(true)
            .build());
        
        SettingsService.SettingsAnalytics analytics = settingsService.getSettingsAnalytics();
        
        assertEquals(3, analytics.getTotalSettings());
        assertEquals(0, analytics.getLightThemeUsers());
        assertEquals(2, analytics.getDarkThemeUsers());
        assertEquals(1, analytics.getAutoThemeUsers());
        assertEquals(2, analytics.getEmailNotificationUsers());
        assertEquals(2, analytics.getTwoFactorUsers());
        assertEquals(3, analytics.getEnglishUsers());
    }
    
    @Test
    void shouldHandleSettingsOperationsForNonExistentUsers() {
        Settings settingsUpdate = Settings.builder()
            .theme(Settings.Theme.DARK)
            .build();
        
        assertThrows(IllegalArgumentException.class, 
            () -> settingsService.updateSettings(999L, settingsUpdate));
        
        Optional<Settings> settings = settingsService.getSettingsWithUserDetails(999L);
        assertTrue(settings.isEmpty());
        
        SettingsService.NotificationPreferences preferences = 
            SettingsService.NotificationPreferences.builder()
                .emailNotifications(false)
                .build();
        
        assertThrows(IllegalArgumentException.class, 
            () -> settingsService.updateNotificationSettings(999L, preferences));
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