package app.lyra.identity.application.service;

import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.identity.domain.UserNotFoundException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCurrentUserServiceTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final GetCurrentUserService service = new GetCurrentUserService(repository);

    @Test
    void returnsTheUserWithThatId() {
        User user = User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana");
        repository.save(user);
        repository.save(User.create(AuthProvider.GOOGLE, "sub-2", "bia@example.com", "Bia"));

        assertThat(service.getUser(user.getId())).isEqualTo(user);
    }

    @Test
    void failsWhenThereIsNoUserWithThatId() {
        UserId unknown = UserId.random();

        assertThatThrownBy(() -> service.getUser(unknown))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: " + unknown);
    }
}
