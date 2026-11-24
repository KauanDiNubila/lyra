package app.lyra.diary.application.service;

import app.lyra.diary.application.port.in.ChangeEntryUseCase.ChangeEntryCommand;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.DiaryEntryNotFoundException;
import app.lyra.diary.domain.Origin;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeEntryServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);
    private static final Food RICE = Food.custom("Arroz", grams("100"),
            new Nutrients(new BigDecimal("128.2585"), new BigDecimal("2.5208"), new BigDecimal("28.0598"),
                    new BigDecimal("0.227"), null, null));
    private static final Food CHICKEN = Food.custom("Frango", grams("100"),
            new Nutrients(new BigDecimal("159"), new BigDecimal("32"), BigDecimal.ZERO, new BigDecimal("2.5"), null, null));
    private static final Food MILKSHAKE = Food.custom("Milkshake", new Quantity(BigDecimal.ONE, MeasurementUnit.PORTION),
            new Nutrients(new BigDecimal("590"), new BigDecimal("13"), new BigDecimal("98"), new BigDecimal("16"), null, null));

    private final InMemoryDiaryEntryRepository repository = new InMemoryDiaryEntryRepository();
    private final ChangeEntryService service = new ChangeEntryService(foods(), repository);

    @Test
    void changesTheQuantityAndSavesIt() {
        DiaryEntry entry = saved(DiaryEntry.log(RICE, DATE, "Almoço", grams("150")));

        DiaryEntry changed = service.change(UserId.random(), entry.getId(), new ChangeEntryCommand(null, grams("200"), null));

        assertThat(changed.macros().kcal()).isEqualByComparingTo("256.517");
        assertThat(changed.getFoodId()).isEqualTo(RICE.getId());
        assertThat(changed.getMealName()).isEqualTo("Almoço");
        assertThat(repository.findById(entry.getId())).contains(changed);
        assertThat(repository.findById(entry.getId()).orElseThrow().getQuantity()).isEqualTo(grams("200"));
    }

    @Test
    void changesOnlyTheMeal() {
        DiaryEntry entry = saved(DiaryEntry.log(RICE, DATE, "Almoço", grams("150")));

        DiaryEntry changed = service.change(UserId.random(), entry.getId(), new ChangeEntryCommand(null, null, "  Pós-treino "));

        assertThat(changed.getMealName()).isEqualTo("Pós-treino");
        assertThat(changed.getQuantity()).isEqualTo(grams("150"));
        assertThat(repository.findById(entry.getId()).orElseThrow().getMealName()).isEqualTo("Pós-treino");
    }

    @Test
    void swapsTheFoodWithANewSnapshotAndTheNewQuantity() {
        DiaryEntry entry = saved(DiaryEntry.fromPlan(RICE, DATE, "Almoço", grams("150"), 0));

        DiaryEntry changed = service.change(UserId.random(), entry.getId(), new ChangeEntryCommand(CHICKEN.getId(), grams("120"), null));

        assertThat(changed.getFoodId()).isEqualTo(CHICKEN.getId());
        assertThat(changed.getFoodName()).isEqualTo("Frango");
        assertThat(changed.getNutrients()).isEqualTo(CHICKEN.getNutrients());
        assertThat(changed.getQuantity()).isEqualTo(grams("120"));
        assertThat(changed.origin()).isEqualTo(Origin.SWAPPED);
        assertThat(changed.getPlanned()).isEqualTo(entry.getPlanned());
        assertThat(repository.findById(entry.getId())).contains(changed);
    }

    @Test
    void swapsTheFoodToOneInAnotherUnit() {
        DiaryEntry entry = saved(DiaryEntry.log(RICE, DATE, "Lanche", grams("150")));
        Quantity onePortion = new Quantity(BigDecimal.ONE, MeasurementUnit.PORTION);

        DiaryEntry changed = service.change(UserId.random(), entry.getId(), new ChangeEntryCommand(MILKSHAKE.getId(), onePortion, null));

        assertThat(changed.getQuantity()).isEqualTo(onePortion);
        assertThat(changed.getFoodReference().unit()).isEqualTo(MeasurementUnit.PORTION);
    }

    @Test
    void changesTheFoodTheQuantityAndTheMealTogether() {
        DiaryEntry entry = saved(DiaryEntry.log(RICE, DATE, "Almoço", grams("150")));

        DiaryEntry changed = service.change(UserId.random(), entry.getId(), new ChangeEntryCommand(CHICKEN.getId(), grams("80"), "Jantar"));

        assertThat(changed.getFoodName()).isEqualTo("Frango");
        assertThat(changed.getQuantity()).isEqualTo(grams("80"));
        assertThat(changed.getMealName()).isEqualTo("Jantar");
    }

    @Test
    void skipsAnEntryOfThePlanWithAQuantityOfZero() {
        DiaryEntry entry = saved(DiaryEntry.fromPlan(RICE, DATE, "Almoço", grams("150"), 0));

        DiaryEntry skipped = service.change(UserId.random(), entry.getId(), new ChangeEntryCommand(null, grams("0"), null));

        assertThat(skipped.origin()).isEqualTo(Origin.SKIPPED);
        assertThat(repository.findById(entry.getId()).orElseThrow().origin()).isEqualTo(Origin.SKIPPED);
    }

    @Test
    void failsWhenThereIsNoEntryWithThatId() {
        UUID unknown = UUID.randomUUID();
        ChangeEntryCommand command = new ChangeEntryCommand(null, grams("200"), null);

        assertThatThrownBy(() -> service.change(UserId.random(), unknown, command))
                .isInstanceOf(DiaryEntryNotFoundException.class)
                .hasMessage("Diary entry not found: " + unknown);
    }

    @Test
    void keepsTheEntryWhenTheFoodDoesNotExist() {
        DiaryEntry entry = saved(DiaryEntry.log(RICE, DATE, "Almoço", grams("150")));
        UUID id = entry.getId();
        ChangeEntryCommand command = new ChangeEntryCommand(UUID.randomUUID(), grams("100"), "Jantar");

        assertThatThrownBy(() -> service.change(UserId.random(), id, command)).isInstanceOf(FoodNotFoundException.class);
        assertThat(repository.findById(id)).contains(entry);
        assertThat(repository.findById(id).orElseThrow().getMealName()).isEqualTo("Almoço");
    }

    @Test
    void keepsTheEntryWhenOneOfTheChangesIsInvalid() {
        DiaryEntry entry = saved(DiaryEntry.log(RICE, DATE, "Almoço", grams("150")));
        UUID id = entry.getId();

        assertThatThrownBy(() -> service.change(UserId.random(), id, new ChangeEntryCommand(null, grams("0"), "Jantar")))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.change(UserId.random(), id, new ChangeEntryCommand(null, grams("100"), "  ")))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.change(UserId.random(), id, new ChangeEntryCommand(MILKSHAKE.getId(), grams("1"), null)))
                .isInstanceOf(InvalidInputException.class);

        DiaryEntry kept = repository.findById(id).orElseThrow();
        assertThat(kept.getQuantity()).isEqualTo(grams("150"));
        assertThat(kept.getMealName()).isEqualTo("Almoço");
        assertThat(kept.getFoodId()).isEqualTo(RICE.getId());
    }

    @Test
    void refusesACommandWithNothingToChange() {
        assertThatThrownBy(() -> new ChangeEntryCommand(null, null, null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Nothing to change: send a quantity, a food or a meal");
    }

    @Test
    void refusesToChangeTheFoodWithoutTheQuantity() {
        UUID foodId = CHICKEN.getId();

        assertThatThrownBy(() -> new ChangeEntryCommand(foodId, null, "Jantar"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Changing the food needs the quantity");
    }

    private DiaryEntry saved(DiaryEntry entry) {
        repository.save(entry);
        return entry;
    }

    private static FindFoodUseCase foods() {
        List<Food> known = List.of(RICE, CHICKEN, MILKSHAKE);
        return (userId, id) -> known.stream().filter(food -> food.getId().equals(id)).findFirst()
                .orElseThrow(() -> new FoodNotFoundException(id));
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
