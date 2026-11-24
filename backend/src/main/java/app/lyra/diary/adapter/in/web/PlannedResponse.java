package app.lyra.diary.adapter.in.web;

import app.lyra.diary.domain.Planned;

import java.util.UUID;

record PlannedResponse(UUID foodId, QuantityJson quantity, MacrosJson macros) {

    static PlannedResponse from(Planned planned) {
        return new PlannedResponse(planned.foodId(), QuantityJson.from(planned.quantity()), MacrosJson.from(planned.macros()));
    }
}
