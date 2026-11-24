package app.lyra.plan.adapter.in.web;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

record PlanResponse(UUID id, String name, boolean active, TargetsJson targets, List<PlanMealResponse> meals) {

    record PlanMealResponse(String name, LocalTime time, List<PlanItemResponse> items) {
    }

    record PlanItemResponse(UUID foodId, String foodName, QuantityJson quantity) {
    }
}
