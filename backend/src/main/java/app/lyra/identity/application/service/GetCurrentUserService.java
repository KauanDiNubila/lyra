package app.lyra.identity.application.service;

import app.lyra.identity.application.port.in.GetCurrentUserUseCase;
import app.lyra.identity.application.port.out.UserRepository;
import app.lyra.identity.domain.User;
import app.lyra.identity.domain.UserNotFoundException;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class GetCurrentUserService implements GetCurrentUserUseCase {

    private final UserRepository userRepository;

    @Override
    public User getUser(UserId id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
