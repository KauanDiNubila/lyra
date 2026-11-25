package app.lyra.weight.application.service;

import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import app.lyra.weight.domain.WeightEntry;
import app.lyra.weight.domain.WeightNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeightServicesTest {

    private static final UserId ANA = UserId.random();
    private static final UserId BRUNO = UserId.random();
    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);

    private final InMemoryWeightRepository repository = new InMemoryWeightRepository();
    private final RecordWeightService recordWeight = new RecordWeightService(repository);
    private final ListWeightsService listWeights = new ListWeightsService(repository);
    private final DeleteWeightService deleteWeight = new DeleteWeightService(repository);

    @Test
    void recordsTheWeightOfTheDayAndReturnsIt() {
        WeightEntry recorded = recordWeight.recordWeight(ANA, DATE, new BigDecimal("82.4"));

        assertThat(recorded).isEqualTo(new WeightEntry(DATE, new BigDecimal("82.4")));
        assertThat(repository.findAll(ANA)).containsExactly(recorded);
    }

    @Test
    void recordingTheSameDayAgainReplacesTheWeight() {
        recordWeight.recordWeight(ANA, DATE, new BigDecimal("82.4"));

        recordWeight.recordWeight(ANA, DATE, new BigDecimal("81.7"));

        assertThat(listWeights.listWeights(ANA)).containsExactly(new WeightEntry(DATE, new BigDecimal("81.7")));
    }

    @Test
    void storesNothingWhenTheWeightIsInvalid() {
        assertThatThrownBy(() -> recordWeight.recordWeight(ANA, DATE, new BigDecimal("8240")))
                .isInstanceOf(InvalidInputException.class);

        assertThat(repository.findAll(ANA)).isEmpty();
    }

    @Test
    void listsOnlyTheWeightsOfWhoAsks() {
        recordWeight.recordWeight(ANA, DATE, new BigDecimal("82"));
        recordWeight.recordWeight(BRUNO, DATE, new BigDecimal("70"));

        assertThat(listWeights.listWeights(ANA)).containsExactly(new WeightEntry(DATE, new BigDecimal("82")));
    }

    @Test
    void deletesTheWeightOfTheDay() {
        recordWeight.recordWeight(ANA, DATE, new BigDecimal("82"));

        deleteWeight.delete(ANA, DATE);

        assertThat(repository.findAll(ANA)).isEmpty();
    }

    @Test
    void failsToDeleteADayWithoutWeight() {
        recordWeight.recordWeight(BRUNO, DATE, new BigDecimal("70"));

        assertThatThrownBy(() -> deleteWeight.delete(ANA, DATE))
                .isInstanceOf(WeightNotFoundException.class)
                .hasMessage("Weight not found for 2026-11-19");
        assertThat(repository.findAll(BRUNO)).hasSize(1);
    }
}
