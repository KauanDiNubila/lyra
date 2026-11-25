package app.lyra.weight.adapter.out.persistence;

import app.lyra.Concurrently;
import app.lyra.TestUsers;
import app.lyra.TestcontainersConfiguration;
import app.lyra.shared.domain.UserId;
import app.lyra.weight.application.port.out.WeightRepository;
import app.lyra.weight.application.port.out.WeightRepositoryContract;
import app.lyra.weight.domain.WeightEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, WeightPersistenceAdapter.class})
class WeightPersistenceAdapterTest implements WeightRepositoryContract {

    @Autowired
    private WeightPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbc;

    private UserId owner;
    private UserId otherOwner;

    @BeforeEach
    void createTheUsers() {
        owner = TestUsers.insert(jdbc);
        otherOwner = TestUsers.insert(jdbc);
    }

    @AfterEach
    void removeWhatTheTestCreated() {
        TestUsers.deleteEverything(jdbc);
    }

    @Override
    public WeightRepository repository() {
        return adapter;
    }

    @Override
    public UserId owner() {
        return owner;
    }

    @Override
    public UserId otherOwner() {
        return otherOwner;
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void theDatabaseRefusesAWeightOutsideTheLimits() {
        assertThatThrownBy(() -> insertRow(owner, "500.01"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("weight_entry_kilograms_check");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void theDatabaseRefusesAWeightOfAnUnknownUser() {
        assertThatThrownBy(() -> insertRow(UserId.random(), "82"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("weight_entry_user_id_fkey");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void twoWritesOfTheSameDayAtTheSameTimeLeaveOneRowAndNeitherFails() throws Exception {
        for (int round = 0; round < 40; round++) {
            LocalDate date = DATE.plusDays(round);
            Concurrently.run(List.of(
                    () -> save(date, "80"),
                    () -> save(date, "81")));
        }

        assertThat(adapter.findAll(owner)).hasSize(40)
                .allSatisfy(found -> assertThat(found.kilograms()).isIn(new BigDecimal("80.00"), new BigDecimal("81.00")));
    }

    private int save(LocalDate date, String kilograms) {
        adapter.save(owner, new WeightEntry(date, new BigDecimal(kilograms)));
        return 0;
    }

    private void insertRow(UserId user, String kilograms) {
        jdbc.sql("insert into weight_entry (user_id, entry_date, kilograms) values (:user, :date, " + kilograms + ")")
                .param("user", user.value())
                .param("date", DATE)
                .update();
    }
}
