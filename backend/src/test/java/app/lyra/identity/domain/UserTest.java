package app.lyra.identity.domain;

import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void aNewUserGetsAnIdAndKeepsWhatTheProviderSaid() {
        User user = User.create(AuthProvider.GOOGLE, "sub-1", "Ana@Example.com ", "  Ana Souza ");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(user.getProviderSubject()).isEqualTo("sub-1");
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(user.getName()).isEqualTo("Ana Souza");
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void usesTheEmailAsTheNameWhenTheProviderGivesNone() {
        assertThat(User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", null).getName()).isEqualTo("ana@example.com");
        assertThat(User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "  ").getName()).isEqualTo("ana@example.com");
    }

    @Test
    void twoUsersWithTheSameIdAreTheSameUser() {
        User user = User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana");

        assertThat(user.withProfile("other@example.com", "Other")).isEqualTo(user);
        assertThat(User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana")).isNotEqualTo(user);
    }

    @Test
    void withProfileChangesOnlyTheEmailAndTheName() {
        User user = User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana");

        User changed = user.withProfile("NEW@example.com", "Ana Maria");

        assertThat(changed.getEmail()).isEqualTo("new@example.com");
        assertThat(changed.getName()).isEqualTo("Ana Maria");
        assertThat(changed.getId()).isEqualTo(user.getId());
        assertThat(changed.getProviderSubject()).isEqualTo("sub-1");
        assertThat(changed.getCreatedAt()).isEqualTo(user.getCreatedAt());
        assertThat(user.getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void requiresTheSubjectAndTheEmail() {
        assertThatThrownBy(() -> User.create(AuthProvider.GOOGLE, " ", "ana@example.com", "Ana"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("The subject of the provider is required");
        assertThatThrownBy(() -> User.create(AuthProvider.GOOGLE, null, "ana@example.com", "Ana"))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> User.create(AuthProvider.GOOGLE, "sub-1", " ", "Ana"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("The e-mail is required");
        assertThatThrownBy(() -> User.create(AuthProvider.GOOGLE, "sub-1", null, "Ana"))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void requiresTheIdTheProviderAndTheCreationDate() {
        UserId id = UserId.random();
        Instant now = Instant.now();

        assertThatThrownBy(() -> new User(null, AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana", now))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new User(id, null, "sub-1", "ana@example.com", "Ana", now))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new User(id, AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana", null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("A user needs an id, a provider and a creation date");
    }
}
