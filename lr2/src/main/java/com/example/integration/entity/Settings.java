package com.example.integration.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Theme theme = Theme.LIGHT;
    
    @Builder.Default
    @Column(name = "language_code")
    private String languageCode = "en";
    
    @Builder.Default
    @Column(name = "time_zone")
    private String timeZone = "UTC";
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "date_format")
    private DateFormat dateFormat = DateFormat.YYYY_MM_DD;
    
    @Builder.Default
    @Column(name = "profile_visibility")
    private Boolean profileVisibility = true;
    
    @Builder.Default
    @Column(name = "allow_messages")
    private Boolean allowMessages = true;
    
    @Builder.Default
    @Column(name = "show_online_status")
    private Boolean showOnlineStatus = true;
    
    @Builder.Default
    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;
    
    @Builder.Default
    @Column(name = "push_notifications")
    private Boolean pushNotifications = true;
    
    @Builder.Default
    @Column(name = "sms_notifications")
    private Boolean smsNotifications = false;
    
    @Builder.Default
    @Column(name = "notification_frequency")
    @Enumerated(EnumType.STRING)
    private NotificationFrequency notificationFrequency = NotificationFrequency.IMMEDIATE;
    
    @Min(value = 10)
    @Max(value = 100)
    @Builder.Default
    @Column(name = "items_per_page")
    private Integer itemsPerPage = 20;
    
    @Builder.Default
    @Column(name = "auto_save")
    private Boolean autoSave = true;
    
    @Min(value = 30)
    @Max(value = 600)
    @Builder.Default
    @Column(name = "auto_save_interval")
    private Integer autoSaveInterval = 60;
    
    @Builder.Default
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;
    
    @Builder.Default
    @Column(name = "session_timeout")
    private Integer sessionTimeout = 1440;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-settings")
    private User user;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public Locale getLocale() {
        return Locale.forLanguageTag(languageCode);
    }
    
    public enum Theme {
        LIGHT, DARK, AUTO
    }
    
    public enum DateFormat {
        YYYY_MM_DD, DD_MM_YYYY, MM_DD_YYYY
    }
    
    public enum NotificationFrequency {
        IMMEDIATE, HOURLY, DAILY, WEEKLY, NEVER
    }
}