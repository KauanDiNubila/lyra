package app.lyra.plan.adapter.in.web;

import app.lyra.plan.application.port.in.ActivatePlanUseCase;
import app.lyra.plan.application.port.in.CreatePlanUseCase;
import app.lyra.plan.application.port.in.GetPlanUseCase;
import app.lyra.plan.application.port.in.ListPlansUseCase;
import app.lyra.plan.application.port.in.ReplacePlanUseCase;
import app.lyra.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
class PlanController {

    private final ListPlansUseCase listPlansUseCase;
    private final CreatePlanUseCase createPlanUseCase;
    private final GetPlanUseCase getPlanUseCase;
    private final ReplacePlanUseCase replacePlanUseCase;
    private final ActivatePlanUseCase activatePlanUseCase;
    private final PlanResponseFactory responses;

    @GetMapping
    List<PlanSummaryResponse> listPlans(UserId userId) {
        return listPlansUseCase.listPlans(userId).stream().map(PlanSummaryResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanResponse createPlan(UserId userId, @RequestBody PlanRequest request) {
        return responses.of(userId, createPlanUseCase.create(userId, request.name(), request.toTargets(), request.toMeals()));
    }

    @GetMapping("/{id}")
    PlanResponse getPlan(UserId userId, @PathVariable UUID id) {
        return responses.of(userId, getPlanUseCase.getPlan(userId, id));
    }

    @PutMapping("/{id}")
    PlanResponse replacePlan(UserId userId, @PathVariable UUID id, @RequestBody PlanRequest request) {
        return responses.of(userId, replacePlanUseCase.replace(userId, id, request.name(), request.toTargets(), request.toMeals()));
    }

    @PostMapping("/{id}/activation")
    PlanResponse activatePlan(UserId userId, @PathVariable UUID id) {
        return responses.of(userId, activatePlanUseCase.activate(userId, id));
    }
}
