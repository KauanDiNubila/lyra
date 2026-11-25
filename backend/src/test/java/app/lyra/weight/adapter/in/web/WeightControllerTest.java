package app.lyra.weight.adapter.in.web;

import app.lyra.SignedInControllerTest;
import app.lyra.weight.application.port.in.DeleteWeightUseCase;
import app.lyra.weight.application.port.in.ListWeightsUseCase;
import app.lyra.weight.application.port.in.RecordWeightUseCase;
import app.lyra.weight.domain.WeightEntry;
import app.lyra.weight.domain.WeightNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WeightController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WeightControllerTest.FakeUseCases.class)
class WeightControllerTest extends SignedInControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class FakeUseCases {

        @Bean
        RecordWeightUseCase recordWeight() {
            return (userId, date, kilograms) -> {
                CALLERS.add(userId);
                return new WeightEntry(date, kilograms);
            };
        }

        @Bean
        ListWeightsUseCase listWeights() {
            return userId -> {
                CALLERS.add(userId);
                return List.of(new WeightEntry(DATE, new BigDecimal("82.40")),
                        new WeightEntry(DATE.plusDays(1), new BigDecimal("81.95")));
            };
        }

        @Bean
        DeleteWeightUseCase deleteWeight() {
            return (userId, date) -> {
                CALLERS.add(userId);
                if (!date.equals(DATE)) {
                    throw new WeightNotFoundException(date);
                }
            };
        }
    }

    @Test
    void everyRouteAsksTheUseCaseOnBehalfOfTheSignedInUser() throws Exception {
        mvc.perform(get("/api/weights"));
        mvc.perform(put("/api/weights/2026-11-19").contentType(MediaType.APPLICATION_JSON).content("{\"kilograms\": 82.4}"));
        mvc.perform(delete("/api/weights/2026-11-19"));

        assertThat(CALLERS).hasSize(3).containsOnly(SIGNED_IN);
    }

    @Test
    void recordingReturnsTheWeightOfTheDay() throws Exception {
        mvc.perform(put("/api/weights/2026-11-19").contentType(MediaType.APPLICATION_JSON).content("{\"kilograms\": 82.4}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"date\": \"2026-11-19\", \"kilograms\": 82.4}"));
    }

    @Test
    void recordingAnImpossibleWeightIsABadRequestProblem() throws Exception {
        mvc.perform(put("/api/weights/2026-11-19").contentType(MediaType.APPLICATION_JSON).content("{\"kilograms\": 8240}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Weight must be between 20 and 500 kilograms"));
    }

    @Test
    void recordingWithoutTheKilogramsNamesTheField() throws Exception {
        mvc.perform(put("/api/weights/2026-11-19").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("kilograms is required"));
    }

    @Test
    void recordingOnAnInvalidDateIsABadRequest() throws Exception {
        mvc.perform(put("/api/weights/2026-13-01").contentType(MediaType.APPLICATION_JSON).content("{\"kilograms\": 82}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsTheWeightsInTheOrderTheUseCaseGives() throws Exception {
        mvc.perform(get("/api/weights"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"date": "2026-11-19", "kilograms": 82.40},
                         {"date": "2026-11-20", "kilograms": 81.95}]
                        """, true));
    }

    @Test
    void deletingTheWeightOfADayAnswersNoContent() throws Exception {
        mvc.perform(delete("/api/weights/2026-11-19"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletingADayWithoutWeightIsANotFoundProblem() throws Exception {
        mvc.perform(delete("/api/weights/2026-11-20"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Weight not found for 2026-11-20"));
    }
}
