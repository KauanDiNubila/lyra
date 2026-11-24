package app.lyra.shared.adapter.in.web;

import app.lyra.shared.domain.UserId;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == UserId.class;
    }

    @Override
    public UserId resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request,
                                  WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No user is signed in");
        }
        return UserId.of(authentication.getName());
    }
}
