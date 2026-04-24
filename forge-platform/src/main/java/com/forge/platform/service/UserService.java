package com.forge.platform.service;

import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.dto.UserResponseDto;
import com.forge.platform.entity.User;
import com.forge.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponseDto registerUser(UserCreateDto dto) {
        // 1. Duplicate Email Check
        if (userRepository.findByEmail(dto.email()).isPresent()) {
            throw new RuntimeException("Sensei, ye email pehle se registered hai!");
        }

        // 2. Map DTO to Entity
        User user = new User();
        user.setEmail(dto.email());
        user.setPassword(dto.password());
        user.setFullName(dto.fullName());

        // 3. Save to DB
        User savedUser = userRepository.save(user);

        // 4. Map Entity back to safe Response DTO (No Password!)
        System.out.println("ID: " + savedUser.getId() + ", Date: " + savedUser.getCreatedAt());
        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getWalletBalance(),
                savedUser.getCreatedAt()
        );
    }
}