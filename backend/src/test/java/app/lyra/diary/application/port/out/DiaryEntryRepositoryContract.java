package app.lyra.diary.application.port.out;

import app.lyra.diary.domain.DiaryEntry;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
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

    @Test
    default void savesAnEntryAndFindsItById() {
        DiaryEntry entry = entry(DATE, "Almoço", "150", nutrients("1.561", "1.2007"));

        repository().save(entry);

        DiaryEntry found = repository().findById(entry.getId()).orElseThrow();
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

        repository().save(entry);

        DiaryEntry found = repository().findById(entry.getId()).orElseThrow();
        assertThat(found.getNutrients().fiber()).isNull();
        assertThat(found.getNutrients().sodium()).isNull();
    }

    @Test
    default void findsNothingForAnUnknownId() {
        assertThat(repository().findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    default void listsOnlyTheEntriesOfTheDateInTheOrderTheyWereSaved() {
        DiaryEntry first = entry(DATE, "Almoço", "100", nutrients(null, null));
        DiaryEntry otherDay = entry(DATE.plusDays(1), "Almoço", "100", nutrients(null, null));
        DiaryEntry second = entry(DATE, "Jantar", "100", nutrients(null, null));
        DiaryEntry third = entry(DATE, "Almoço", "50", nutrients(null, null));
        repository().save(first);
        repository().save(otherDay);
        repository().save(second);
        repository().save(third);

        assertThat(repository().findByDate(DATE)).containsExactly(first, second, third);
    }

    @Test
    default void listsNothingForADateWithoutEntries() {
        assertThat(repository().findByDate(DATE)).isEmpty();
    }

    @Test
    default void savingAgainWithTheSameIdReplacesTheEntryAndKeepsItsPlace() {
        DiaryEntry first = entry(DATE, "Almoço", "100", nutrients(null, null));
        DiaryEntry second = entry(DATE, "Jantar", "100", nutrients(null, null));
        repository().save(first);
        repository().save(second);
        DiaryEntry changed = new DiaryEntry(first.getId(), first.getDate(), first.getMealName(), first.getFoodId(),
                first.getFoodName(), first.getFoodReference(), first.getNutrients(), grams("200"));

        repository().save(changed);

        assertThat(repository().findByDate(DATE)).containsExactly(first, second);
        assertThat(repository().findById(first.getId()).orElseThrow().getQuantity().amount()).isEqualByComparingTo("200");
    }

    @Test
    default void deletesAnEntry() {
        DiaryEntry entry = entry(DATE, "Almoço", "100", nutrients(null, null));
        repository().save(entry);

        repository().deleteById(entry.getId());

        assertThat(repository().findById(entry.getId())).isEmpty();
    }

    @Test
    default void deletingAnUnknownIdDoesNothing() {
        assertThatCode(() -> repository().deleteById(UUID.randomUUID())).doesNotThrowAnyException();
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
