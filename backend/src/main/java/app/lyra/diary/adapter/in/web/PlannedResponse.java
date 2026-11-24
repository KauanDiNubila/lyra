package app.lyra.diary.adapter.in.web;

import app.lyra.diary.domain.Planned;

import java.util.UUID;

record PlannedResponse(UUID foodId, String foodName, QuantityJson quantity, MacrosJson macros) {

    static PlannedResponse from(Planned planned) {
        return new PlannedResponse(planned.foodId(), planned.foodName(), QuantityJson.from(planned.quantity()),
                MacrosJson.from(planned.macros()));
    }
}
