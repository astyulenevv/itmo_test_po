package com.example.integration.integration;

import com.example.integration.entity.Profile;
import com.example.integration.entity.User;
import com.example.integration.repository.ProfileRepository;
import com.example.integration.repository.UserRepository;
import com.example.integration.service.ProfileService;
import com.example.integration.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ProfileServiceIntegrationTest {
    
    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProfileRepository profileRepository;
    
    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    void shouldUpdateProfileAndSynchronizeWithUser() {
        User user = createTestUser("profileuser", "profile@example.com");
        Long userId = user.getId();
        
        User originalUser = userRepository.findById(userId).orElseThrow();
        Profile originalProfile = originalUser.getProfile();
        
        Profile profileUpdate = Profile.builder()
            .firstName("John")
            .lastName("Doe")
            .bio("Software Engineer")
            .location("New York")
            .birthDate(LocalDate.of(1990, 1, 15))
            .gender(Profile.Gender.MALE)
            .build();
        
        Profile updatedProfile = profileService.updateProfile(userId, profileUpdate);
        
        assertNotNull(updatedProfile);
        assertEquals("John", updatedProfile.getFirstName());
        assertEquals("Doe", updatedProfile.getLastName());
        assertEquals("Software Engineer", updatedProfile.getBio());
        assertEquals("New York", updatedProfile.getLocation());
        assertEquals(LocalDate.of(1990, 1, 15), updatedProfile.getBirthDate());
        assertEquals(Profile.Gender.MALE, updatedProfile.getGender());
        
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertTrue(updatedUser.getUpdatedAt().isAfter(originalUser.getCreatedAt()));
        
        assertTrue(updatedProfile.getUpdatedAt().isAfter(originalProfile.getCreatedAt()));
    }
    
    @Test
    void shouldSearchPublicProfilesWithPrivacyFiltering() {
        User activeUser = createTestUser("activeuser", "active@example.com");
        User suspendedUser = createTestUser("suspendeduser", "suspended@example.com");
        User privateUser = createTestUser("privateuser", "private@example.com");
        
        profileService.updateProfile(activeUser.getId(), Profile.builder()
            .firstName("Active")
            .lastName("User")
            .bio("I am active")
            .isPublic(true)
            .build());
        
        profileService.updateProfile(suspendedUser.getId(), Profile.builder()
            .firstName("Suspended")
            .lastName("User")
            .bio("I am suspended")
            .isPublic(true)
            .build());
        
        profileService.updateProfile(privateUser.getId(), Profile.builder()
            .firstName("Private")
            .lastName("User")
            .bio("I am private")
            .isPublic(false)
            .build());
        
        userService.updateUserStatus(suspendedUser.getId(), User.UserStatus.SUSPENDED);
        
        List<Profile> allPublicProfiles = profileService.searchPublicProfiles(null);
        List<Profile> userProfiles = profileService.searchPublicProfiles("User");
        
        assertEquals(1, allPublicProfiles.size());
        assertEquals("activeuser", allPublicProfiles.get(0).getUser().getUsername());
        
        assertEquals(1, userProfiles.size());
        assertEquals("Active User", userProfiles.get(0).getFullName());
    }
    
    @Test
    void shouldUpdateProfileVisibilityWithBusinessLogic() {
        User user = createTestUser("visibilityuser", "visibility@example.com");
        Long userId = user.getId();
        
        Optional<Profile> initialProfile = profileRepository.findByUserId(userId);
        assertTrue(initialProfile.isPresent());
        assertTrue(initialProfile.get().getIsPublic());
        
        Profile updatedProfile = profileService.updateProfileVisibility(userId, false);
        
        assertFalse(updatedProfile.getIsPublic());
        assertNotNull(updatedProfile.getUpdatedAt());
        
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertNotNull(updatedUser.getUpdatedAt());
        
        Profile publicProfile = profileService.updateProfileVisibility(userId, true);
        
        assertTrue(publicProfile.getIsPublic());
        assertTrue(publicProfile.getUpdatedAt().isAfter(updatedProfile.getUpdatedAt()));
    }
    
    @Test
    void shouldPerformDemographicQueriesAcrossProfiles() {
        User youngUser = createTestUser("young", "young@example.com");
        User middleAgedUser = createTestUser("middle", "middle@example.com");
        User olderUser = createTestUser("older", "older@example.com");
        
        profileService.updateProfile(youngUser.getId(), Profile.builder()
            .firstName("Young")
            .birthDate(LocalDate.of(2000, 1, 1))
            .isPublic(true)
            .build());
        
        profileService.updateProfile(middleAgedUser.getId(), Profile.builder()
            .firstName("Middle")
            .birthDate(LocalDate.of(1985, 1, 1))
            .isPublic(true)
            .build());
        
        profileService.updateProfile(olderUser.getId(), Profile.builder()
            .firstName("Older")
            .birthDate(LocalDate.of(1970, 1, 1))
            .isPublic(true)
            .build());
        
        List<Profile> young = profileService.getProfilesByAge(20, 30);
        List<Profile> middle = profileService.getProfilesByAge(35, 45);
        List<Profile> older = profileService.getProfilesByAge(50, 60);
        List<Profile> broad = profileService.getProfilesByAge(20, 60);
        
        assertEquals(1, young.size());
        assertEquals("Young", young.get(0).getFirstName());
        
        assertEquals(1, middle.size());
        assertEquals("Middle", middle.get(0).getFirstName());
        
        assertEquals(1, older.size());
        assertEquals("Older", older.get(0).getFirstName());
        
        assertEquals(3, broad.size());
    }
    
    @Test
    void shouldCalculateAccurateProfileCompletionStatistics() {
        User user = createTestUser("incomplete", "incomplete@example.com");
        Long userId = user.getId();
        
        ProfileService.ProfileCompletionStats initialStats = 
            profileService.getProfileCompletionStats(userId);
        
        assertTrue(initialStats.getCompletionPercentage() < 50);
        assertTrue(initialStats.getMissingFields().size() > 5);
        
        profileService.updateProfile(userId, Profile.builder()
            .firstName("Complete")
            .lastName("User")
            .bio("I am a complete user profile")
            .location("Complete City")
            .birthDate(LocalDate.of(1990, 1, 1))
            .phoneNumber("123-456-7890")
            .website("https://complete.example.com")
            .gender(Profile.Gender.OTHER)
            .profileImageUrl("https://images.example.com/profile.jpg")
            .build());
        
        ProfileService.ProfileCompletionStats completeStats = 
            profileService.getProfileCompletionStats(userId);
        
        assertEquals(100, completeStats.getCompletionPercentage());
        assertEquals(0, completeStats.getMissingFields().size());
        assertEquals(9, completeStats.getCompletedFields());
        assertEquals(9, completeStats.getTotalFields());
    }
    
    @Test
    void shouldHandleProfileOperationsForNonExistentUsers() {
        Profile profileUpdate = Profile.builder()
            .firstName("Non")
            .lastName("Existent")
            .build();
        
        assertThrows(IllegalArgumentException.class, 
            () -> profileService.updateProfile(999L, profileUpdate));
        
        Optional<Profile> profile = profileService.getProfileWithUserDetails(999L);
        assertTrue(profile.isEmpty());
        
        assertThrows(IllegalArgumentException.class, 
            () -> profileService.updateProfileVisibility(999L, false));
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