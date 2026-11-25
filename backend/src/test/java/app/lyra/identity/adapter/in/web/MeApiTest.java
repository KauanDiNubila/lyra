package app.lyra.identity.adapter.in.web;

import app.lyra.TestSessions;
import app.lyra.TestcontainersConfiguration;
import app.lyra.identity.application.port.in.SignInUseCase;
import app.lyra.identity.application.port.in.SignInUseCase.SignInCommand;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.shared.domain.UserId;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    void exportsTheAccountAsADownloadEvenWhenItHasNoData() throws Exception {
        User user = signIn.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "Ana@Example.com", "Ana Souza"));

        mvc.perform(get("/api/me/export").with(sessionOf(user.getId())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"lyra-export.json\""))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.exportedAt").isNotEmpty())
                .andExpect(jsonPath("$.account.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.account.email").value("ana@example.com"))
                .andExpect(jsonPath("$.account.name").value("Ana Souza"))
                .andExpect(jsonPath("$.customFoods", hasSize(0)))
                .andExpect(jsonPath("$.plans", hasSize(0)))
                .andExpect(jsonPath("$.diaryEntries", hasSize(0)))
                .andExpect(jsonPath("$.weights", hasSize(0)));
    }

    @Test
    void deletesTheAccountAndEndsTheSession() throws Exception {
        User user = signIn.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));
        MockHttpSession session = new MockHttpSession();

        mvc.perform(delete("/api/me").session(session).with(TestSessions.of(user.getId())))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
        assertThat(jdbc.sql("select count(*) from app_user").query(Integer.class).single()).isZero();
    }

    @Test
    void deletingTheAccountNeedsTheCsrfToken() throws Exception {
        User user = signIn.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));

        mvc.perform(delete("/api/me").with(sessionOf(user.getId())))
                .andExpect(status().isForbidden());

        assertThat(jdbc.sql("select count(*) from app_user").query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void answersUnauthorizedAndEndsTheSessionWhenTheUserIsGone() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get("/api/me").session(session).with(sessionOf(UserId.random())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));

        assertThat(session.isInvalid()).isTrue();
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

        Cookie token = mvc.perform(get("/api/me").with(sessionOf(user.getId()))).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/logout").with(sessionOf(user.getId())).cookie(token).header("X-XSRF-TOKEN", token.getValue()))
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
