package app.lyra.food.application.service;

import app.lyra.food.application.port.out.ProductCatalog;
import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.ProductNotFoundException;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.ServiceUnavailableException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LookupFoodByBarcodeServiceTest {

    private static final UserId USER = UserId.random();
    private static final String CODE = "7891000100103";
    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Nutrients NUTRIENTS = new Nutrients(new BigDecimal("500"), new BigDecimal("6"),
            new BigDecimal("60"), new BigDecimal("30"), null, null);

    private final InMemoryFoodRepository repository = new InMemoryFoodRepository();
    private final List<Barcode> asked = new ArrayList<>();

    private LookupFoodByBarcodeService serviceWith(ProductCatalog catalog) {
        return new LookupFoodByBarcodeService(repository, barcode -> {
            asked.add(barcode);
            return catalog.findByBarcode(barcode);
        });
    }

    private static Food product(String name) {
        return new Food(UUID.randomUUID(), name, FoodSource.OPEN_FOOD_FACTS, HUNDRED_GRAMS, NUTRIENTS);
    }

    @Test
    void fetchesTheProductFromTheCatalogStoresItAndReturnsIt() {
        Food product = product("Biscoito");
        LookupFoodByBarcodeService service = serviceWith(barcode -> Optional.of(product));

        Food found = service.lookup(USER, CODE);

        assertThat(found).isEqualTo(product);
        assertThat(asked).containsExactly(new Barcode(CODE));
        assertThat(repository.findByBarcode(new Barcode(CODE))).contains(product);
    }

    @Test
    void asksTheCatalogOnlyOnceForTheSameCode() {
        LookupFoodByBarcodeService service = serviceWith(barcode -> Optional.of(product("Biscoito")));

        Food first = service.lookup(USER, CODE);
        Food second = service.lookup(UserId.random(), CODE);

        assertThat(second).isEqualTo(first);
        assertThat(asked).hasSize(1);
    }

    @Test
    void returnsTheProductThatWonTheRaceWhenAnotherRequestStoredItFirst() {
        Food winner = product("Biscoito do vencedor");
        LookupFoodByBarcodeService service = serviceWith(barcode -> {
            repository.saveProduct(barcode, winner);
            return Optional.of(product("Biscoito do perdedor"));
        });

        Food found = service.lookup(USER, CODE);

        assertThat(found).isEqualTo(winner);
    }

    @Test
    void failsWhenTheCatalogDoesNotKnowTheProduct() {
        LookupFoodByBarcodeService service = serviceWith(barcode -> Optional.empty());

        assertThatThrownBy(() -> service.lookup(USER, CODE))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: " + CODE);
        assertThat(repository.findByBarcode(new Barcode(CODE))).isEmpty();
    }

    @Test
    void storesNothingWhenTheCatalogFindsTheProductWithoutUsableData() {
        LookupFoodByBarcodeService service = serviceWith(barcode -> {
            throw ProductNotFoundException.incomplete(barcode);
        });

        assertThatThrownBy(() -> service.lookup(USER, CODE))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product " + CODE + " has no usable name or nutrition data");
        assertThat(repository.findByBarcode(new Barcode(CODE))).isEmpty();
    }

    @Test
    void storesNothingWhenTheCatalogIsUnavailable() {
        LookupFoodByBarcodeService service = serviceWith(barcode -> {
            throw new ServiceUnavailableException("The product source is unavailable, try again later", null);
        });

        assertThatThrownBy(() -> service.lookup(USER, CODE)).isInstanceOf(ServiceUnavailableException.class);
        assertThat(repository.findByBarcode(new Barcode(CODE))).isEmpty();
    }

    @Test
    void refusesAnInvalidCodeWithoutAskingTheCatalog() {
        LookupFoodByBarcodeService service = serviceWith(barcode -> Optional.of(product("Biscoito")));

        assertThatThrownBy(() -> service.lookup(USER, "12ab"))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Barcode must have between 8 and 14 digits");
        assertThat(asked).isEmpty();
    }
}
