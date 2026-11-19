package app.lyra.food.adapter.in.web;

import app.lyra.food.application.port.in.CreateCustomFoodUseCase;
import app.lyra.food.application.port.in.SearchFoodsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
class FoodController {

    private final SearchFoodsUseCase searchFoods;
    private final CreateCustomFoodUseCase createCustomFood;

    @GetMapping
    List<FoodResponse> search(@RequestParam String query) {
        return searchFoods.search(query).stream().map(FoodResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    FoodResponse create(@Valid @RequestBody CreateFoodRequest request) {
        return FoodResponse.from(createCustomFood.create(
                request.name(), request.referenceAmount().toDomain(), request.nutrients().toDomain()));
    }
}
