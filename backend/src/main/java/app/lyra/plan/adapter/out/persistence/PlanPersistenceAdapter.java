package app.lyra.plan.adapter.out.persistence;

import app.lyra.food.domain.Quantity;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.domain.DailyTargets;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
class PlanPersistenceAdapter implements PlanRepository {

    private static final long PLAN_CHANGES_LOCK_KEY = 7_401_001L;

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void lockPlanChanges() {
        entityManager.createNativeQuery("select count(*) from (select pg_advisory_xact_lock(:key)) as locked")
                .setParameter("key", PLAN_CHANGES_LOCK_KEY)
                .getSingleResult();
    }

    @Override
    @Transactional
    public void save(DietPlan plan) {
        entityManager.merge(toEntity(plan));
        entityManager.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DietPlan> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(DietPlanJpaEntity.class, id))
                .map(PlanPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DietPlan> findAll() {
        return entityManager
                .createQuery("select p from DietPlanJpaEntity p order by p.planOrder", DietPlanJpaEntity.class)
                .getResultList().stream()
                .map(PlanPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DietPlan> findActive() {
        return entityManager
                .createQuery("select p from DietPlanJpaEntity p where p.active = true", DietPlanJpaEntity.class)
                .getResultStream()
                .findFirst()
                .map(PlanPersistenceAdapter::toDomain);
    }

    private static DietPlanJpaEntity toEntity(DietPlan plan) {
        DietPlanJpaEntity entity = new DietPlanJpaEntity();
        entity.setId(plan.getId());
        entity.setName(plan.getName());
        entity.setActive(plan.isActive());
        entity.setTargetKcal(plan.getTargets().kcal());
        entity.setTargetProtein(plan.getTargets().protein());
        entity.setTargetCarbohydrate(plan.getTargets().carbohydrate());
        entity.setTargetFat(plan.getTargets().fat());
        List<PlannedMeal> meals = plan.getMeals();
        entity.setMeals(IntStream.range(0, meals.size())
                .mapToObj(index -> toEntity(plan.getId(), meals.get(index), index)).toList());
        return entity;
    }

    private static PlannedMealJpaEntity toEntity(UUID planId, PlannedMeal meal, int order) {
        PlannedMealJpaEntity entity = new PlannedMealJpaEntity();
        entity.setId(childId(planId, order));
        entity.setMealOrder(order);
        entity.setName(meal.name());
        entity.setMealTime(meal.time());
        List<PlannedItem> items = meal.items();
        entity.setItems(IntStream.range(0, items.size())
                .mapToObj(index -> toEntity(entity.getId(), items.get(index), index)).toList());
        return entity;
    }

    private static PlannedItemJpaEntity toEntity(UUID mealId, PlannedItem item, int order) {
        PlannedItemJpaEntity entity = new PlannedItemJpaEntity();
        entity.setId(childId(mealId, order));
        entity.setItemOrder(order);
        entity.setFoodId(item.foodId());
        entity.setQuantityAmount(item.quantity().amount());
        entity.setQuantityUnit(item.quantity().unit());
        return entity;
    }

    private static UUID childId(UUID parentId, int order) {
        return UUID.nameUUIDFromBytes((parentId + ":" + order).getBytes(StandardCharsets.UTF_8));
    }

    private static DietPlan toDomain(DietPlanJpaEntity entity) {
        return new DietPlan(
                entity.getId(),
                entity.getName(),
                new DailyTargets(entity.getTargetKcal(), entity.getTargetProtein(), entity.getTargetCarbohydrate(),
                        entity.getTargetFat()),
                entity.getMeals().stream().map(PlanPersistenceAdapter::toDomain).toList(),
                entity.isActive());
    }

    private static PlannedMeal toDomain(PlannedMealJpaEntity entity) {
        return new PlannedMeal(
                entity.getName(),
                entity.getMealTime(),
                entity.getItems().stream().map(PlanPersistenceAdapter::toDomain).toList());
    }

    private static PlannedItem toDomain(PlannedItemJpaEntity entity) {
        return new PlannedItem(entity.getFoodId(), new Quantity(entity.getQuantityAmount(), entity.getQuantityUnit()));
    }
}
