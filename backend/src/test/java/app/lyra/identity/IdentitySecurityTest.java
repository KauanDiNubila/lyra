package app.lyra.identity;

import app.lyra.TestSessions;
import app.lyra.TestcontainersConfiguration;
import app.lyra.TestUsers;
import app.lyra.shared.domain.UserId;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IdentitySecurityTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void removeWhatTheTestCreated() {
        TestUsers.deleteEverything(jdbc);
    }

    @Test
    void asksForASessionWithAProblemDetailsAnswer() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").value("Sign in to use the API"));
    }

    @Test
    void doesNotSendTheBrowserToALoginPageEvenWhenItAsksForHtml() throws Exception {
        mvc.perform(get("/api/me").accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void sendsTheUserToGoogleToSignIn() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://accounts.google.com/")))
                .andExpect(header().string("Location", containsString("client_id=")))
                .andExpect(header().string("Location", containsString("scope=openid%20email%20profile")));
    }

    @Test
    void logoutNeedsTheCsrfToken() throws Exception {
        UserId id = UserId.random();

        mvc.perform(post("/api/logout").with(user(id.toString()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/logout").with(TestSessions.of(id))).andExpect(status().isNoContent());
    }

    @Test
    void refusesALogoutWithATokenThatIsNotTheOneOfTheCookie() throws Exception {
        mvc.perform(post("/api/logout").with(user(UserId.random().toString()))
                        .cookie(new Cookie("XSRF-TOKEN", "the-cookie-token")).header("X-XSRF-TOKEN", "another-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aWriteWithoutASessionAsksForOneEvenWithoutTheCsrfToken() throws Exception {
        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void writesToTheOtherRoutesNeedTheCsrfToken() throws Exception {
        mvc.perform(post("/api/plans").with(user(UserId.random().toString())).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/plans").with(TestSessions.of(TestUsers.insert(jdbc))).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("apiRoutes")
    void everyApiRouteAsksForASession(HttpMethod method, String path) throws Exception {
        mvc.perform(request(method, path).with(TestSessions.withCsrfToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    static Stream<Arguments> apiRoutes() {
        String id = UUID.randomUUID().toString();
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/me"),
                Arguments.of(HttpMethod.GET, "/api/me/export"),
                Arguments.of(HttpMethod.DELETE, "/api/me"),
                Arguments.of(HttpMethod.GET, "/api/foods?query=arroz"),
                Arguments.of(HttpMethod.POST, "/api/foods"),
                Arguments.of(HttpMethod.GET, "/api/plans"),
                Arguments.of(HttpMethod.POST, "/api/plans"),
                Arguments.of(HttpMethod.GET, "/api/plans/" + id),
                Arguments.of(HttpMethod.PUT, "/api/plans/" + id),
                Arguments.of(HttpMethod.POST, "/api/plans/" + id + "/activation"),
                Arguments.of(HttpMethod.GET, "/api/diary/2026-11-19"),
                Arguments.of(HttpMethod.POST, "/api/diary/2026-11-19/entries"),
                Arguments.of(HttpMethod.POST, "/api/diary/2026-11-19/quick-entries"),
                Arguments.of(HttpMethod.POST, "/api/diary/2026-11-19/plan"),
                Arguments.of(HttpMethod.PATCH, "/api/diary/entries/" + id),
                Arguments.of(HttpMethod.DELETE, "/api/diary/entries/" + id));
    }
}
