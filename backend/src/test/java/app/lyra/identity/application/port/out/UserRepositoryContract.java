package app.lyra.identity.application.port.out;

import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public interface UserRepositoryContract {

    UserRepository repository();

    @Test
    default void savesAUserAndFindsItById() {
        User user = User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana");

        repository().save(user);

        User found = repository().findById(user.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(user.getId());
        assertThat(found.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(found.getProviderSubject()).isEqualTo("sub-1");
        assertThat(found.getEmail()).isEqualTo("ana@example.com");
        assertThat(found.getName()).isEqualTo("Ana");
        assertThat(found.getCreatedAt()).isCloseTo(user.getCreatedAt(), within(1, ChronoUnit.MICROS));
    }

    @Test
    default void findsAUserByTheProviderAndTheSubject() {
        User user = User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana");
        repository().save(user);
        repository().save(User.create(AuthProvider.GOOGLE, "sub-2", "bia@example.com", "Bia"));

        assertThat(repository().findByProvider(AuthProvider.GOOGLE, "sub-1").orElseThrow().getId()).isEqualTo(user.getId());
    }

    @Test
    default void findsNothingForAnUnknownIdOrSubject() {
        assertThat(repository().findById(UserId.random())).isEmpty();
        assertThat(repository().findByProvider(AuthProvider.GOOGLE, "nobody")).isEmpty();
    }

    @Test
    default void savingAgainWithTheSameIdChangesTheProfile() {
        User user = User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana");
        repository().save(user);

        repository().save(user.withProfile("ana.souza@example.com", "Ana Souza"));

        User found = repository().findById(user.getId()).orElseThrow();
        assertThat(found.getEmail()).isEqualTo("ana.souza@example.com");
        assertThat(found.getName()).isEqualTo("Ana Souza");
        assertThat(repository().findByProvider(AuthProvider.GOOGLE, "sub-1")).isPresent();
    }
}
