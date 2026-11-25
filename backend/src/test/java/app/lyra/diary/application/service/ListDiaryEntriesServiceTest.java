package app.lyra.diary.application.service;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ListDiaryEntriesServiceTest {

    private static final UserId USER = UserId.random();

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Food RICE = Food.custom("Arroz", HUNDRED_GRAMS,
            new Nutrients(new BigDecimal("128"), new BigDecimal("2"), new BigDecimal("28"), new BigDecimal("0.2"), null, null));

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();
    private final ListDiaryEntriesService service = new ListDiaryEntriesService(repository);

    @Test
    void listsTheEntriesOfEveryDateOfTheUserAndNothingOfTheOthers() {
        DiaryEntry today = DiaryEntry.log(RICE, DATE, "Almoço", HUNDRED_GRAMS);
        DiaryEntry yesterday = DiaryEntry.log(RICE, DATE.minusDays(1), "Jantar", HUNDRED_GRAMS);
        repository.save(USER, today);
        repository.save(USER, yesterday);
        repository.save(UserId.random(), DiaryEntry.log(RICE, DATE, "Almoço", HUNDRED_GRAMS));

        assertThat(service.listEntries(USER)).containsExactly(yesterday, today);
    }

    @Test
    void listsNothingWhenTheUserHasNoEntry() {
        assertThat(service.listEntries(USER)).isEmpty();
    }
}
