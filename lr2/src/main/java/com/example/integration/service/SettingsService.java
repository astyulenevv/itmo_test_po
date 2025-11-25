package com.example.integration.service;

import com.example.integration.entity.Settings;
import com.example.integration.entity.User;
import com.example.integration.repository.SettingsRepository;
import com.example.integration.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SettingsService {
    
    private final SettingsRepository settingsRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public Settings updateSettings(Long userId, @Valid Settings settingsUpdate) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        Settings existingSettings = user.getSettings();
        if (existingSettings == null) {
            settingsUpdate.setUser(user);
            settingsUpdate.setCreatedAt(LocalDateTime.now());
            Settings savedSettings = settingsRepository.save(settingsUpdate);
            
            user.setSettings(savedSettings);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            return savedSettings;
        }
        
        updateSettingsFields(existingSettings, settingsUpdate);
        existingSettings.setUpdatedAt(LocalDateTime.now());
        
        Settings savedSettings = settingsRepository.save(existingSettings);
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return savedSettings;
    }
    
    @Transactional(readOnly = true)
    public Optional<Settings> getSettingsWithUserDetails(Long userId) {
        return settingsRepository.findByUserId(userId);
    }
    
    @Transactional
    public Settings updateNotificationSettings(Long userId, NotificationPreferences preferences) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        Settings settings = user.getSettings();
        if (settings == null) {
            throw new IllegalArgumentException("Settings not found for user: " + userId);
        }
        
        settings.setEmailNotifications(preferences.getEmailNotifications());
        settings.setPushNotifications(preferences.getPushNotifications());
        settings.setSmsNotifications(preferences.getSmsNotifications());
        settings.setNotificationFrequency(preferences.getNotificationFrequency());
        settings.setUpdatedAt(LocalDateTime.now());
        
        Settings savedSettings = settingsRepository.save(settings);
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return savedSettings;
    }
    
    @Transactional
    public Settings updatePrivacySettings(Long userId, PrivacyPreferences preferences) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        Settings settings = user.getSettings();
        if (settings == null) {
            throw new IllegalArgumentException("Settings not found for user: " + userId);
        }
        
        settings.setProfileVisibility(preferences.getProfileVisibility());
        settings.setAllowMessages(preferences.getAllowMessages());
        settings.setShowOnlineStatus(preferences.getShowOnlineStatus());
        settings.setUpdatedAt(LocalDateTime.now());
        
        Settings savedSettings = settingsRepository.save(settings);
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return savedSettings;
    }
    
    @Transactional
    public Settings updateSecuritySettings(Long userId, SecurityPreferences preferences) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        Settings settings = user.getSettings();
        if (settings == null) {
            throw new IllegalArgumentException("Settings not found for user: " + userId);
        }
        
        if (preferences.getSessionTimeout() < 30 || preferences.getSessionTimeout() > 10080) {
            throw new IllegalArgumentException("Session timeout must be between 30 and 10080 minutes");
        }
        
        settings.setTwoFactorEnabled(preferences.getTwoFactorEnabled());
        settings.setSessionTimeout(preferences.getSessionTimeout());
        settings.setUpdatedAt(LocalDateTime.now());
        
        Settings savedSettings = settingsRepository.save(settings);
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return savedSettings;
    }
    
    @Transactional(readOnly = true)
    public List<User> getUsersByTheme(Settings.Theme theme) {
        List<Settings> settingsList = settingsRepository.findByTheme(theme);
        
        return settingsList.stream()
            .map(Settings::getUser)
            .filter(user -> user.getStatus() == User.UserStatus.ACTIVE)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public SettingsAnalytics getSettingsAnalytics() {
        long totalSettings = settingsRepository.count();
        
        long lightThemeUsers = settingsRepository.countByTheme(Settings.Theme.LIGHT);
        long darkThemeUsers = settingsRepository.countByTheme(Settings.Theme.DARK);
        long autoThemeUsers = settingsRepository.countByTheme(Settings.Theme.AUTO);
        
        long emailNotificationUsers = settingsRepository.findByEmailNotificationsTrue().size();
        long pushNotificationUsers = settingsRepository.findByEmailNotificationsTrue().size();
        long twoFactorUsers = settingsRepository.findByTwoFactorEnabledTrue().size();
        
        List<Settings> allSettings = settingsRepository.findAll();
        long englishUsers = allSettings.stream()
            .filter(s -> "en".equals(s.getLanguageCode()))
            .count();
        
        return SettingsAnalytics.builder()
            .totalSettings(totalSettings)
            .lightThemeUsers(lightThemeUsers)
            .darkThemeUsers(darkThemeUsers)
            .autoThemeUsers(autoThemeUsers)
            .emailNotificationUsers(emailNotificationUsers)
            .pushNotificationUsers(pushNotificationUsers)
            .twoFactorUsers(twoFactorUsers)
            .englishUsers(englishUsers)
            .build();
    }
    
    private void updateSettingsFields(Settings existing, Settings update) {
        if (update.getTheme() != null) existing.setTheme(update.getTheme());
        if (update.getLanguageCode() != null) existing.setLanguageCode(update.getLanguageCode());
        if (update.getTimeZone() != null) existing.setTimeZone(update.getTimeZone());
        if (update.getDateFormat() != null) existing.setDateFormat(update.getDateFormat());
        if (update.getProfileVisibility() != null) existing.setProfileVisibility(update.getProfileVisibility());
        if (update.getAllowMessages() != null) existing.setAllowMessages(update.getAllowMessages());
        if (update.getShowOnlineStatus() != null) existing.setShowOnlineStatus(update.getShowOnlineStatus());
        if (update.getEmailNotifications() != null) existing.setEmailNotifications(update.getEmailNotifications());
        if (update.getPushNotifications() != null) existing.setPushNotifications(update.getPushNotifications());
        if (update.getSmsNotifications() != null) existing.setSmsNotifications(update.getSmsNotifications());
        if (update.getNotificationFrequency() != null) existing.setNotificationFrequency(update.getNotificationFrequency());
        if (update.getItemsPerPage() != null) existing.setItemsPerPage(update.getItemsPerPage());
        if (update.getAutoSave() != null) existing.setAutoSave(update.getAutoSave());
        if (update.getAutoSaveInterval() != null) existing.setAutoSaveInterval(update.getAutoSaveInterval());
        if (update.getTwoFactorEnabled() != null) existing.setTwoFactorEnabled(update.getTwoFactorEnabled());
        if (update.getSessionTimeout() != null) existing.setSessionTimeout(update.getSessionTimeout());
    }
    
    @lombok.Data
    @lombok.Builder
    public static class NotificationPreferences {
        private Boolean emailNotifications;
        private Boolean pushNotifications;
        private Boolean smsNotifications;
        private Settings.NotificationFrequency notificationFrequency;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class PrivacyPreferences {
        private Boolean profileVisibility;
        private Boolean allowMessages;
        private Boolean showOnlineStatus;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SecurityPreferences {
        private Boolean twoFactorEnabled;
        private Integer sessionTimeout;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SettingsAnalytics {
        private long totalSettings;
        private long lightThemeUsers;
        private long darkThemeUsers;
        private long autoThemeUsers;
        private long emailNotificationUsers;
        private long pushNotificationUsers;
        private long twoFactorUsers;
        private long englishUsers;
    }
}