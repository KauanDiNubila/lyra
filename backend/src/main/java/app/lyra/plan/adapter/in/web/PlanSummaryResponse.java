package app.lyra.plan.adapter.in.web;

import app.lyra.plan.domain.DietPlan;

import java.util.UUID;

record PlanSummaryResponse(UUID id, String name, boolean active) {

    static PlanSummaryResponse from(DietPlan plan) {
        return new PlanSummaryResponse(plan.getId(), plan.getName(), plan.isActive());
    }
}
