package app.lyra.diary.application.service;

import app.lyra.diary.domain.DiaryDay;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.Macros;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GetDiaryDayServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Food RICE = Food.custom("Arroz", grams("100"),
            new Nutrients(new BigDecimal("128"), new BigDecimal("2"), new BigDecimal("28"), new BigDecimal("0.2"), null, null));

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();
    private final GetDiaryDayService service = new GetDiaryDayService(repository);

    @Test
    void returnsTheEntriesOfThatDateOnly() {
        DiaryEntry today = DiaryEntry.log(RICE, DATE, "Almoço", grams("100"));
        repository.save(today);
        repository.save(DiaryEntry.log(RICE, DATE.minusDays(1), "Almoço", grams("100")));

        DiaryDay day = service.getDay(DATE);

        assertThat(day.date()).isEqualTo(DATE);
        assertThat(day.entries()).containsExactly(today);
    }

    @Test
    void returnsAnEmptyDayWhenNothingWasEaten() {
        DiaryDay day = service.getDay(DATE);

        assertThat(day.entries()).isEmpty();
        assertThat(day.total()).isEqualTo(Macros.ZERO);
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
