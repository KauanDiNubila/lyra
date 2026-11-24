package app.lyra.identity.adapter.in.web;

import app.lyra.TestcontainersConfiguration;
import app.lyra.identity.application.port.in.SignInUseCase;
import app.lyra.identity.application.port.in.SignInUseCase.SignInCommand;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MeApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SignInUseCase signIn;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void removeWhatTheTestCreated() {
        jdbc.sql("delete from app_user").update();
    }

    @Test
    void showsWhoIsSignedIn() throws Exception {
        User user = signIn.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "Ana@Example.com", "Ana Souza"));

        mvc.perform(get("/api/me").with(sessionOf(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.name").value("Ana Souza"));
    }

    @Test
    void answersNotFoundWhenTheSessionBelongsToAUserThatIsGone() throws Exception {
        mvc.perform(get("/api/me").with(sessionOf(UserId.random())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not found"));
    }

    @Test
    void answersUnauthorizedWithoutASession() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void handsTheCsrfTokenToTheBrowserInACookieItCanRead() throws Exception {
        User user = signIn.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));

        mvc.perform(get("/api/me").with(sessionOf(user.getId())))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void signsOutWithTheCsrfToken() throws Exception {
        User user = signIn.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));

        mvc.perform(post("/api/logout").with(sessionOf(user.getId())).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/logout").with(sessionOf(user.getId())))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor sessionOf(UserId id) {
        Instant now = Instant.now();
        OidcIdToken idToken = OidcIdToken.withTokenValue("id-token").subject("sub-1")
                .issuedAt(now).expiresAt(now.plusSeconds(60)).build();
        return oidcLogin().oidcUser(new SignedInOidcUser(id, idToken));
    }
}
