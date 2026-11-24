package app.lyra.food.application.port.out;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public interface FoodRepositoryContract {

    FoodRepository repository();

    @Test
    default void findsFoodsMatchingEveryTermIgnoringAccentsAndCase() {
        saveCustom("Quimera Feijão Preto", "Quimera Feijão Branco", "Quimera Arroz");

        assertThat(names(search("QUIMERA feijao"))).containsExactlyInAnyOrder("Quimera Feijão Preto", "Quimera Feijão Branco");
    }

    @Test
    default void returnsNothingWhenNoFoodMatches() {
        saveCustom("Quimera Arroz");

        assertThat(search("quimera feijao")).isEmpty();
    }

    @Test
    default void returnsAtMostTheRequestedNumberOfFoods() {
        IntStream.range(0, 5).forEach(i -> saveCustom("Quimera bolo " + i));

        assertThat(repository().search(FoodSearchQuery.of("quimera bolo"), 3)).hasSize(3);
    }

    @Test
    default void listsCustomFoodsBeforeTacoFoods() {
        repository().save(food("Quimera curto", FoodSource.TACO));
        repository().save(food("Quimera personalizado com nome bem mais longo", FoodSource.CUSTOM));

        assertThat(names(search("quimera"))).containsExactly("Quimera personalizado com nome bem mais longo", "Quimera curto");
    }

    @Test
    default void listsShorterNamesFirstAndLeavesTheOrderOfEqualLengthsOpen() {
        saveCustom("Quimera b", "Quimera ab cd", "Quimera a");

        List<String> names = names(search("quimera"));

        assertThat(names.subList(0, 2)).containsExactlyInAnyOrder("Quimera a", "Quimera b");
        assertThat(names.get(2)).isEqualTo("Quimera ab cd");
    }

    @Test
    default void savedFoodComesBackWithTheSameValuesIncludingUnknownNutrients() {
        Quantity onePortion = new Quantity(BigDecimal.ONE, MeasurementUnit.PORTION);
        Nutrients nutrients = new Nutrients(new BigDecimal("590.5"), new BigDecimal("13"), new BigDecimal("98.25"),
                new BigDecimal("16"), null, new BigDecimal("0.123456"));
        Food saved = Food.custom("Quimera milkshake de morango", onePortion, nutrients);

        repository().save(saved);
        Food found = search("quimera milkshake").getFirst();

        assertThat(found).isEqualTo(saved);
        assertThat(found.getName()).isEqualTo(saved.getName());
        assertThat(found.getSource()).isEqualTo(FoodSource.CUSTOM);
        assertThat(found.getReferenceAmount()).isEqualTo(onePortion);
        assertThat(found.getNutrients()).isEqualTo(nutrients);
    }

    @Test
    default void findsASavedFoodById() {
        Food saved = food("Quimera por id", FoodSource.CUSTOM);
        repository().save(saved);

        Food found = repository().findById(saved.getId()).orElseThrow();

        assertThat(found).isEqualTo(saved);
        assertThat(found.getName()).isEqualTo("Quimera por id");
        assertThat(found.getReferenceAmount()).isEqualTo(saved.getReferenceAmount());
        assertThat(found.getNutrients()).isEqualTo(saved.getNutrients());
    }

    @Test
    default void findsNothingForAnUnknownId() {
        assertThat(repository().findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    default void findsOnlyTheRequestedFoodsAndIgnoresUnknownIds() {
        Food first = food("Quimera lote um", FoodSource.CUSTOM);
        Food second = food("Quimera lote dois", FoodSource.CUSTOM);
        Food notRequested = food("Quimera lote tres", FoodSource.CUSTOM);
        repository().save(first);
        repository().save(second);
        repository().save(notRequested);

        List<Food> found = repository().findAllById(List.of(first.getId(), second.getId(), UUID.randomUUID()));

        assertThat(found).containsExactlyInAnyOrder(first, second);
        assertThat(names(found)).containsExactlyInAnyOrder("Quimera lote um", "Quimera lote dois");
    }

    @Test
    default void findsNothingWhenNoIdIsRequested() {
        repository().save(food("Quimera sem pedido", FoodSource.CUSTOM));

        assertThat(repository().findAllById(List.of())).isEmpty();
    }

    @Test
    default void percentSignInATermIsTakenLiterally() {
        saveCustom("Quimera 100% natural", "Quimera 100 de laranja");

        List<Food> found = repository().search(new FoodSearchQuery(List.of("quimera", "100%")), 20);

        assertThat(names(found)).containsExactly("Quimera 100% natural");
    }

    @Test
    default void underscoreInATermIsTakenLiterally() {
        saveCustom("Quimera_integral", "Quimera integral");

        List<Food> found = repository().search(new FoodSearchQuery(List.of("quimera_integral")), 20);

        assertThat(names(found)).containsExactly("Quimera_integral");
    }

    @Test
    default void backslashInATermIsTakenLiterally() {
        saveCustom("Quimera a\\b", "Quimera ab");

        List<Food> found = repository().search(new FoodSearchQuery(List.of("quimera", "a\\b")), 20);

        assertThat(names(found)).containsExactly("Quimera a\\b");
    }

    private List<Food> search(String query) {
        return repository().search(FoodSearchQuery.of(query), 20);
    }

    private void saveCustom(String... names) {
        for (String name : names) {
            repository().save(food(name, FoodSource.CUSTOM));
        }
    }

    private static Food food(String name, FoodSource source) {
        Quantity hundredGrams = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
        Nutrients nutrients = new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null);
        return new Food(UUID.randomUUID(), name, source, hundredGrams, nutrients);
    }

    private static List<String> names(List<Food> foods) {
        return foods.stream().map(Food::getName).toList();
    }
}
