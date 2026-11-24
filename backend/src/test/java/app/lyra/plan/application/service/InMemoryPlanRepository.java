package app.lyra.plan.application.service;

import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DietPlan;
import app.lyra.shared.domain.UserId;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryPlanRepository implements PlanRepository {

    private record Stored(UserId owner, DietPlan plan) {
    }

    private final Map<UUID, Stored> plans = new LinkedHashMap<>();

    @Override
    public void lockPlanChanges(UserId user) {
    }

    @Override
    public void save(UserId owner, DietPlan plan) {
        boolean anotherIsActive = plansOf(owner).stream().anyMatch(other -> other.isActive() && !other.equals(plan));
        if (plan.isActive() && anotherIsActive) {
            throw new IllegalStateException("Only one plan can be active");
        }
        plans.put(plan.getId(), new Stored(owner, plan));
    }

    @Override
    public Optional<DietPlan> findById(UserId user, UUID id) {
        return Optional.ofNullable(plans.get(id)).filter(stored -> stored.owner().equals(user)).map(Stored::plan);
    }

    @Override
    public List<DietPlan> findAll(UserId user) {
        return plansOf(user);
    }

    @Override
    public Optional<DietPlan> findActive(UserId user) {
        return plansOf(user).stream().filter(DietPlan::isActive).findFirst();
    }

    private List<DietPlan> plansOf(UserId user) {
        return plans.values().stream().filter(stored -> stored.owner().equals(user)).map(Stored::plan).toList();
    }
}
