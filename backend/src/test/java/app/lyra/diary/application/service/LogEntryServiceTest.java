package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.LogEntryUseCase.LogEntryCommand;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.application.port.in.FindFoodUseCase;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogEntryServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Food RICE = Food.custom("Arroz", grams("100"),
            new Nutrients(new BigDecimal("128.2585"), new BigDecimal("2.5208"), new BigDecimal("28.0598"),
                    new BigDecimal("0.227"), null, null));

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();

    @Test
    void savesAnEntryWithACopyOfTheFood() {
        LogEntryService service = new LogEntryService((userId, id) -> RICE, repository);

        DiaryEntry entry = service.log(UserId.random(), new LogEntryCommand(DATE, "Almoço", RICE.getId(), grams("150")));

        assertThat(entry.getFoodId()).isEqualTo(RICE.getId());
        assertThat(entry.getFoodName()).isEqualTo("Arroz");
        assertThat(entry.getQuantity()).isEqualTo(grams("150"));
        assertThat(repository.findById(entry.getId())).contains(entry);
    }

    @Test
    void savesNothingWhenTheFoodDoesNotExist() {
        FindFoodUseCase unknownFood = (userId, id) -> {
            throw new FoodNotFoundException(id);
        };
        LogEntryService service = new LogEntryService(unknownFood, repository);
        LogEntryCommand command = new LogEntryCommand(DATE, "Almoço", RICE.getId(), grams("150"));

        assertThatThrownBy(() -> service.log(UserId.random(), command)).isInstanceOf(FoodNotFoundException.class);
        assertThat(repository.findByDate(DATE)).isEmpty();
    }

    @Test
    void savesNothingWhenTheUnitIsNotTheFoodUnit() {
        LogEntryService service = new LogEntryService((userId, id) -> RICE, repository);
        Quantity milliliters = new Quantity(new BigDecimal("150"), MeasurementUnit.MILLILITER);
        LogEntryCommand command = new LogEntryCommand(DATE, "Almoço", RICE.getId(), milliliters);

        assertThatThrownBy(() -> service.log(UserId.random(), command)).isInstanceOf(InvalidInputException.class);
        assertThat(repository.findByDate(DATE)).isEmpty();
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
