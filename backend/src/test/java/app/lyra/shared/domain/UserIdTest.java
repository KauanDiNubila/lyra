package app.lyra.shared.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdTest {

    @Test
    void keepsTheUuidAndPrintsItAsText() {
        UUID uuid = UUID.randomUUID();

        UserId id = new UserId(uuid);

        assertThat(id.value()).isEqualTo(uuid);
        assertThat(id).hasToString(uuid.toString());
    }

    @Test
    void readsAnIdFromItsText() {
        UserId id = UserId.random();

        assertThat(UserId.of(id.toString())).isEqualTo(id);
    }

    @Test
    void randomIdsAreDifferent() {
        assertThat(UserId.random()).isNotEqualTo(UserId.random());
    }

    @Test
    void requiresAValue() {
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("The user id is required");
    }

    @Test
    void rejectsTextThatIsNotAnId() {
        assertThatThrownBy(() -> UserId.of("not-an-id"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("The user id is not valid");
        assertThatThrownBy(() -> UserId.of(null)).isInstanceOf(InvalidInputException.class);
    }
}
