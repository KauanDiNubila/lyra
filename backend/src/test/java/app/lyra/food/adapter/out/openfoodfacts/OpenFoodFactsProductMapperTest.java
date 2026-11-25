package app.lyra.food.adapter.out.openfoodfacts;

import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenFoodFactsProductMapperTest {

    private static final Barcode BARCODE = new Barcode("3017620422003");
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String COMPLETE = """
            {"code": "3017620422003", "status": 1, "status_verbose": "product found",
             "product": {"product_name": "Nutella", "brands": "Ferrero, Nutella", "product_quantity_unit": "g",
                         "nutriments": {"energy-kcal_100g": 539, "proteins_100g": 6.3, "carbohydrates_100g": 57.5,
                                        "fat_100g": 30.9, "fiber_100g": 0, "sodium_100g": 0.0428}}}
            """;

    private static Optional<Food> map(String json) {
        JsonNode body = JSON.readTree(json);
        return OpenFoodFactsProductMapper.toFood(BARCODE, body);
    }

    private static String productWith(String productFields, String nutriments) {
        return """
                {"status": 1, "product": {%s "nutriments": {%s}}}
                """.formatted(productFields, nutriments);
    }

    private static final String ALL_MACROS = "\"energy-kcal_100g\": 100, \"proteins_100g\": 1, \"carbohydrates_100g\": 2, \"fat_100g\": 3";

    @Test
    void mapsACompleteProductToAFoodPerOneHundredGrams() {
        Food food = map(COMPLETE).orElseThrow();

        assertThat(food.getSource()).isEqualTo(FoodSource.OPEN_FOOD_FACTS);
        assertThat(food.getName()).isEqualTo("Nutella (Ferrero)");
        assertThat(food.getReferenceAmount().amount()).isEqualByComparingTo("100");
        assertThat(food.getReferenceAmount().unit()).isEqualTo(MeasurementUnit.GRAM);
        assertThat(food.getNutrients().kcal()).isEqualByComparingTo("539");
        assertThat(food.getNutrients().protein()).isEqualByComparingTo("6.3");
        assertThat(food.getNutrients().carbohydrate()).isEqualByComparingTo("57.5");
        assertThat(food.getNutrients().fat()).isEqualByComparingTo("30.9");
        assertThat(food.getNutrients().fiber()).isEqualByComparingTo("0");
    }

    @Test
    void convertsTheSodiumFromGramsToMilligrams() {
        Food food = map(COMPLETE).orElseThrow();

        assertThat(food.getNutrients().sodium()).isEqualByComparingTo("42.8");
    }

    @Test
    void keepsTheFiberAndTheSodiumUnknownWhenTheProductHasNoValueForThem() {
        Food food = map(productWith("\"product_name\": \"Suco\",", ALL_MACROS)).orElseThrow();

        assertThat(food.getNutrients().fiber()).isNull();
        assertThat(food.getNutrients().sodium()).isNull();
    }

    @Test
    void usesMillilitersWhenTheProductIsSoldInMilliliters() {
        Food food = map(productWith("\"product_name\": \"Suco\", \"product_quantity_unit\": \"ml\",", ALL_MACROS)).orElseThrow();

        assertThat(food.getReferenceAmount().unit()).isEqualTo(MeasurementUnit.MILLILITER);
    }

    @Test
    void doesNotRepeatTheBrandThatIsAlreadyInTheName() {
        Food food = map(productWith("\"product_name\": \"Nutella 400g\", \"brands\": \"nutella, Ferrero\",", ALL_MACROS)).orElseThrow();

        assertThat(food.getName()).isEqualTo("Nutella 400g");
    }

    @Test
    void usesTheNameAloneWhenThereIsNoBrand() {
        Food food = map(productWith("\"product_name\": \" Suco \", \"brands\": \" , \",", ALL_MACROS)).orElseThrow();

        assertThat(food.getName()).isEqualTo("Suco");
    }


    @Test
    void acceptsAProductThatIsPureFat() {
        Food food = map(productWith("\"product_name\": \"Azeite\",",
                "\"energy-kcal_100g\": 900, \"proteins_100g\": 0, \"carbohydrates_100g\": 0, \"fat_100g\": 100")).orElseThrow();

        assertThat(food.getNutrients().kcal()).isEqualByComparingTo("900");
    }

    @Test
    void acceptsMacrosThatSumAFewGramsOverOneHundredBecauseOfRounding() {
        Food food = map(productWith("\"product_name\": \"Bolacha\",",
                "\"energy-kcal_100g\": 450, \"proteins_100g\": 10.2, \"carbohydrates_100g\": 70.3, \"fat_100g\": 20.1")).orElseThrow();

        assertThat(food.getNutrients().fat()).isEqualByComparingTo("20.1");
    }

    @Test
    void refusesCaloriesThatNoFoodCanHavePerOneHundredGrams() {
        assertThatThrownBy(() -> map(productWith("\"product_name\": \"Erro\",",
                "\"energy-kcal_100g\": 900.1, \"proteins_100g\": 0, \"carbohydrates_100g\": 0, \"fat_100g\": 100")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void refusesAMacroAboveOneHundredGrams() {
        assertThatThrownBy(() -> map(productWith("\"product_name\": \"Erro\",",
                "\"energy-kcal_100g\": 400, \"proteins_100g\": 101, \"carbohydrates_100g\": 0, \"fat_100g\": 0")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void refusesMacrosThatSumMoreThanTheProductCanWeigh() {
        assertThatThrownBy(() -> map(productWith("\"product_name\": \"Erro\",",
                "\"energy-kcal_100g\": 800, \"proteins_100g\": 40, \"carbohydrates_100g\": 40, \"fat_100g\": 25.1")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void refusesAFiberOrASodiumAboveOneHundredGrams() {
        assertThatThrownBy(() -> map(productWith("\"product_name\": \"Erro\",", ALL_MACROS + ", \"fiber_100g\": 101")))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> map(productWith("\"product_name\": \"Erro\",", ALL_MACROS + ", \"sodium_100g\": 101")))
                .isInstanceOf(ProductNotFoundException.class);
    }
    @Test
    void answersNothingWhenTheSourceDoesNotKnowTheCode() {
        assertThat(map("{\"code\": \"00000000\", \"status\": 0, \"status_verbose\": \"no code or invalid code\"}")).isEmpty();
    }

    @Test
    void refusesAProductWithoutAName() {
        assertThatThrownBy(() -> map(productWith("\"brands\": \"Ferrero\",", ALL_MACROS)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product 3017620422003 has no usable name or nutrition data");
    }

    @Test
    void refusesAProductWhenAnyOfTheFourMainValuesIsMissing() {
        String[] withoutOne = {
                "\"proteins_100g\": 1, \"carbohydrates_100g\": 2, \"fat_100g\": 3",
                "\"energy-kcal_100g\": 100, \"carbohydrates_100g\": 2, \"fat_100g\": 3",
                "\"energy-kcal_100g\": 100, \"proteins_100g\": 1, \"fat_100g\": 3",
                "\"energy-kcal_100g\": 100, \"proteins_100g\": 1, \"carbohydrates_100g\": 2"};

        for (String nutriments : withoutOne) {
            assertThatThrownBy(() -> map(productWith("\"product_name\": \"Suco\",", nutriments)))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    @Test
    void refusesAProductWhoseValuesAreNotNumbers() {
        String nutriments = "\"energy-kcal_100g\": \"100\", \"proteins_100g\": 1, \"carbohydrates_100g\": 2, \"fat_100g\": 3";

        assertThatThrownBy(() -> map(productWith("\"product_name\": \"Suco\",", nutriments)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void refusesAProductWithANegativeValue() {
        String nutriments = "\"energy-kcal_100g\": 100, \"proteins_100g\": -1, \"carbohydrates_100g\": 2, \"fat_100g\": 3";

        assertThatThrownBy(() -> map(productWith("\"product_name\": \"Suco\",", nutriments)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void refusesAProductWhoseNameIsTooShort() {
        assertThatThrownBy(() -> map(productWith("\"product_name\": \"A\",", ALL_MACROS)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void refusesAnAnswerWithoutAProduct() {
        assertThatThrownBy(() -> map("{\"status\": 1}")).isInstanceOf(ProductNotFoundException.class);
    }
}
