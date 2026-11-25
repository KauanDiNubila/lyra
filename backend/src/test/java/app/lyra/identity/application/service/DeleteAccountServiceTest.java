package app.lyra.identity.application.service;

import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.identity.domain.UserNotFoundException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeleteAccountServiceTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final DeleteAccountService service = new DeleteAccountService(repository);

    @Test
    void deletesTheUserAndKeepsTheOthers() {
        User ana = User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana");
        User bia = User.create(AuthProvider.GOOGLE, "sub-2", "bia@example.com", "Bia");
        repository.save(ana);
        repository.save(bia);

        service.delete(ana.getId());

        assertThat(repository.findById(ana.getId())).isEmpty();
        assertThat(repository.findById(bia.getId())).isPresent();
    }

    @Test
    void failsWhenThereIsNoUserWithThatId() {
        UserId unknown = UserId.random();

        assertThatThrownBy(() -> service.delete(unknown))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: " + unknown);
    }
}
