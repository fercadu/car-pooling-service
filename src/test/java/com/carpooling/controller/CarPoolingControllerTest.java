package com.carpooling.controller;

import com.carpooling.dto.request.CarRequestDTO;
import com.carpooling.dto.request.JourneyRequestDTO;
import com.carpooling.dto.request.LoginRequestDTO;
import com.carpooling.dto.response.AuthResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CarPoolingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    private String obtainAdminToken() throws Exception {
        LoginRequestDTO login = new LoginRequestDTO("admin", "admin123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponseDTO auth = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponseDTO.class);
        return auth.token();
    }

    @BeforeEach
    void setUp() throws Exception {
        adminToken = obtainAdminToken();
        List<CarRequestDTO> cars = List.of(new CarRequestDTO(1, 4), new CarRequestDTO(2, 6));
        mockMvc.perform(put("/api/v1/cars")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());
    }

    @Test
    void statusReturns200() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk());
    }

    @Test
    void loadCarsReturns200() throws Exception {
        List<CarRequestDTO> cars = List.of(new CarRequestDTO(10, 5));
        mockMvc.perform(put("/api/v1/cars")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());
    }

    @Test
    void loadCarsRejects7Seats() throws Exception {
        String body = "[{\"id\":1,\"seats\":7}]";
        mockMvc.perform(put("/api/v1/cars")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void journeyAssignsCar() throws Exception {
        JourneyRequestDTO j = new JourneyRequestDTO(1, 3);
        mockMvc.perform(post("/api/v1/journeys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(j)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/journeys/1/car")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.seats").exists());
    }

    @Test
    void journeyGoesToWaitingQueue() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(1, 4))));
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(2, 6))));
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(3, 3))));

        mockMvc.perform(get("/api/v1/journeys/3/car")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void dropoffFreesSeatsAndAssignsWaiting() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(1, 4))));
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(2, 6))));
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(3, 3))));

        mockMvc.perform(delete("/api/v1/journeys/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/journeys/3/car")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void dropoffOfUnknownGroupReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/journeys/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void locateOfUnknownGroupReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/journeys/999/car")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void dropoffOfWaitingGroupRemovesFromQueue() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(1, 4))));
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(2, 6))));
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(3, 3))));

        mockMvc.perform(delete("/api/v1/journeys/3")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/journeys/3/car")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void fairnessSmallGroupServedBeforeLargeWhenNoCarForLarge() throws Exception {
        List<CarRequestDTO> cars = List.of(new CarRequestDTO(1, 4));
        mockMvc.perform(put("/api/v1/cars")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cars)));

        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(1, 4))));
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(2, 6))));
        mockMvc.perform(post("/api/v1/journeys")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(3, 2))));

        mockMvc.perform(delete("/api/v1/journeys/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/journeys/2/car")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/journeys/3/car")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void journeyRejectsPeopleOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"people\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/journeys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":2,\"people\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateJourneyReturns409() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"people\":3}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/journeys")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"people\":2}"))
                .andExpect(status().isConflict());
    }

    @Test
    void nonNumericPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/journeys/abc/car")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        mockMvc.perform(delete("/api/v1/journeys/abc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void negativePathVariableReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/journeys/-5")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/journeys/-1/car")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyCarListReturns400() throws Exception {
        mockMvc.perform(put("/api/v1/cars")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(post("/api/v1/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    // ── Auth & Security tests ──

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(put("/api/v1/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":1,\"seats\":4}]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void observerCannotLoadCars() throws Exception {
        // Register a new user (gets OBSERVER role by default)
        String regBody = "{\"username\":\"viewer1\",\"password\":\"pass123\",\"email\":\"v@test.com\"}";
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regBody))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponseDTO auth = objectMapper.readValue(
                regResult.getResponse().getContentAsString(), AuthResponseDTO.class);

        mockMvc.perform(put("/api/v1/cars")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":1,\"seats\":4}]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginWithInvalidCredentialsReturns401() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"wrongpassword\"}";
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerDuplicateUsernameReturns400() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"pass123\"}";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"));
    }
}
