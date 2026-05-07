package com.forge.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forge.platform.dto.AuthResponse;
import com.forge.platform.dto.LoginRequest;
import com.forge.platform.dto.UserCreateDto;
import com.forge.platform.entity.User;
import com.forge.platform.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Filters disabled, no 403 headache
@ActiveProfiles("test") // Bypass initData
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // 🔥 Wahi apne Context-saving Mocks taaki Spring chup-chaap load ho jaye
    @MockBean
    private com.forge.platform.security.JwtService jwtService;

    @MockBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void register_Success() throws Exception {
        // Direct JSON string bhejna sabse safe hai taaki DTO validation me issue na aaye
        String jsonRequest = "{\"email\": \"newuser@forge.com\", \"password\": \"password123\", \"fullName\": \"Test User\"}";

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("newuser@forge.com");

        when(authService.register(any(UserCreateDto.class))).thenReturn(mockUser);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk()); // Controller code bas 200 OK bhej raha hai, wahi check karenge

        verify(authService).register(any(UserCreateDto.class));
    }

    @Test
    void login_Success() throws Exception {
        String jsonRequest = "{\"email\": \"sensei@forge.com\", \"password\": \"password123\"}";

        // AuthResponse ka mock direct le rahe hain taaki uske internal structure se farq na pade
        AuthResponse mockResponse = mock(AuthResponse.class);

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk());

        verify(authService).login(any(LoginRequest.class));
    }
}