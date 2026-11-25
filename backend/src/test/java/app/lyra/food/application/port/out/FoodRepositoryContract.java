package app.lyra.food.application.port.out;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public interface FoodRepositoryContract {

    FoodRepository repository();

    UserId owner();

    UserId otherOwner();

    void seedTaco(Food food);

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

        assertThat(repository().search(owner(), FoodSearchQuery.of("quimera bolo"), 3)).hasSize(3);
    }

    @Test
    default void listsCustomFoodsBeforeTacoFoods() {
        seedTaco(food("Quimera curto", FoodSource.TACO));
        repository().save(owner(), food("Quimera personalizado com nome bem mais longo", FoodSource.CUSTOM));

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

        repository().save(owner(), saved);
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
        repository().save(owner(), saved);

        Food found = repository().findById(owner(), saved.getId()).orElseThrow();

        assertThat(found).isEqualTo(saved);
        assertThat(found.getName()).isEqualTo("Quimera por id");
        assertThat(found.getReferenceAmount()).isEqualTo(saved.getReferenceAmount());
        assertThat(found.getNutrients()).isEqualTo(saved.getNutrients());
    }

    @Test
    default void findsNothingForAnUnknownId() {
        assertThat(repository().findById(owner(), UUID.randomUUID())).isEmpty();
    }

    @Test
    default void findsOnlyTheRequestedFoodsAndIgnoresUnknownIds() {
        Food first = food("Quimera lote um", FoodSource.CUSTOM);
        Food second = food("Quimera lote dois", FoodSource.CUSTOM);
        Food notRequested = food("Quimera lote tres", FoodSource.CUSTOM);
        repository().save(owner(), first);
        repository().save(owner(), second);
        repository().save(owner(), notRequested);

        List<Food> found = repository().findAllById(owner(), List.of(first.getId(), second.getId(), UUID.randomUUID()));

        assertThat(found).containsExactlyInAnyOrder(first, second);
        assertThat(names(found)).containsExactlyInAnyOrder("Quimera lote um", "Quimera lote dois");
    }

    @Test
    default void findsNothingWhenNoIdIsRequested() {
        repository().save(owner(), food("Quimera sem pedido", FoodSource.CUSTOM));

        assertThat(repository().findAllById(owner(), List.of())).isEmpty();
    }

    @Test
    default void percentSignInATermIsTakenLiterally() {
        saveCustom("Quimera 100% natural", "Quimera 100 de laranja");

        List<Food> found = repository().search(owner(), new FoodSearchQuery(List.of("quimera", "100%")), 20);

        assertThat(names(found)).containsExactly("Quimera 100% natural");
    }

    @Test
    default void underscoreInATermIsTakenLiterally() {
        saveCustom("Quimera_integral", "Quimera integral");

        List<Food> found = repository().search(owner(), new FoodSearchQuery(List.of("quimera_integral")), 20);

        assertThat(names(found)).containsExactly("Quimera_integral");
    }

    @Test
    default void backslashInATermIsTakenLiterally() {
        saveCustom("Quimera a\\b", "Quimera ab");

        List<Food> found = repository().search(owner(), new FoodSearchQuery(List.of("quimera", "a\\b")), 20);

        assertThat(names(found)).containsExactly("Quimera a\\b");
    }

    @Test
    default void anotherUsersCustomFoodIsInvisible() {
        Food mine = food("Quimera privado", FoodSource.CUSTOM);
        repository().save(owner(), mine);

        assertThat(repository().findById(otherOwner(), mine.getId())).isEmpty();
        assertThat(repository().findAllById(otherOwner(), List.of(mine.getId()))).isEmpty();
        assertThat(repository().search(otherOwner(), FoodSearchQuery.of("quimera privado"), 20)).isEmpty();
        assertThat(repository().findById(owner(), mine.getId())).isPresent();
    }


    @Test
    default void refusesToSaveAFoodThatIsNotCustom() {
        Food taco = food("Quimera taco", FoodSource.TACO);

        assertThatThrownBy(() -> repository().save(owner(), taco)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void listsOnlyTheCustomFoodsOfTheUserByName() {
        repository().save(owner(), food("Quimera b", FoodSource.CUSTOM));
        repository().save(owner(), food("Quimera a", FoodSource.CUSTOM));
        repository().save(otherOwner(), food("Quimera dele", FoodSource.CUSTOM));
        seedTaco(food("Quimera taco", FoodSource.TACO));

        assertThat(names(repository().findCustom(owner()))).containsExactly("Quimera a", "Quimera b");
    }
    @Test
    default void tacoFoodsAreVisibleToEveryUser() {
        Food taco = food("Quimera publico", FoodSource.TACO);
        seedTaco(taco);

        assertThat(repository().findById(otherOwner(), taco.getId())).isPresent();
        assertThat(repository().findAllById(otherOwner(), List.of(taco.getId()))).containsExactly(taco);
        assertThat(names(repository().search(otherOwner(), FoodSearchQuery.of("quimera publico"), 20)))
                .containsExactly("Quimera publico");
    }

    private List<Food> search(String query) {
        return repository().search(owner(), FoodSearchQuery.of(query), 20);
    }

    private void saveCustom(String... names) {
        for (String name : names) {
            repository().save(owner(), food(name, FoodSource.CUSTOM));
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
