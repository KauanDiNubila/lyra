package app.lyra.food.adapter.out.openfoodfacts;

import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.ProductNotFoundException;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

final class OpenFoodFactsProductMapper {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MILLIGRAMS_PER_GRAM = new BigDecimal("1000");
    private static final BigDecimal MAX_KCAL = new BigDecimal("900");
    private static final BigDecimal MAX_MACROS = new BigDecimal("105");

    private OpenFoodFactsProductMapper() {
    }

    static Optional<Food> toFood(Barcode barcode, JsonNode body) {
        if (body.path("status").asInt() != 1) {
            return Optional.empty();
        }
        JsonNode product = body.path("product");
        JsonNode nutriments = product.path("nutriments");
        String name = name(product);
        BigDecimal kcal = decimal(nutriments, "energy-kcal_100g");
        BigDecimal protein = decimal(nutriments, "proteins_100g");
        BigDecimal carbohydrate = decimal(nutriments, "carbohydrates_100g");
        BigDecimal fat = decimal(nutriments, "fat_100g");
        BigDecimal fiber = decimal(nutriments, "fiber_100g");
        BigDecimal sodiumInGrams = decimal(nutriments, "sodium_100g");
        if (name == null || kcal == null || protein == null || carbohydrate == null || fat == null) {
            throw ProductNotFoundException.incomplete(barcode);
        }
        if (isImpossibleFor100(kcal, protein, carbohydrate, fat, fiber, sodiumInGrams)) {
            throw ProductNotFoundException.incomplete(barcode);
        }
        BigDecimal sodium = sodiumInGrams == null ? null : sodiumInGrams.multiply(MILLIGRAMS_PER_GRAM);
        try {
            Nutrients nutrients = new Nutrients(kcal, protein, carbohydrate, fat, fiber, sodium);
            Quantity reference = new Quantity(HUNDRED, unit(product));
            return Optional.of(new Food(UUID.randomUUID(), name, FoodSource.OPEN_FOOD_FACTS, reference, nutrients));
        } catch (InvalidInputException invalidData) {
            throw ProductNotFoundException.incomplete(barcode);
        }
    }

    private static boolean isImpossibleFor100(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat,
                                              BigDecimal fiber, BigDecimal sodiumInGrams) {
        BigDecimal macros = protein.add(carbohydrate).add(fat);
        return kcal.compareTo(MAX_KCAL) > 0
                || macros.compareTo(MAX_MACROS) > 0
                || exceedsAHundred(protein, carbohydrate, fat, fiber, sodiumInGrams);
    }

    private static boolean exceedsAHundred(BigDecimal... amounts) {
        for (BigDecimal amount : amounts) {
            if (amount != null && amount.compareTo(HUNDRED) > 0) {
                return true;
            }
        }
        return false;
    }

    private static String name(JsonNode product) {
        String name = text(product, "product_name");
        if (name == null) {
            return null;
        }
        String brand = firstBrand(text(product, "brands"));
        boolean brandIsNew = brand != null && !name.toLowerCase(Locale.ROOT).contains(brand.toLowerCase(Locale.ROOT));
        return brandIsNew ? name + " (" + brand + ")" : name;
    }

    private static String firstBrand(String brands) {
        if (brands == null) {
            return null;
        }
        for (String brand : brands.split(",")) {
            if (!brand.isBlank()) {
                return brand.trim();
            }
        }
        return null;
    }

    private static MeasurementUnit unit(JsonNode product) {
        return "ml".equalsIgnoreCase(text(product, "product_quantity_unit")) ? MeasurementUnit.MILLILITER : MeasurementUnit.GRAM;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString()) {
            return null;
        }
        String trimmed = value.asString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isNumber() ? value.decimalValue() : null;
    }
}
