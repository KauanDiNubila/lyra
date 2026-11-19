package app.lyra.diary.adapter.out.persistence;

import app.lyra.TestcontainersConfiguration;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.application.port.out.DiaryEntryRepositoryContract;
import app.lyra.diary.domain.DiaryEntry;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, DiaryEntryPersistenceAdapter.class})
class DiaryEntryPersistenceAdapterTest implements DiaryEntryRepositoryContract {

    @Autowired
    private DiaryEntryPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbc;

    @Override
    public DiaryEntryRepository repository() {
        return adapter;
    }

    @Override
    public UUID savedFoodId() {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                        insert into food (id, name, search_name, source, reference_amount, reference_unit, kcal, protein, carbohydrate, fat)
                        values (:id, 'Arroz', 'arroz', 'CUSTOM', 100, 'GRAM', 1, 1, 1, 1)
                        """)
                .param("id", id)
                .update();
        return id;
    }

    @AfterEach
    void removeWhatTheTestCreated() {
        jdbc.sql("delete from diary_entry").update();
        jdbc.sql("delete from food where source_reference is null").update();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsAnEntryOfAFoodThatDoesNotExist() {
        DiaryEntry entry = entry(DATE, "Almoço", "100", nutrients(null, null));
        DiaryEntry orphan = new DiaryEntry(entry.getId(), DATE, "Almoço", UUID.randomUUID(), "Arroz",
                entry.getFoodReference(), entry.getNutrients(), entry.getQuantity());

        assertThatThrownBy(() -> adapter.save(orphan)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
