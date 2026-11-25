package app.lyra.food.application.port.out;

import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;

import java.util.Optional;

public interface ProductCatalog {

    Optional<Food> findByBarcode(Barcode barcode);
}
