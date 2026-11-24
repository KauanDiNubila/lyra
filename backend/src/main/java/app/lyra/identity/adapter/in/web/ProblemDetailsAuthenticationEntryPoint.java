package app.lyra.identity.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver exceptionResolver;

    ProblemDetailsAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
        exceptionResolver.resolveException(request, response, null, exception);
    }
}
