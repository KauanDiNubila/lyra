package app.lyra.diary.application.service;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.DiaryEntryNotFoundException;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeleteEntryServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Food RICE = Food.custom("Arroz", HUNDRED_GRAMS,
            new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null));

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();
    private final DeleteEntryService service = new DeleteEntryService(repository);

    @Test
    void deletesTheEntry() {
        DiaryEntry entry = DiaryEntry.log(RICE, DATE, "Almoço", HUNDRED_GRAMS);
        DiaryEntry other = DiaryEntry.log(RICE, DATE, "Jantar", HUNDRED_GRAMS);
        repository.save(entry);
        repository.save(other);

        service.delete(entry.getId());

        assertThat(repository.findByDate(DATE)).containsExactly(other);
    }

    @Test
    void refusesToDeleteAnEntryOfThePlanAndKeepsIt() {
        DiaryEntry planned = DiaryEntry.fromPlan(RICE, DATE, "Almoço", HUNDRED_GRAMS, 0);
        repository.save(planned);
        UUID id = planned.getId();

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("An entry of the plan cannot be deleted: change its quantity to zero to skip it");
        assertThat(repository.findByDate(DATE)).containsExactly(planned);
    }

    @Test
    void failsWhenThereIsNoEntryWithThatId() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.delete(unknown))
                .isInstanceOf(DiaryEntryNotFoundException.class)
                .hasMessage("Diary entry not found: " + unknown);
    }
}
