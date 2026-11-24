package app.lyra.identity.application.service;

import app.lyra.identity.application.port.in.SignInUseCase;
import app.lyra.identity.application.port.out.UserRepository;
import app.lyra.identity.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SignInService implements SignInUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User signIn(SignInCommand command) {
        User user = userRepository.findByProvider(command.provider(), command.providerSubject())
                .map(existing -> existing.withProfile(command.email(), command.name()))
                .orElseGet(() -> User.create(command.provider(), command.providerSubject(), command.email(), command.name()));
        userRepository.save(user);
        return user;
    }
}
