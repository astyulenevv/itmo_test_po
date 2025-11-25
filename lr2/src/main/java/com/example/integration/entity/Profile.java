package com.example.integration.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import javax.validation.constraints.Past;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Size(max = 100)
    @Column(name = "first_name")
    private String firstName;
    
    @Size(max = 100)
    @Column(name = "last_name")
    private String lastName;
    
    @Past
    @Column(name = "birth_date")
    private LocalDate birthDate;
    
    @Size(max = 500)
    private String bio;
    
    @Size(max = 100)
    private String location;
    
    @Size(max = 255)
    private String website;
    
    @Size(max = 20)
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Gender gender = Gender.NOT_SPECIFIED;
    
    @Column(name = "profile_image_url")
    private String profileImageUrl;
    
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = true;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-profile")
    private User user;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return null;
    }
    
    public enum Gender {
        MALE, FEMALE, OTHER, NOT_SPECIFIED
    }
}