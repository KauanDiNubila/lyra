package app.lyra.diary.adapter.out.persistence;

import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.diary.domain.Macros;
import app.lyra.diary.domain.Planned;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class DiaryEntryPersistenceAdapter implements DiaryEntryRepository {

    private static final int DAY_LOCK_NAMESPACE = 7_402;

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void lockDay(LocalDate date) {
        entityManager.createNativeQuery("select count(*) from (select pg_advisory_xact_lock(:namespace, :day)) as locked")
                .setParameter("namespace", DAY_LOCK_NAMESPACE)
                .setParameter("day", (int) date.toEpochDay())
                .getSingleResult();
    }

    @Override
    @Transactional
    public void save(DiaryEntry entry) {
        entityManager.merge(toEntity(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DiaryEntry> findById(UUID id) {
        return Optional.ofNullable(entityManager.find(DiaryEntryJpaEntity.class, id))
                .map(DiaryEntryPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiaryEntry> findByDate(LocalDate date) {
        return entityManager
                .createQuery("select e from DiaryEntryJpaEntity e where e.entryDate = :date order by e.entryOrder",
                        DiaryEntryJpaEntity.class)
                .setParameter("date", date)
                .getResultList().stream()
                .map(DiaryEntryPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        entityManager.createQuery("delete from DiaryEntryJpaEntity e where e.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    private static DiaryEntryJpaEntity toEntity(DiaryEntry entry) {
        DiaryEntryJpaEntity entity = new DiaryEntryJpaEntity();
        entity.setId(entry.getId());
        entity.setEntryDate(entry.getDate());
        entity.setMealName(entry.getMealName());
        entity.setFoodId(entry.getFoodId());
        entity.setFoodName(entry.getFoodName());
        entity.setReferenceAmount(entry.getFoodReference().amount());
        entity.setReferenceUnit(entry.getFoodReference().unit());
        Nutrients nutrients = entry.getNutrients();
        entity.setKcal(nutrients.kcal());
        entity.setProtein(nutrients.protein());
        entity.setCarbohydrate(nutrients.carbohydrate());
        entity.setFat(nutrients.fat());
        entity.setFiber(nutrients.fiber());
        entity.setSodium(nutrients.sodium());
        entity.setQuantityAmount(entry.getQuantity().amount());
        entity.setQuantityUnit(entry.getQuantity().unit());
        entry.getPlanned().ifPresent(planned -> setPlanned(entity, planned));
        return entity;
    }

    private static void setPlanned(DiaryEntryJpaEntity entity, Planned planned) {
        entity.setPlannedFoodId(planned.foodId());
        entity.setPlannedQuantityAmount(planned.quantity().amount());
        entity.setPlannedQuantityUnit(planned.quantity().unit());
        entity.setPlannedKcal(planned.macros().kcal());
        entity.setPlannedProtein(planned.macros().protein());
        entity.setPlannedCarbohydrate(planned.macros().carbohydrate());
        entity.setPlannedFat(planned.macros().fat());
        entity.setPlannedSlot(planned.slot());
    }

    private static DiaryEntry toDomain(DiaryEntryJpaEntity entity) {
        return new DiaryEntry(
                entity.getId(),
                entity.getEntryDate(),
                entity.getMealName(),
                entity.getFoodId(),
                entity.getFoodName(),
                new Quantity(entity.getReferenceAmount(), entity.getReferenceUnit()),
                new Nutrients(entity.getKcal(), entity.getProtein(), entity.getCarbohydrate(), entity.getFat(),
                        entity.getFiber(), entity.getSodium()),
                new Quantity(entity.getQuantityAmount(), entity.getQuantityUnit()),
                toPlanned(entity));
    }

    private static Planned toPlanned(DiaryEntryJpaEntity entity) {
        if (entity.getPlannedSlot() == null) {
            return null;
        }
        return new Planned(
                entity.getPlannedFoodId(),
                new Quantity(entity.getPlannedQuantityAmount(), entity.getPlannedQuantityUnit()),
                new Macros(entity.getPlannedKcal(), entity.getPlannedProtein(), entity.getPlannedCarbohydrate(),
                        entity.getPlannedFat()),
                entity.getPlannedSlot());
    }
}
