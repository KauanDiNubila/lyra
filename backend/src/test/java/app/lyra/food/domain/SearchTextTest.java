package app.lyra.food.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchTextTest {

    @Test
    void removesAccentsLowercasesAndCollapsesSpaces() {
        assertThat(SearchText.normalize("  Pão de   QUEIJO, Açúcar ")).isEqualTo("pao de queijo, acucar");
    }
}
