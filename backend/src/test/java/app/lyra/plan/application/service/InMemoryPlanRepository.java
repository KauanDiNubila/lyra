package app.lyra.plan.application.service;

import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DietPlan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryPlanRepository implements PlanRepository {

    private final Map<UUID, DietPlan> plans = new LinkedHashMap<>();

    @Override
    public void save(DietPlan plan) {
        boolean anotherIsActive = plans.values().stream().anyMatch(other -> other.isActive() && !other.equals(plan));
        if (plan.isActive() && anotherIsActive) {
            throw new IllegalStateException("Only one plan can be active");
        }
        plans.put(plan.getId(), plan);
    }

    @Override
    public Optional<DietPlan> findById(UUID id) {
        return Optional.ofNullable(plans.get(id));
    }

    @Override
    public List<DietPlan> findAll() {
        return new ArrayList<>(plans.values());
    }

    @Override
    public Optional<DietPlan> findActive() {
        return plans.values().stream().filter(DietPlan::isActive).findFirst();
    }
}
