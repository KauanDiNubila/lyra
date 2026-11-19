package app.lyra;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(properties = "springdoc.default-produces-media-type=application/json")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiContractTest {

    private static final Path CONTRACT = Path.of("..", "docs", "api", "openapi.json");
    private static final Path GENERATED = Path.of("target", "openapi.json");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void generatedApiMatchesTheCommittedContract() throws Exception {
        String body = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        JsonNode generated = objectMapper.readTree(body);
        Files.writeString(GENERATED, objectMapper.writer(SerializationFeature.INDENT_OUTPUT).writeValueAsString(generated));

        assertThat(Files.exists(CONTRACT))
                .as("Contract missing: copy %s to %s", GENERATED, CONTRACT)
                .isTrue();
        assertThat(generated)
                .as("API changed: review the diff and copy %s to %s", GENERATED, CONTRACT)
                .isEqualTo(objectMapper.readTree(Files.readString(CONTRACT)));
    }
}
