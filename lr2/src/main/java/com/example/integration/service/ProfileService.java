package com.example.integration.service;

import com.example.integration.entity.Profile;
import com.example.integration.entity.User;
import com.example.integration.repository.ProfileRepository;
import com.example.integration.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
    
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public Profile updateProfile(Long userId, @Valid Profile profileUpdate) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        Profile existingProfile = user.getProfile();
        if (existingProfile == null) {
            profileUpdate.setUser(user);
            profileUpdate.setCreatedAt(LocalDateTime.now());
            Profile savedProfile = profileRepository.save(profileUpdate);
            
            user.setProfile(savedProfile);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            return savedProfile;
        }
        
        updateProfileFields(existingProfile, profileUpdate);
        existingProfile.setUpdatedAt(LocalDateTime.now());
        
        Profile savedProfile = profileRepository.save(existingProfile);
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return savedProfile;
    }
    
    @Transactional(readOnly = true)
    public Optional<Profile> getProfileWithUserDetails(Long userId) {
        return profileRepository.findByUserId(userId);
    }
    
    @Transactional(readOnly = true)
    public List<Profile> searchPublicProfiles(String searchTerm) {
        List<Profile> profiles;
        
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            profiles = profileRepository.searchByName(searchTerm.trim());
        } else {
            profiles = profileRepository.findByIsPublicTrue();
        }
        
        return profiles.stream()
            .filter(profile -> profile.getUser().getStatus() == User.UserStatus.ACTIVE)
            .filter(Profile::getIsPublic)
            .toList();
    }
    
    @Transactional
    public Profile updateProfileVisibility(Long userId, boolean isPublic) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        Profile profile = user.getProfile();
        if (profile == null) {
            throw new IllegalArgumentException("Profile not found for user: " + userId);
        }
        
        profile.setIsPublic(isPublic);
        profile.setUpdatedAt(LocalDateTime.now());
        
        Profile savedProfile = profileRepository.save(profile);
        
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        return savedProfile;
    }
    
    @Transactional(readOnly = true)
    public List<Profile> getProfilesByAge(int minAge, int maxAge) {
        LocalDate maxBirthDate = LocalDate.now().minusYears(minAge);
        LocalDate minBirthDate = LocalDate.now().minusYears(maxAge + 1);
        
        List<Profile> profiles = profileRepository.findByBirthDateBetween(minBirthDate, maxBirthDate);
        
        return profiles.stream()
            .filter(profile -> profile.getIsPublic())
            .filter(profile -> profile.getUser().getStatus() == User.UserStatus.ACTIVE)
            .toList();
    }
    
    @Transactional(readOnly = true)
    public ProfileCompletionStats getProfileCompletionStats(Long userId) {
        Optional<Profile> profileOpt = profileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return ProfileCompletionStats.builder()
                .completionPercentage(0)
                .missingFields(List.of("Profile not created"))
                .build();
        }
        
        Profile profile = profileOpt.get();
        int totalFields = 9;
        int completedFields = 0;
        List<String> missingFields = new java.util.ArrayList<>();
        
        if (profile.getFirstName() != null && !profile.getFirstName().isEmpty()) completedFields++;
        else missingFields.add("First Name");
        
        if (profile.getLastName() != null && !profile.getLastName().isEmpty()) completedFields++;
        else missingFields.add("Last Name");
        
        if (profile.getBio() != null && !profile.getBio().isEmpty()) completedFields++;
        else missingFields.add("Bio");
        
        if (profile.getLocation() != null && !profile.getLocation().isEmpty()) completedFields++;
        else missingFields.add("Location");
        
        if (profile.getBirthDate() != null) completedFields++;
        else missingFields.add("Birth Date");
        
        if (profile.getPhoneNumber() != null && !profile.getPhoneNumber().isEmpty()) completedFields++;
        else missingFields.add("Phone Number");
        
        if (profile.getWebsite() != null && !profile.getWebsite().isEmpty()) completedFields++;
        else missingFields.add("Website");
        
        if (profile.getProfileImageUrl() != null && !profile.getProfileImageUrl().isEmpty()) completedFields++;
        else missingFields.add("Profile Image");
        
        if (profile.getGender() != Profile.Gender.NOT_SPECIFIED) completedFields++;
        else missingFields.add("Gender");
        
        int completionPercentage = (int) ((double) completedFields / totalFields * 100);
        
        return ProfileCompletionStats.builder()
            .completionPercentage(completionPercentage)
            .completedFields(completedFields)
            .totalFields(totalFields)
            .missingFields(missingFields)
            .build();
    }
    
    private void updateProfileFields(Profile existing, Profile update) {
        if (update.getFirstName() != null) existing.setFirstName(update.getFirstName());
        if (update.getLastName() != null) existing.setLastName(update.getLastName());
        if (update.getBio() != null) existing.setBio(update.getBio());
        if (update.getLocation() != null) existing.setLocation(update.getLocation());
        if (update.getBirthDate() != null) existing.setBirthDate(update.getBirthDate());
        if (update.getPhoneNumber() != null) existing.setPhoneNumber(update.getPhoneNumber());
        if (update.getWebsite() != null) existing.setWebsite(update.getWebsite());
        if (update.getProfileImageUrl() != null) existing.setProfileImageUrl(update.getProfileImageUrl());
        if (update.getGender() != null) existing.setGender(update.getGender());
        if (update.getIsPublic() != null) existing.setIsPublic(update.getIsPublic());
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ProfileCompletionStats {
        private int completionPercentage;
        private int completedFields;
        private int totalFields;
        private List<String> missingFields;
    }
}