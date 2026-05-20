package com.forge.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.dto.UserRequestDto;
import com.forge.platform.dto.UserResponseDto;
import com.forge.platform.entity.User;
import com.forge.platform.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private com.forge.platform.security.JwtService jwtService;
    @MockBean private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    private User testUser;
    private UserResponseDto testProfileDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("sensei@forge.com")
                .password("hashed_password")
                .fullName("Sensei Rajput")
                .build();
        testUser.setId(1L);

        testProfileDto = new UserResponseDto(
                1L,
                "sensei@forge.com",
                "Sensei Rajput",
                new BigDecimal("5000.00"),
                LocalDateTime.now()
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── POST /api/v1/users/register ───────────────────────────────────────────

    @Test
    void register_returns200_withSavedUser() throws Exception {
        UserCreateDto dto = UserCreateDto.builder()
                .email("new@forge.com")
                .password("password123")
                .fullName("New User")
                .build();

        User savedUser = User.builder()
                .email("new@forge.com")
                .password("hashed")
                .fullName("New User")
                .build();
        savedUser.setId(2L);

        when(userService.registerUser(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@forge.com"))
                .andExpect(jsonPath("$.fullName").value("New User"));

        verify(userService).registerUser(any(User.class));
    }

    @Test
    void register_callsService_withCorrectUserFields() throws Exception {
        UserCreateDto dto = UserCreateDto.builder()
                .email("check@forge.com")
                .password("pass123")
                .fullName("Check User")
                .build();

        when(userService.registerUser(any(User.class))).thenReturn(testUser);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userService).registerUser(argThat(u ->
                u.getEmail().equals("check@forge.com") &&
                        u.getFullName().equals("Check User")
        ));
    }

    // ── GET /api/v1/users/me ──────────────────────────────────────────────────

    @Test
    void getProfile_returns200_withUserDto() throws Exception {
        when(userService.getUserProfile(testUser)).thenReturn(testProfileDto);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("sensei@forge.com"))
                .andExpect(jsonPath("$.fullName").value("Sensei Rajput"))
                .andExpect(jsonPath("$.walletBalance").value(5000.00));
    }

    @Test
    void getProfile_callsServiceWithAuthenticatedUser() throws Exception {
        when(userService.getUserProfile(testUser)).thenReturn(testProfileDto);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk());

        verify(userService).getUserProfile(testUser);
    }

    // ── PUT /api/v1/users/me ──────────────────────────────────────────────────

    @Test
    void updateProfile_returns200_withUpdatedDto() throws Exception {
        UserRequestDto dto = new UserRequestDto();
        dto.setFullName("Updated Sensei");

        UserResponseDto updated = new UserResponseDto(
                1L, "sensei@forge.com", "Updated Sensei",
                new BigDecimal("5000.00"), LocalDateTime.now()
        );
        when(userService.updateProfile(eq(testUser), any(UserRequestDto.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Sensei"));
    }

    @Test
    void updateProfile_passesCorrectDtoToService() throws Exception {
        UserRequestDto dto = new UserRequestDto();
        dto.setFullName("Ketansingh");

        when(userService.updateProfile(any(), any())).thenReturn(testProfileDto);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userService).updateProfile(eq(testUser), argThat(d ->
                "Ketansingh".equals(d.getFullName())
        ));
    }

    // ── PUT /api/v1/users/me/password ─────────────────────────────────────────

    @Test
    void changePassword_returns200_whenSuccess() throws Exception {
        UserRequestDto dto = new UserRequestDto();
        dto.setCurrentPassword("oldpass");
        dto.setNewPassword("newpass123");

        doNothing().when(userService).changePassword(eq(testUser), any(UserRequestDto.class));

        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userService).changePassword(eq(testUser), any());
    }

    @Test
    void changePassword_returns500_whenCurrentPasswordWrong() throws Exception {
        UserRequestDto dto = new UserRequestDto();
        dto.setCurrentPassword("wrongpass");
        dto.setNewPassword("newpass123");

        doThrow(new RuntimeException("Current password incorrect"))
                .when(userService).changePassword(eq(testUser), any());

        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().is5xxServerError());
    }
}