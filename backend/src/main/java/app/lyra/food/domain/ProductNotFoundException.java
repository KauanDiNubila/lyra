package app.lyra.food.domain;

import app.lyra.shared.domain.NotFoundException;

public class ProductNotFoundException extends NotFoundException {

    private ProductNotFoundException(String message) {
        super(message);
    }

    public static ProductNotFoundException missing(Barcode barcode) {
        return new ProductNotFoundException("Product not found: " + barcode);
    }

    public static ProductNotFoundException incomplete(Barcode barcode) {
        return new ProductNotFoundException("Product " + barcode + " has no usable name or nutrition data");
    }
}
