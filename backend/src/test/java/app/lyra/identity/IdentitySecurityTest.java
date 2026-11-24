package app.lyra.identity;

import app.lyra.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
    void theOtherRoutesStayOpenUntilTheOwnerStep() throws Exception {
        mvc.perform(get("/api/plans")).andExpect(status().isOk());
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
        mvc.perform(post("/api/logout").with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void writesToTheOtherRoutesNeedNoCsrfTokenYet() throws Exception {
        mvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
