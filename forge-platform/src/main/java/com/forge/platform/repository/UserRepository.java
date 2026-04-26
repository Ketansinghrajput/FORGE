package com.forge.platform.repository;

import com.forge.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Login ke liye ye chahiye tha (jo pehle se hoga shayad)
    Optional<User> findByEmail(String email);

    // Ye line missing thi jiska error aa raha hai!
    boolean existsByEmail(String email);
}