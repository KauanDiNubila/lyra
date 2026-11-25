package app.lyra.food.application.service;

import app.lyra.food.application.port.in.LookupFoodByBarcodeUseCase;
import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.application.port.out.ProductCatalog;
import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.ProductNotFoundException;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class LookupFoodByBarcodeService implements LookupFoodByBarcodeUseCase {

    private final FoodRepository foodRepository;
    private final ProductCatalog productCatalog;

    @Override
    public Food lookup(UserId userId, String code) {
        Barcode barcode = new Barcode(code);
        return foodRepository.findByBarcode(barcode).orElseGet(() -> fetchAndStore(barcode));
    }

    private Food fetchAndStore(Barcode barcode) {
        Food product = productCatalog.findByBarcode(barcode).orElseThrow(() -> ProductNotFoundException.missing(barcode));
        return foodRepository.saveProduct(barcode, product);
    }
}
