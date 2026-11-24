package app.lyra.identity.application.service;

import app.lyra.identity.application.port.in.SignInUseCase.SignInCommand;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignInServiceTest {

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final SignInService service = new SignInService(repository);

    @Test
    void createsTheUserOnTheFirstSignIn() {
        User user = service.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));

        assertThat(user.getEmail()).isEqualTo("ana@example.com");
        assertThat(repository.findById(user.getId())).contains(user);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void findsTheSameUserOnTheNextSignIns() {
        User first = service.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));

        User second = service.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void keepsTheEmailAndTheNameUpToDate() {
        User first = service.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));

        User second = service.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana.souza@example.com", "Ana Souza"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(repository.findById(first.getId()).orElseThrow().getEmail()).isEqualTo("ana.souza@example.com");
        assertThat(repository.findById(first.getId()).orElseThrow().getName()).isEqualTo("Ana Souza");
    }

    @Test
    void differentSubjectsAreDifferentUsers() {
        User first = service.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));

        User second = service.signIn(new SignInCommand(AuthProvider.GOOGLE, "sub-2", "ana2@example.com", "Ana"));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void savesNothingWhenTheProfileIsInvalid() {
        SignInCommand command = new SignInCommand(AuthProvider.GOOGLE, "sub-1", " ", "Ana");

        assertThatThrownBy(() -> service.signIn(command)).isInstanceOf(InvalidInputException.class);
        assertThat(repository.count()).isZero();
    }
}
