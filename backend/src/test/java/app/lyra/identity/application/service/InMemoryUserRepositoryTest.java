package app.lyra.identity.application.service;

import app.lyra.identity.application.port.out.UserRepository;
import app.lyra.identity.application.port.out.UserRepositoryContract;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryUserRepositoryTest implements UserRepositoryContract {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();

    @Override
    public UserRepository repository() {
        return repository;
    }

    @Test
    void refusesTwoUsersWithTheSameProviderAndSubject() {
        repository.save(User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));
        User second = User.create(AuthProvider.GOOGLE, "sub-1", "other@example.com", "Other");

        assertThatThrownBy(() -> repository.save(second)).isInstanceOf(IllegalStateException.class);
    }
}
