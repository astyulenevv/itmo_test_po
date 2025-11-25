package com.example.integration.integration;

import com.example.integration.entity.User;
import com.example.integration.repository.ProfileRepository;
import com.example.integration.repository.SettingsRepository;
import com.example.integration.repository.UserRepository;
import com.example.integration.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RestApiIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ProfileRepository profileRepository;
    
    @Autowired
    private SettingsRepository settingsRepository;
    
    @Autowired
    private UserService userService;
    
    @BeforeEach
    void setUp() {
        settingsRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }
    
    @Test
    void shouldCreateUserThroughRestApiWithCompleteIntegrationChain() throws Exception {
        User newUser = User.builder()
            .username("apiuser")
            .email("apiuser@example.com")
            .password("password123")
            .build();
        
        String userJson = objectMapper.writeValueAsString(newUser);
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("apiuser"))
                .andExpect(jsonPath("$.email").value("apiuser@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());
        
        assertTrue(userRepository.existsByUsername("apiuser"));
        assertTrue(userRepository.existsByEmail("apiuser@example.com"));
        
        User createdUser = userRepository.findByUsername("apiuser").orElseThrow();
        assertTrue(profileRepository.findByUserId(createdUser.getId()).isPresent());
        assertTrue(settingsRepository.findByUserId(createdUser.getId()).isPresent());
    }
    
    @Test
    void shouldHandleDuplicateUserCreationWithProperErrorResponses() throws Exception {
        User existingUser = User.builder()
            .username("duplicate")
            .email("duplicate@example.com")
            .password("password123")
            .build();
        
        userService.createUserWithDefaults(existingUser);
        
        User duplicateUsername = User.builder()
            .username("duplicate")
            .email("different@example.com")
            .password("password123")
            .build();
        
        String duplicateUsernameJson = objectMapper.writeValueAsString(duplicateUsername);
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateUsernameJson))
                .andExpect(status().isBadRequest());
        
        User duplicateEmail = User.builder()
            .username("different")
            .email("duplicate@example.com")
            .password("password123")
            .build();
        
        String duplicateEmailJson = objectMapper.writeValueAsString(duplicateEmail);
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateEmailJson))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void shouldRetrieveUserWithCompleteDetailsThroughRestApi() throws Exception {
        User user = User.builder()
            .username("detailuser")
            .email("detail@example.com")
            .password("password123")
            .build();
        
        User createdUser = userService.createUserWithDefaults(user);
        
        mockMvc.perform(get("/api/users/detailuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("detailuser"))
                .andExpect(jsonPath("$.email").value("detail@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").value(createdUser.getId()))
                .andExpect(jsonPath("$.createdAt").exists());
        
        mockMvc.perform(get("/api/users/detail@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("detailuser"));
        
        mockMvc.perform(get("/api/users/nonexistent"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldUpdateUserStatusThroughRestApiWithCascadeEffects() throws Exception {
        User user = createTestUser("statususer", "status@example.com");
        Long userId = user.getId();
        
        assertTrue(profileRepository.findByUserId(userId).get().getIsPublic());
        
        mockMvc.perform(put("/api/users/{userId}/status", userId)
                .param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.updatedAt").exists());
        
        assertFalse(profileRepository.findByUserId(userId).get().getIsPublic());
        
        mockMvc.perform(put("/api/users/{userId}/status", userId)
                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
    
    @Test
    void shouldDeleteUserThroughRestApiWithCascadingOperations() throws Exception {
        User user = createTestUser("deleteuser", "delete@example.com");
        Long userId = user.getId();
        
        assertTrue(userRepository.findById(userId).isPresent());
        assertTrue(profileRepository.findByUserId(userId).isPresent());
        assertTrue(settingsRepository.findByUserId(userId).isPresent());
        
        mockMvc.perform(delete("/api/users/{userId}", userId))
                .andExpect(status().isNoContent());
        
        assertFalse(userRepository.findById(userId).isPresent());
        assertFalse(profileRepository.findByUserId(userId).isPresent());
        assertFalse(settingsRepository.findByUserId(userId).isPresent());
        
        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldRetrieveActiveUsersWithPublicProfilesThroughRestApi() throws Exception {
        User activePublic = createTestUser("activepublic", "activepublic@example.com");
        User activePrivate = createTestUser("activeprivate", "activeprivate@example.com");
        User suspendedUser = createTestUser("suspended", "suspended@example.com");
        
        profileRepository.findByUserId(activePrivate.getId()).ifPresent(profile -> {
            profile.setIsPublic(false);
            profileRepository.save(profile);
        });
        
        userService.updateUserStatus(suspendedUser.getId(), User.UserStatus.SUSPENDED);
        
        mockMvc.perform(get("/api/users/active-public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("activepublic"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }
    
    @Test
    void shouldRetrieveUserStatisticsThroughRestApi() throws Exception {
        User active1 = createTestUser("active1", "active1@example.com");
        User active2 = createTestUser("active2", "active2@example.com");
        User suspended = createTestUser("suspended", "suspended@example.com");
        
        settingsRepository.findByUserId(active1.getId()).ifPresent(settings -> {
            settings.setEmailNotifications(true);
            settings.setTwoFactorEnabled(true);
            settingsRepository.save(settings);
        });
        
        settingsRepository.findByUserId(active2.getId()).ifPresent(settings -> {
            settings.setEmailNotifications(true);
            settingsRepository.save(settings);
        });
        
        userService.updateUserStatus(suspended.getId(), User.UserStatus.SUSPENDED);
        
        mockMvc.perform(get("/api/users/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.activeUsers").value(2))
                .andExpect(jsonPath("$.suspendedUsers").value(1))
                .andExpect(jsonPath("$.deletedUsers").value(0))
                .andExpect(jsonPath("$.publicProfiles").value(2))
                .andExpect(jsonPath("$.privateProfiles").value(1))
                .andExpect(jsonPath("$.usersWithEmailNotifications").value(2))
                .andExpect(jsonPath("$.usersWithTwoFactor").value(1));
    }
    
    @Test
    void shouldHandleInvalidRequestsWithProperHttpStatusCodes() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json"))
                .andExpect(status().isBadRequest());
        
        User invalidUser = User.builder()
            .username("")
            .email("invalid-email")
            .password("123")
            .build();
        
        String invalidUserJson = objectMapper.writeValueAsString(invalidUser);
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUserJson))
                .andExpect(status().isBadRequest());
        
        mockMvc.perform(get("/api/nonexistent"))
                .andExpect(status().isNotFound());
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