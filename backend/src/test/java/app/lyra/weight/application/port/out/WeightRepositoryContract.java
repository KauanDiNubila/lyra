package app.lyra.weight.application.port.out;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.domain.WeightEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

public interface WeightRepositoryContract {

    LocalDate DATE = LocalDate.of(2026, 11, 19);

    WeightRepository repository();

    UserId owner();

    UserId otherOwner();

    static WeightEntry weight(LocalDate date, String kilograms) {
        return new WeightEntry(date, new BigDecimal(kilograms));
    }

    @Test
    default void savesAWeightAndListsIt() {
        repository().save(owner(), weight(DATE, "82.45"));

        assertThat(repository().findAll(owner())).singleElement().satisfies(found -> {
            assertThat(found.date()).isEqualTo(DATE);
            assertThat(found.kilograms()).isEqualByComparingTo("82.45");
        });
    }

    @Test
    default void replacesTheWeightOfADateThatAlreadyHasOne() {
        repository().save(owner(), weight(DATE, "82.45"));

        repository().save(owner(), weight(DATE, "81.9"));

        assertThat(repository().findAll(owner())).singleElement()
                .satisfies(found -> assertThat(found.kilograms()).isEqualByComparingTo("81.9"));
    }

    @Test
    default void listsTheWeightsFromTheOldestDate() {
        repository().save(owner(), weight(DATE.plusDays(2), "81"));
        repository().save(owner(), weight(DATE, "83"));
        repository().save(owner(), weight(DATE.plusDays(1), "82"));

        assertThat(repository().findAll(owner()))
                .extracting(WeightEntry::date)
                .containsExactly(DATE, DATE.plusDays(1), DATE.plusDays(2));
    }

    @Test
    default void keepsTheWeightsOfEachOwnerApart() {
        repository().save(owner(), weight(DATE, "82"));
        repository().save(otherOwner(), weight(DATE, "60"));

        assertThat(repository().findAll(owner())).singleElement()
                .satisfies(found -> assertThat(found.kilograms()).isEqualByComparingTo("82"));
        assertThat(repository().findAll(otherOwner())).singleElement()
                .satisfies(found -> assertThat(found.kilograms()).isEqualByComparingTo("60"));
    }

    @Test
    default void listsNothingForAnOwnerWithoutWeights() {
        assertThat(repository().findAll(owner())).isEmpty();
    }

    @Test
    default void deletesTheWeightOfADateAndSaysItDid() {
        repository().save(owner(), weight(DATE, "82"));
        repository().save(owner(), weight(DATE.plusDays(1), "81"));

        assertThat(repository().delete(owner(), DATE)).isTrue();

        assertThat(repository().findAll(owner())).extracting(WeightEntry::date).containsExactly(DATE.plusDays(1));
    }

    @Test
    default void saysNothingWasDeletedForADateWithoutWeight() {
        assertThat(repository().delete(owner(), DATE)).isFalse();
    }

    @Test
    default void doesNotDeleteTheWeightOfAnotherOwner() {
        repository().save(owner(), weight(DATE, "82"));

        assertThat(repository().delete(otherOwner(), DATE)).isFalse();

        assertThat(repository().findAll(owner())).hasSize(1);
    }
}
