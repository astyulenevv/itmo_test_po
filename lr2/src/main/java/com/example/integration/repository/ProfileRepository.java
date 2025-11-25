package com.example.integration.repository;

import com.example.integration.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {
    
    Optional<Profile> findByUserId(Long userId);
    
    @Query("SELECT p FROM Profile p JOIN p.user u WHERE u.username = :username")
    Optional<Profile> findByUserUsername(@Param("username") String username);
    
    List<Profile> findByIsPublicTrue();
    
    List<Profile> findByLocationContainingIgnoreCase(String location);
    
    @Query("SELECT p FROM Profile p WHERE p.birthDate BETWEEN :startDate AND :endDate")
    List<Profile> findByBirthDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT p FROM Profile p WHERE p.firstName IS NOT NULL " +
           "AND p.lastName IS NOT NULL AND p.bio IS NOT NULL")
    List<Profile> findCompleteProfiles();
    
    List<Profile> findByGender(Profile.Gender gender);
    
    List<Profile> findByWebsiteIsNotNull();
    
    @Query("SELECT p FROM Profile p WHERE " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Profile> searchByName(@Param("name") String name);
    
    Long countByIsPublic(Boolean isPublic);
    
    @Query("SELECT p FROM Profile p JOIN FETCH p.user WHERE p.isPublic = true")
    List<Profile> findPublicProfilesWithUserDetails();
}