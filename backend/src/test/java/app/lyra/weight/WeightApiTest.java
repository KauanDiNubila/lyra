package app.lyra.weight;

import app.lyra.TestSessions;
import app.lyra.TestUsers;
import app.lyra.TestcontainersConfiguration;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WeightApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    private UserId user;

    @BeforeEach
    void signInAUser() {
        user = TestUsers.insert(jdbc);
    }

    @AfterEach
    void removeWhatTheTestCreated() {
        TestUsers.deleteEverything(jdbc);
    }

    @Test
    void recordsCorrectsListsAndDeletesTheWeightOfEachDay() throws Exception {
        record("2026-11-20", "81.6").andExpect(status().isOk());
        record("2026-11-19", "82.45")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-19"))
                .andExpect(jsonPath("$.kilograms").value(82.45));
        record("2026-11-19", "82.1").andExpect(status().isOk());

        perform(get("/api/weights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2026-11-19"))
                .andExpect(jsonPath("$[0].kilograms").value(82.1))
                .andExpect(jsonPath("$[1].date").value("2026-11-20"))
                .andExpect(jsonPath("$[1].kilograms").value(81.6));

        perform(delete("/api/weights/2026-11-19")).andExpect(status().isNoContent());
        perform(delete("/api/weights/2026-11-19")).andExpect(status().isNotFound());
        perform(get("/api/weights")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void anImpossibleWeightIsRefusedAndNothingIsStored() throws Exception {
        record("2026-11-19", "8240").andExpect(status().isBadRequest());
        record("2026-11-19", "82.455").andExpect(status().isBadRequest());

        perform(get("/api/weights")).andExpect(jsonPath("$", hasSize(0)));
    }

    private ResultActions record(String date, String kilograms) throws Exception {
        return perform(put("/api/weights/" + date).contentType(MediaType.APPLICATION_JSON)
                .content("{\"kilograms\": " + kilograms + "}"));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(TestSessions.of(user)));
    }
}
