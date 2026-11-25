package app.lyra.food.application.port.in;

import app.lyra.food.domain.Food;
import app.lyra.shared.domain.UserId;

public interface LookupFoodByBarcodeUseCase {

    Food lookup(UserId userId, String code);
}
