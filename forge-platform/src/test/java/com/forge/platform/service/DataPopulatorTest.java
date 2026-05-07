package com.forge.platform.service;

import com.forge.platform.entity.User;
import com.forge.platform.repository.UserRepository;
import com.forge.platform.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataPopulatorTest {

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;

    @InjectMocks private DataPopulator dataPopulator;

    @Test
    void run_ShouldPopulateData_WhenUserDoesNotExist() throws Exception {
        when(userRepository.findByEmail("sensei@forge.com")).thenReturn(Optional.empty());

        User savedUser = User.builder().id(1L).email("sensei@forge.com").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        dataPopulator.run();

        verify(userRepository).save(any(User.class));
        verify(walletRepository).save(any());
    }

    @Test
    void run_ShouldSkip_WhenUserExists() throws Exception {
        when(userRepository.findByEmail("sensei@forge.com")).thenReturn(Optional.of(new User()));

        dataPopulator.run();

        verify(userRepository, never()).save(any());
        verify(walletRepository, never()).save(any());
    }
}