package com.example.integration.repository;

import com.example.integration.entity.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, Long> {
    
    Optional<Settings> findByUserId(Long userId);
    
    @Query("SELECT s FROM Settings s JOIN s.user u WHERE u.username = :username")
    Optional<Settings> findByUserUsername(@Param("username") String username);
    
    List<Settings> findByTheme(Settings.Theme theme);
    
    List<Settings> findByLanguageCode(String languageCode);
    
    List<Settings> findByTimeZone(String timeZone);
    
    List<Settings> findByEmailNotificationsTrue();
    
    List<Settings> findByTwoFactorEnabledTrue();
    
    List<Settings> findByNotificationFrequency(Settings.NotificationFrequency frequency);
    
    List<Settings> findByAutoSaveTrue();
    
    @Query("SELECT s FROM Settings s WHERE s.itemsPerPage != 20")
    List<Settings> findSettingsWithCustomItemsPerPage();
    
    Long countByTheme(Settings.Theme theme);
    
    @Query("SELECT s FROM Settings s JOIN FETCH s.user u WHERE " +
           "s.emailNotifications = true OR s.pushNotifications = true OR s.smsNotifications = true")
    List<Settings> findSettingsWithNotificationsEnabled();
    
    @Query("SELECT s FROM Settings s WHERE s.profileVisibility = :profileVisibility " +
           "AND s.allowMessages = :allowMessages AND s.showOnlineStatus = :showOnlineStatus")
    List<Settings> findByPrivacySettings(@Param("profileVisibility") Boolean profileVisibility,
                                       @Param("allowMessages") Boolean allowMessages,
                                       @Param("showOnlineStatus") Boolean showOnlineStatus);
    
    @Query("SELECT s FROM Settings s WHERE s.sessionTimeout BETWEEN :minTimeout AND :maxTimeout")
    List<Settings> findBySessionTimeoutBetween(@Param("minTimeout") Integer minTimeout, @Param("maxTimeout") Integer maxTimeout);
}