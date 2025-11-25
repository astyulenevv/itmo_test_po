package com.example.integration.integration;

import com.example.integration.entity.Profile;
import com.example.integration.entity.Settings;
import com.example.integration.entity.User;
import com.example.integration.repository.ProfileRepository;
import com.example.integration.repository.SettingsRepository;
import com.example.integration.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class RepositoryIntegrationTest {
    
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
    void shouldPersistAndRetrieveEntitiesWithProperRelationships() {
        User user = User.builder()
            .username("repouser")
            .email("repo@example.com")
            .password("password123")
            .status(User.UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .build();
        
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId());
        
        Profile profile = Profile.builder()
            .user(savedUser)
            .firstName("Repository")
            .lastName("User")
            .bio("Testing repository integration")
            .isPublic(true)
            .createdAt(LocalDateTime.now())
            .build();
        
        Profile savedProfile = profileRepository.save(profile);
        assertNotNull(savedProfile.getId());
        
        Settings settings = Settings.builder()
            .user(savedUser)
            .theme(Settings.Theme.DARK)
            .languageCode("es")
            .emailNotifications(false)
            .twoFactorEnabled(true)
            .createdAt(LocalDateTime.now())
            .build();
        
        Settings savedSettings = settingsRepository.save(settings);
        assertNotNull(savedSettings.getId());
        
        Optional<User> retrievedUser = userRepository.findById(savedUser.getId());
        assertTrue(retrievedUser.isPresent());
        assertEquals("repouser", retrievedUser.get().getUsername());
        
        Optional<Profile> retrievedProfile = profileRepository.findByUserId(savedUser.getId());
        assertTrue(retrievedProfile.isPresent());
        assertEquals("Repository User", retrievedProfile.get().getFullName());
        assertEquals(savedUser.getId(), retrievedProfile.get().getUser().getId());
        
        Optional<Settings> retrievedSettings = settingsRepository.findByUserId(savedUser.getId());
        assertTrue(retrievedSettings.isPresent());
        assertEquals(Settings.Theme.DARK, retrievedSettings.get().getTheme());
        assertEquals(savedUser.getId(), retrievedSettings.get().getUser().getId());
    }
    
    @Test
    void shouldExecuteComplexJpqlQueriesAcrossMultipleEntities() {
        User user1 = createUserWithProfileAndSettings("user1", "user1@example.com", 
            "User", "One", true, Settings.Theme.DARK);
        User user2 = createUserWithProfileAndSettings("user2", "user2@example.com", 
            "User", "Two", true, Settings.Theme.LIGHT);
        User user3 = createUserWithProfileAndSettings("user3", "user3@example.com", 
            "User", "Three", false, Settings.Theme.DARK);
        
        List<User> activeUsersWithProfiles = userRepository.findActiveUsersWithProfiles();
        List<Profile> publicProfilesWithUserDetails = profileRepository.findPublicProfilesWithUserDetails();
        List<Settings> notificationSettings = settingsRepository.findSettingsWithNotificationsEnabled();
        
        assertEquals(3, activeUsersWithProfiles.size());
        assertEquals(2, publicProfilesWithUserDetails.size());
        
        assertTrue(publicProfilesWithUserDetails.stream()
            .anyMatch(p -> "User One".equals(p.getFullName())));
        assertTrue(publicProfilesWithUserDetails.stream()
            .anyMatch(p -> "User Two".equals(p.getFullName())));
        assertFalse(publicProfilesWithUserDetails.stream()
            .anyMatch(p -> "User Three".equals(p.getFullName())));
    }
    
    @Test
    void shouldHandleCustomQueryMethodsWithParameters() {
        User youngUser = createUserWithProfileAndSettings("young", "young@example.com",
            "Young", "User", true, Settings.Theme.LIGHT);
        Profile youngProfile = youngUser.getProfile();
        youngProfile.setBirthDate(LocalDate.of(2000, 1, 1));
        youngProfile.setLocation("New York");
        profileRepository.save(youngProfile);
        
        User oldUser = createUserWithProfileAndSettings("old", "old@example.com",
            "Old", "User", true, Settings.Theme.DARK);
        Profile oldProfile = oldUser.getProfile();
        oldProfile.setBirthDate(LocalDate.of(1980, 1, 1));
        oldProfile.setLocation("Los Angeles");
        profileRepository.save(oldProfile);
        
        LocalDate startDate = LocalDate.of(1999, 1, 1);
        LocalDate endDate = LocalDate.of(2001, 1, 1);
        List<Profile> profilesInAgeRange = profileRepository.findByBirthDateBetween(startDate, endDate);
        
        List<Profile> nyProfiles = profileRepository.findByLocationContainingIgnoreCase("new york");
        List<Profile> laProfiles = profileRepository.findByLocationContainingIgnoreCase("angeles");
        
        assertEquals(1, profilesInAgeRange.size());
        assertEquals("Young User", profilesInAgeRange.get(0).getFullName());
        
        assertEquals(1, nyProfiles.size());
        assertEquals("Young User", nyProfiles.get(0).getFullName());
        
        assertEquals(1, laProfiles.size());
        assertEquals("Old User", laProfiles.get(0).getFullName());
    }
    
    @Test
    void shouldHandleDatabaseConstraintsAndValidation() {
        User user1 = User.builder()
            .username("unique1")
            .email("unique1@example.com")
            .password("password123")
            .build();
        
        userRepository.save(user1);
        
        assertTrue(userRepository.existsByUsername("unique1"));
        assertFalse(userRepository.existsByUsername("nonexistent"));
        
        assertTrue(userRepository.existsByEmail("unique1@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
        
        Optional<User> foundByUsername = userRepository.findByUsernameOrEmail("unique1");
        assertTrue(foundByUsername.isPresent());
        assertEquals("unique1", foundByUsername.get().getUsername());
        
        Optional<User> foundByEmail = userRepository.findByUsernameOrEmail("unique1@example.com");
        assertTrue(foundByEmail.isPresent());
        assertEquals("unique1@example.com", foundByEmail.get().getEmail());
        
        Optional<User> notFound = userRepository.findByUsernameOrEmail("nonexistent");
        assertTrue(notFound.isEmpty());
    }
    
    @Test
    void shouldPerformAggregationAndCountingOperations() {
        User activeUser1 = createUserWithProfileAndSettings("active1", "active1@example.com",
            "Active", "One", true, Settings.Theme.LIGHT);
        User activeUser2 = createUserWithProfileAndSettings("active2", "active2@example.com",
            "Active", "Two", false, Settings.Theme.DARK);
        User inactiveUser = User.builder()
            .username("inactive")
            .email("inactive@example.com")
            .password("password123")
            .status(User.UserStatus.INACTIVE)
            .build();
        userRepository.save(inactiveUser);
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByStatus(User.UserStatus.INACTIVE);
        
        long publicProfiles = profileRepository.countByIsPublic(true);
        long privateProfiles = profileRepository.countByIsPublic(false);
        
        long lightThemeSettings = settingsRepository.countByTheme(Settings.Theme.LIGHT);
        long darkThemeSettings = settingsRepository.countByTheme(Settings.Theme.DARK);
        
        assertEquals(3, totalUsers);
        assertEquals(2, activeUsers);
        assertEquals(1, inactiveUsers);
        assertEquals(1, publicProfiles);
        assertEquals(1, privateProfiles);
        assertEquals(1, lightThemeSettings);
        assertEquals(1, darkThemeSettings);
    }
    
    @Test
    void shouldHandleCascadeOperationsAndEntityLifecycle() {
        User user = createUserWithProfileAndSettings("cascade", "cascade@example.com",
            "Cascade", "Test", true, Settings.Theme.AUTO);
        
        Long userId = user.getId();
        Long profileId = user.getProfile().getId();
        Long settingsId = user.getSettings().getId();
        
        assertTrue(userRepository.findById(userId).isPresent());
        assertTrue(profileRepository.findById(profileId).isPresent());
        assertTrue(settingsRepository.findById(settingsId).isPresent());
        
        userRepository.delete(user);
        userRepository.flush();
        
        assertTrue(userRepository.findById(userId).isEmpty());
        assertTrue(profileRepository.findById(profileId).isEmpty());
        assertTrue(settingsRepository.findById(settingsId).isEmpty());
    }
    
    @Test
    void shouldHandleSearchAndFilteringOperations() {
        User user1 = createUserWithProfileAndSettings("search1", "search1@example.com",
            "John", "Doe", true, Settings.Theme.LIGHT);
        user1.getProfile().setBio("Software Engineer at Tech Company");
        profileRepository.save(user1.getProfile());
        
        User user2 = createUserWithProfileAndSettings("search2", "search2@example.com",
            "Jane", "Smith", true, Settings.Theme.DARK);
        user2.getProfile().setBio("Product Manager");
        profileRepository.save(user2.getProfile());
        
        User user3 = createUserWithProfileAndSettings("search3", "search3@example.com",
            "Bob", "Johnson", false, Settings.Theme.AUTO);
        user3.getProfile().setBio("Data Scientist");
        profileRepository.save(user3.getProfile());
        
        List<Profile> johnProfiles = profileRepository.searchByName("John");
        List<Profile> jProfiles = profileRepository.searchByName("J");
        List<Profile> completeProfiles = profileRepository.findCompleteProfiles();
        
        assertEquals(2, johnProfiles.size());
        assertEquals(3, jProfiles.size());
        assertEquals(3, completeProfiles.size());
        
        assertTrue(johnProfiles.stream().anyMatch(p -> "John Doe".equals(p.getFullName())));
        assertTrue(johnProfiles.stream().anyMatch(p -> "Bob Johnson".equals(p.getFullName())));
    }
    
    private User createUserWithProfileAndSettings(String username, String email, 
            String firstName, String lastName, boolean isPublic, Settings.Theme theme) {
        
        User user = User.builder()
            .username(username)
            .email(email)
            .password("password123")
            .status(User.UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .build();
        User savedUser = userRepository.save(user);
        
        Profile profile = Profile.builder()
            .user(savedUser)
            .firstName(firstName)
            .lastName(lastName)
            .bio("Bio for " + firstName + " " + lastName)
            .isPublic(isPublic)
            .createdAt(LocalDateTime.now())
            .build();
        Profile savedProfile = profileRepository.save(profile);
        
        Settings settings = Settings.builder()
            .user(savedUser)
            .theme(theme)
            .languageCode("en")
            .emailNotifications(true)
            .twoFactorEnabled(false)
            .createdAt(LocalDateTime.now())
            .build();
        Settings savedSettings = settingsRepository.save(settings);
        
        savedUser.setProfile(savedProfile);
        savedUser.setSettings(savedSettings);
        
        return savedUser;
    }
}