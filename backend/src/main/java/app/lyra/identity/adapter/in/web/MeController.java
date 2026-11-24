package app.lyra.identity.adapter.in.web;

import app.lyra.identity.application.port.in.GetCurrentUserUseCase;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
class MeController {

    private final GetCurrentUserUseCase getCurrentUser;

    @GetMapping
    MeResponse me(UserId userId) {
        return MeResponse.from(getCurrentUser.getUser(userId));
    }
}
