package app.lyra.food.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoodSearchQueryTest {

    @Test
    void splitsIntoNormalizedTerms() {
        assertThat(FoodSearchQuery.of("  Feijão   CARIOCA ").terms()).containsExactly("feijao", "carioca");
    }

    @Test
    void punctuationSeparatesTermsAndNeverReachesTheQuery() {
        assertThat(FoodSearchQuery.of("arroz,cozido%_").terms()).containsExactly("arroz", "cozido");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "a", " ,. ", "%"})
    void rejectsQueriesShorterThanTwoCharacters(String query) {
        assertThatThrownBy(() -> FoodSearchQuery.of(query)).isInstanceOf(InvalidFoodException.class);
    }

    @Test
    void termsCannotBeChangedAfterCreation() {
        FoodSearchQuery query = FoodSearchQuery.of("arroz");

        assertThatThrownBy(() -> query.terms().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }
}
