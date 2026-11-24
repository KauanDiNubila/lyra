package app.lyra.identity.adapter.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
class SecurityConfiguration {

    private static final String[] PROTECTED_PATHS = {"/api/me", "/api/logout"};

    private final GoogleOidcUserService googleOidcUserService;
    private final ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        List<RequestMatcher> protectedMatchers = Stream.of(PROTECTED_PATHS).<RequestMatcher>map(paths::matcher).toList();
        RequestMatcher protectedPaths = request -> protectedMatchers.stream().anyMatch(matcher -> matcher.matches(request));
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PROTECTED_PATHS).authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(login -> login
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(googleOidcUserService))
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/?login=failed"))
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .csrf(csrf -> csrf
                        .spa()
                        .ignoringRequestMatchers(request -> !protectedPaths.matches(request)))
                .exceptionHandling(handling -> handling.authenticationEntryPoint(authenticationEntryPoint));
        return http.build();
    }
}
