package app.lyra.diary.application.port.out;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.Macros;
import app.lyra.diary.domain.Origin;
import app.lyra.diary.domain.Planned;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public interface DiaryEntryRepositoryContract {

    LocalDate DATE = LocalDate.of(2026, 11, 19);

    DiaryEntryRepository repository();

    UUID savedFoodId();

    UserId owner();

    UserId otherOwner();

    @Test
    default void savesAnEntryAndFindsItById() {
        DiaryEntry entry = entry(DATE, "Almoço", "150", nutrients("1.561", "1.2007"));

        repository().save(owner(), entry);

        DiaryEntry found = repository().findById(owner(), entry.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(entry.getId());
        assertThat(found.getDate()).isEqualTo(DATE);
        assertThat(found.getMealName()).isEqualTo("Almoço");
        assertThat(found.getFoodId()).isEqualTo(entry.getFoodId());
        assertThat(found.getFoodName()).isEqualTo("Arroz");
        assertThat(found.getFoodReference().amount()).isEqualByComparingTo("100");
        assertThat(found.getFoodReference().unit()).isEqualTo(MeasurementUnit.GRAM);
        assertThat(found.getNutrients().kcal()).isEqualByComparingTo("128.2585");
        assertThat(found.getNutrients().protein()).isEqualByComparingTo("2.5208");
        assertThat(found.getNutrients().carbohydrate()).isEqualByComparingTo("28.0598");
        assertThat(found.getNutrients().fat()).isEqualByComparingTo("0.227");
        assertThat(found.getNutrients().fiber()).isEqualByComparingTo("1.561");
        assertThat(found.getNutrients().sodium()).isEqualByComparingTo("1.2007");
        assertThat(found.getQuantity().amount()).isEqualByComparingTo("150");
        assertThat(found.getQuantity().unit()).isEqualTo(MeasurementUnit.GRAM);
    }

    @Test
    default void keepsMissingFiberAndSodiumAsNull() {
        DiaryEntry entry = entry(DATE, "Almoço", "150", nutrients(null, null));

        repository().save(owner(), entry);

        DiaryEntry found = repository().findById(owner(), entry.getId()).orElseThrow();
        assertThat(found.getNutrients().fiber()).isNull();
        assertThat(found.getNutrients().sodium()).isNull();
    }

    @Test
    default void findsNothingForAnUnknownId() {
        assertThat(repository().findById(owner(), UUID.randomUUID())).isEmpty();
    }

    @Test
    default void listsOnlyTheEntriesOfTheDateInTheOrderTheyWereSaved() {
        DiaryEntry first = entry(DATE, "Almoço", "100", nutrients(null, null));
        DiaryEntry otherDay = entry(DATE.plusDays(1), "Almoço", "100", nutrients(null, null));
        DiaryEntry second = entry(DATE, "Jantar", "100", nutrients(null, null));
        DiaryEntry third = entry(DATE, "Almoço", "50", nutrients(null, null));
        repository().save(owner(), first);
        repository().save(owner(), otherDay);
        repository().save(owner(), second);
        repository().save(owner(), third);

        assertThat(repository().findByDate(owner(), DATE)).containsExactly(first, second, third);
    }

    @Test
    default void listsNothingForADateWithoutEntries() {
        assertThat(repository().findByDate(owner(), DATE)).isEmpty();
    }

    @Test
    default void savingAgainWithTheSameIdReplacesTheEntryAndKeepsItsPlace() {
        DiaryEntry first = entry(DATE, "Almoço", "100", nutrients(null, null));
        DiaryEntry second = entry(DATE, "Jantar", "100", nutrients(null, null));
        repository().save(owner(), first);
        repository().save(owner(), second);
        DiaryEntry changed = new DiaryEntry(first.getId(), first.getDate(), first.getMealName(), first.getFoodId(),
                first.getFoodName(), first.getFoodReference(), first.getNutrients(), grams("200"));

        repository().save(owner(), changed);

        assertThat(repository().findByDate(owner(), DATE)).containsExactly(first, second);
        assertThat(repository().findById(owner(), first.getId()).orElseThrow().getQuantity().amount()).isEqualByComparingTo("200");
    }

    @Test
    default void deletesAnEntry() {
        DiaryEntry entry = entry(DATE, "Almoço", "100", nutrients(null, null));
        repository().save(owner(), entry);

        repository().deleteById(owner(), entry.getId());

        assertThat(repository().findById(owner(), entry.getId())).isEmpty();
    }

    @Test
    default void deletingAnUnknownIdDoesNothing() {
        assertThatCode(() -> repository().deleteById(owner(), UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    default void aDayCanBeLockedMoreThanOnceInARow() {
        assertThatCode(() -> {
            repository().lockDay(owner(), DATE);
            repository().lockDay(owner(), DATE);
        }).doesNotThrowAnyException();
    }

    @Test
    default void anEntryLoggedByHandComesBackWithoutPlan() {
        DiaryEntry entry = entry(DATE, "Almoço", "150", nutrients(null, null));
        repository().save(owner(), entry);

        DiaryEntry found = repository().findById(owner(), entry.getId()).orElseThrow();

        assertThat(found.getPlanned()).isEmpty();
        assertThat(found.origin()).isEqualTo(Origin.EXTRA);
    }

    @Test
    default void savesAnEntryFromThePlanWithWhatWasPlanned() {
        UUID foodId = savedFoodId();
        DiaryEntry entry = planEntry(DATE, "Almoço", foodId, foodId, 2, "150");
        repository().save(owner(), entry);

        DiaryEntry found = repository().findById(owner(), entry.getId()).orElseThrow();

        Planned planned = found.getPlanned().orElseThrow();
        assertThat(planned.foodId()).isEqualTo(foodId);
        assertThat(planned.foodName()).isEqualTo("Arroz planejado");
        assertThat(planned.quantity().amount()).isEqualByComparingTo("150");
        assertThat(planned.quantity().unit()).isEqualTo(MeasurementUnit.GRAM);
        assertThat(planned.macros().kcal()).isEqualByComparingTo("192.38775");
        assertThat(planned.macros().protein()).isEqualByComparingTo("3.7812");
        assertThat(planned.macros().carbohydrate()).isEqualByComparingTo("42.0897");
        assertThat(planned.macros().fat()).isEqualByComparingTo("0.3405");
        assertThat(planned.slot()).isEqualTo(2);
        assertThat(found.origin()).isEqualTo(Origin.PLANNED);
    }

    @Test
    default void keepsASkippedEntryWithAQuantityOfZero() {
        UUID foodId = savedFoodId();
        DiaryEntry skipped = planEntry(DATE, "Almoço", foodId, foodId, 0, "0");
        repository().save(owner(), skipped);

        DiaryEntry found = repository().findById(owner(), skipped.getId()).orElseThrow();

        assertThat(found.getQuantity().amount()).isEqualByComparingTo("0");
        assertThat(found.origin()).isEqualTo(Origin.SKIPPED);
        assertThat(found.getPlanned().orElseThrow().quantity().amount()).isEqualByComparingTo("150");
    }

    @Test
    default void keepsTheFoodThatWasPlannedWhenAnotherWasEaten() {
        UUID planned = savedFoodId();
        UUID eaten = savedFoodId();
        DiaryEntry swapped = planEntry(DATE, "Almoço", eaten, planned, 0, "120");
        repository().save(owner(), swapped);

        DiaryEntry found = repository().findById(owner(), swapped.getId()).orElseThrow();

        assertThat(found.getFoodId()).isEqualTo(eaten);
        assertThat(found.getPlanned().orElseThrow().foodId()).isEqualTo(planned);
        assertThat(found.getPlanned().orElseThrow().foodName()).isEqualTo("Arroz planejado");
        assertThat(found.origin()).isEqualTo(Origin.SWAPPED);
    }

    @Test
    default void changingAnEntryOfThePlanKeepsWhatWasPlanned() {
        UUID foodId = savedFoodId();
        DiaryEntry entry = planEntry(DATE, "Almoço", foodId, foodId, 1, "150");
        repository().save(owner(), entry);

        repository().save(owner(), entry.withQuantity(grams("200")));

        DiaryEntry found = repository().findById(owner(), entry.getId()).orElseThrow();
        assertThat(found.getQuantity().amount()).isEqualByComparingTo("200");
        assertThat(found.getPlanned()).isEqualTo(entry.getPlanned());
        assertThat(found.origin()).isEqualTo(Origin.ADJUSTED);
    }

    @Test
    default void anotherUsersEntriesAreNeitherFoundNorListed() {
        DiaryEntry entry = entry(DATE, "Almoço", "100", nutrients(null, null));
        repository().save(owner(), entry);

        assertThat(repository().findById(otherOwner(), entry.getId())).isEmpty();
        assertThat(repository().findByDate(otherOwner(), DATE)).isEmpty();
    }

    @Test
    default void anotherUserCannotDeleteAnEntry() {
        DiaryEntry entry = entry(DATE, "Almoço", "100", nutrients(null, null));
        repository().save(owner(), entry);

        repository().deleteById(otherOwner(), entry.getId());

        assertThat(repository().findById(owner(), entry.getId())).isPresent();
    }

    default DiaryEntry planEntry(LocalDate date, String mealName, UUID foodId, UUID plannedFoodId, int slot, String grams) {
        Macros plannedMacros = new Macros(new BigDecimal("192.38775"), new BigDecimal("3.7812"),
                new BigDecimal("42.0897"), new BigDecimal("0.3405"));
        Planned planned = new Planned(plannedFoodId, "Arroz planejado", grams("150"), plannedMacros, slot);
        return new DiaryEntry(UUID.randomUUID(), date, mealName, foodId, "Arroz", grams("100"),
                nutrients(null, null), grams(grams), planned);
    }

    default DiaryEntry entry(LocalDate date, String mealName, String grams, Nutrients nutrients) {
        return new DiaryEntry(UUID.randomUUID(), date, mealName, savedFoodId(), "Arroz", grams("100"), nutrients, grams(grams));
    }

    default Nutrients nutrients(String fiber, String sodium) {
        return new Nutrients(new BigDecimal("128.2585"), new BigDecimal("2.5208"), new BigDecimal("28.0598"),
                new BigDecimal("0.227"), fiber == null ? null : new BigDecimal(fiber), sodium == null ? null : new BigDecimal(sodium));
    }

    private static Quantity grams(String amount) {
        return new Quantity(new BigDecimal(amount), MeasurementUnit.GRAM);
    }
}
