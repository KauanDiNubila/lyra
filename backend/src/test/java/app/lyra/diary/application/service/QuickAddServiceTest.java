package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.QuickAddUseCase.QuickAddCommand;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.application.port.in.CreateCustomFoodUseCase;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuickAddServiceTest {

    private static final UserId USER = UserId.random();
    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Nutrients BIG_MAC = new Nutrients(new BigDecimal("550"), new BigDecimal("25"),
            new BigDecimal("45"), new BigDecimal("30"), null, null);

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();
    private final List<Food> createdFoods = new ArrayList<>();
    private final List<UserId> creators = new ArrayList<>();
    private final CreateCustomFoodUseCase createCustomFood = (userId, name, referenceAmount, nutrients) -> {
        Food food = Food.custom(name, referenceAmount, nutrients);
        createdFoods.add(food);
        creators.add(userId);
        return food;
    };
    private final QuickAddService service = new QuickAddService(createCustomFood, repository);

    @Test
    void createsAOnePortionCustomFoodForTheUserAndLogsTheEntryWithIt() {
        DiaryEntry entry = service.quickAdd(USER, new QuickAddCommand(DATE, "Almoço", "Big Mac", new BigDecimal("2"), BIG_MAC));

        Food food = createdFoods.getFirst();
        assertThat(creators).containsExactly(USER);
        assertThat(food.getName()).isEqualTo("Big Mac");
        assertThat(food.getSource()).isEqualTo(FoodSource.CUSTOM);
        assertThat(food.getReferenceAmount().amount()).isEqualByComparingTo("1");
        assertThat(food.getReferenceAmount().unit()).isEqualTo(MeasurementUnit.PORTION);
        assertThat(food.getNutrients().kcal()).isEqualByComparingTo("550");
        assertThat(food.getNutrients().fiber()).isNull();
        assertThat(food.getNutrients().sodium()).isNull();
        assertThat(entry.getFoodId()).isEqualTo(food.getId());
        assertThat(entry.getDate()).isEqualTo(DATE);
        assertThat(entry.getMealName()).isEqualTo("Almoço");
        assertThat(entry.getQuantity().amount()).isEqualByComparingTo("2");
        assertThat(entry.getQuantity().unit()).isEqualTo(MeasurementUnit.PORTION);
        assertThat(entry.macros().kcal()).isEqualByComparingTo("1100");
        assertThat(repository.findById(USER, entry.getId())).contains(entry);
    }

    @Test
    void refusesAQuantityOfZeroPortions() {
        assertThatThrownBy(() -> service.quickAdd(USER, new QuickAddCommand(DATE, "Almoço", "Big Mac", BigDecimal.ZERO, BIG_MAC)))
                .isInstanceOf(InvalidInputException.class);
        assertThat(repository.findByDate(USER, DATE)).isEmpty();
    }

    @Test
    void refusesAFoodNameThatIsTooShort() {
        assertThatThrownBy(() -> service.quickAdd(USER, new QuickAddCommand(DATE, "Almoço", "B", BigDecimal.ONE, BIG_MAC)))
                .isInstanceOf(InvalidInputException.class);
        assertThat(repository.findByDate(USER, DATE)).isEmpty();
    }
}
