package com.carpooling.controller;

import com.carpooling.dto.request.CarRequestDTO;
import com.carpooling.dto.request.JourneyRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    @BeforeEach
    void setUp() throws Exception {
        List<CarRequestDTO> cars = List.of(new CarRequestDTO(1, 4), new CarRequestDTO(2, 6));
        mockMvc.perform(put("/api/v1/cars")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());
    }

    @Test
    void loadCarsRejects7Seats() throws Exception {
        String body = "[{\"id\":1,\"seats\":7}]";
        mockMvc.perform(put("/api/v1/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void journeyAssignsCar() throws Exception {
        JourneyRequestDTO j = new JourneyRequestDTO(1, 3);
        mockMvc.perform(post("/api/v1/journeys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(j)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/journeys/1/car"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.seats").exists());
    }

    @Test
    void journeyGoesToWaitingQueue() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(1, 4))));
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(2, 6))));
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(3, 3))));

        mockMvc.perform(get("/api/v1/journeys/3/car"))
                .andExpect(status().isNoContent());
    }

    @Test
    void dropoffFreesSeatsAndAssignsWaiting() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(1, 4))));
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(2, 6))));
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(3, 3))));

        mockMvc.perform(delete("/api/v1/journeys/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/journeys/3/car"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void dropoffOfUnknownGroupReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/journeys/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void locateOfUnknownGroupReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/journeys/999/car"))
                .andExpect(status().isNotFound());
    }

    @Test
    void dropoffOfWaitingGroupRemovesFromQueue() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(1, 4))));
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(2, 6))));
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(3, 3))));

        mockMvc.perform(delete("/api/v1/journeys/3"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/journeys/3/car"))
                .andExpect(status().isNotFound());
    }

    @Test
    void fairnessSmallGroupServedBeforeLargeWhenNoCarForLarge() throws Exception {
        List<CarRequestDTO> cars = List.of(new CarRequestDTO(1, 4));
        mockMvc.perform(put("/api/v1/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cars)));

        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(1, 4))));
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(2, 6))));
        mockMvc.perform(post("/api/v1/journeys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new JourneyRequestDTO(3, 2))));

        mockMvc.perform(delete("/api/v1/journeys/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/journeys/2/car"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/journeys/3/car"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void journeyRejectsPeopleOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"people\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/journeys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":2,\"people\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateJourneyReturns409() throws Exception {
        mockMvc.perform(post("/api/v1/journeys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"people\":3}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/journeys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"people\":2}"))
                .andExpect(status().isConflict());
    }

    @Test
    void nonNumericPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/journeys/abc/car"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        mockMvc.perform(delete("/api/v1/journeys/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void negativePathVariableReturns400() throws Exception {
        mockMvc.perform(delete("/api/v1/journeys/-5"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/journeys/-1/car"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyCarListReturns400() throws Exception {
        mockMvc.perform(put("/api/v1/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(post("/api/v1/status"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
