package app.lyra.identity.application.service;

import app.lyra.identity.application.port.in.DeleteAccountUseCase;
import app.lyra.identity.application.port.out.UserRepository;
import app.lyra.identity.domain.UserNotFoundException;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class DeleteAccountService implements DeleteAccountUseCase {

    private final UserRepository userRepository;

    @Override
    public void delete(UserId userId) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.deleteById(userId);
    }
}
