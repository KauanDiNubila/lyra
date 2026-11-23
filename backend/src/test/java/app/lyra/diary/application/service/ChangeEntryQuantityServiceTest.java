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

class ChangeEntryQuantityServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Food RICE = Food.custom("Arroz", grams("100"),
            new Nutrients(new BigDecimal("128.2585"), new BigDecimal("2.5208"), new BigDecimal("28.0598"),
                    new BigDecimal("0.227"), null, null));

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();
    private final ChangeEntryQuantityService service = new ChangeEntryQuantityService(repository);

    @Test
    void changesTheQuantityAndSavesIt() {
        DiaryEntry entry = DiaryEntry.log(RICE, DATE, "Almoço", grams("150"));
        repository.save(entry);

        DiaryEntry changed = service.changeQuantity(entry.getId(), grams("200"));

        assertThat(changed.macros().kcal()).isEqualByComparingTo("256.517");
        assertThat(repository.findById(entry.getId()).orElseThrow().getQuantity()).isEqualTo(grams("200"));
    }

    @Test
    void failsWhenThereIsNoEntryWithThatId() {
        UUID unknown = UUID.randomUUID();
        Quantity quantity = grams("200");

        assertThatThrownBy(() -> service.changeQuantity(unknown, quantity))
                .isInstanceOf(DiaryEntryNotFoundException.class)
                .hasMessage("Diary entry not found: " + unknown);
    }

    @Test
    void keepsTheEntryWhenTheNewQuantityIsInvalid() {
        DiaryEntry entry = DiaryEntry.log(RICE, DATE, "Almoço", grams("150"));
        repository.save(entry);
        UUID id = entry.getId();
        Quantity zero = grams("0");

        assertThatThrownBy(() -> service.changeQuantity(id, zero)).isInstanceOf(InvalidInputException.class);
        assertThat(repository.findById(id).orElseThrow().getQuantity()).isEqualTo(grams("150"));
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
