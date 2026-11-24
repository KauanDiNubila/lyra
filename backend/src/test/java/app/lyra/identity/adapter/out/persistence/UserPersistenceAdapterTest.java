package app.lyra.identity.adapter.out.persistence;

import app.lyra.TestcontainersConfiguration;
import app.lyra.identity.application.port.out.UserRepository;
import app.lyra.identity.application.port.out.UserRepositoryContract;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, UserPersistenceAdapter.class})
class UserPersistenceAdapterTest implements UserRepositoryContract {

    @Autowired
    private UserPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbc;

    @Override
    public UserRepository repository() {
        return adapter;
    }

    @AfterEach
    void removeWhatTheTestCreated() {
        jdbc.sql("delete from app_user").update();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsTwoUsersWithTheSameProviderAndSubject() {
        adapter.save(User.create(AuthProvider.GOOGLE, "sub-1", "ana@example.com", "Ana"));
        User second = User.create(AuthProvider.GOOGLE, "sub-1", "other@example.com", "Other");

        assertThatThrownBy(() -> adapter.save(second))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("app_user_provider_subject_key");
    }
}
