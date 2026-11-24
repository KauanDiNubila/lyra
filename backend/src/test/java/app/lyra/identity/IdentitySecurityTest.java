package app.lyra.identity;

import app.lyra.TestSessions;
import app.lyra.TestcontainersConfiguration;
import app.lyra.shared.domain.UserId;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void aRouteThatKnowsTheUserAnswersUnauthorizedWithoutASession() throws Exception {
        mvc.perform(get("/api/plans")).andExpect(status().isUnauthorized());
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
        mvc.perform(post("/api/logout")).andExpect(status().isForbidden());

        Cookie token = mvc.perform(get("/api/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/logout").cookie(token).header("X-XSRF-TOKEN", token.getValue()))
                .andExpect(status().isNoContent());
    }

    @Test
    void refusesALogoutWithATokenThatIsNotTheOneOfTheCookie() throws Exception {
        Cookie token = mvc.perform(get("/api/me")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mvc.perform(post("/api/logout").cookie(token).header("X-XSRF-TOKEN", "another-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void writesToTheOtherRoutesNeedNoCsrfTokenYet() throws Exception {
        mvc.perform(post("/api/plans").with(TestSessions.of(UserId.random())).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
