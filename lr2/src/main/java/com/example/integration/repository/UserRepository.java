package com.example.integration.repository;

import com.example.integration.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);
    
    List<User> findByStatus(User.UserStatus status);
    
    List<User> findByCreatedAtAfter(LocalDateTime date);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.profile WHERE u.status = 'ACTIVE'")
    List<User> findActiveUsersWithProfiles();
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.settings s WHERE s.theme = :theme")
    List<User> findUsersByThemePreference(@Param("theme") String theme);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    Long countByStatus(@Param("status") User.UserStatus status);
}