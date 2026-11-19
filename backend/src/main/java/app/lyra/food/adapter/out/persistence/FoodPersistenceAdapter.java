package app.lyra.food.adapter.out.persistence;

import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.food.domain.SearchText;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class FoodPersistenceAdapter implements FoodRepository {

    private static final char LIKE_ESCAPE = '\\';

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void save(Food food) {
        entityManager.persist(toEntity(food));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Food> search(FoodSearchQuery query, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FoodJpaEntity> criteria = cb.createQuery(FoodJpaEntity.class);
        Root<FoodJpaEntity> food = criteria.from(FoodJpaEntity.class);

        Predicate[] everyTerm = query.terms().stream()
                .map(term -> cb.like(food.get("searchName"), containsPattern(term), LIKE_ESCAPE))
                .toArray(Predicate[]::new);

        criteria.where(everyTerm).orderBy(
                cb.asc(cb.selectCase().when(cb.equal(food.get("source"), FoodSource.CUSTOM), 0).otherwise(1)),
                cb.asc(cb.length(food.get("name"))));

        return entityManager.createQuery(criteria).setMaxResults(limit).getResultList().stream()
                .map(FoodPersistenceAdapter::toDomain)
                .toList();
    }

    private static String containsPattern(String term) {
        String literal = term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + literal + "%";
    }

    private static FoodJpaEntity toEntity(Food food) {
        FoodJpaEntity entity = new FoodJpaEntity();
        entity.setId(food.getId());
        entity.setName(food.getName());
        entity.setSearchName(SearchText.normalize(food.getName()));
        entity.setSource(food.getSource());
        entity.setReferenceAmount(food.getReferenceAmount().amount());
        entity.setReferenceUnit(food.getReferenceAmount().unit());
        Nutrients nutrients = food.getNutrients();
        entity.setKcal(nutrients.kcal());
        entity.setProtein(nutrients.protein());
        entity.setCarbohydrate(nutrients.carbohydrate());
        entity.setFat(nutrients.fat());
        entity.setFiber(nutrients.fiber());
        entity.setSodium(nutrients.sodium());
        return entity;
    }

    private static Food toDomain(FoodJpaEntity entity) {
        return new Food(
                entity.getId(),
                entity.getName(),
                entity.getSource(),
                new Quantity(entity.getReferenceAmount(), entity.getReferenceUnit()),
                new Nutrients(entity.getKcal(), entity.getProtein(), entity.getCarbohydrate(), entity.getFat(),
                        entity.getFiber(), entity.getSodium()));
    }
}
