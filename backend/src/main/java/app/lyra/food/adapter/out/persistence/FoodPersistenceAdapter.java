package app.lyra.food.adapter.out.persistence;

import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.food.domain.SearchText;
import app.lyra.shared.domain.UserId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
class FoodPersistenceAdapter implements FoodRepository {

    private static final char LIKE_ESCAPE = '\\';
    private static final String PRODUCT_REFERENCE_PREFIX = "off:";
    private static final int PRODUCT_LOCK_NAMESPACE = 7_403;

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void save(UserId owner, Food food) {
        if (food.getSource() != FoodSource.CUSTOM) {
            throw new IllegalArgumentException("Only custom foods are saved by a user");
        }
        FoodJpaEntity entity = toEntity(food);
        entity.setUserId(owner.value());
        entityManager.persist(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Food> findById(UserId user, UUID id) {
        return findAllById(user, List.of(id)).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Food> findAllById(UserId user, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return entityManager
                .createQuery("select f from FoodJpaEntity f where f.id in :ids and (f.userId is null or f.userId = :user)",
                        FoodJpaEntity.class)
                .setParameter("ids", ids)
                .setParameter("user", user.value())
                .getResultList().stream()
                .map(FoodPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Food> search(UserId user, FoodSearchQuery query, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<FoodJpaEntity> criteria = cb.createQuery(FoodJpaEntity.class);
        Root<FoodJpaEntity> food = criteria.from(FoodJpaEntity.class);

        Predicate visibleToTheUser = cb.or(cb.isNull(food.get("userId")), cb.equal(food.get("userId"), user.value()));
        Predicate[] visibleAndMatching = Stream.concat(
                        Stream.of(visibleToTheUser),
                        query.terms().stream().map(term -> cb.like(food.get("searchName"), containsPattern(term), LIKE_ESCAPE)))
                .toArray(Predicate[]::new);

        criteria.where(visibleAndMatching).orderBy(
                cb.asc(cb.selectCase().when(cb.equal(food.get("source"), FoodSource.CUSTOM), 0).otherwise(1)),
                cb.asc(cb.length(food.get("name"))));

        return entityManager.createQuery(criteria).setMaxResults(limit).getResultList().stream()
                .map(FoodPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Food> findCustom(UserId user) {
        return entityManager
                .createQuery("select f from FoodJpaEntity f where f.userId = :user order by f.name, f.id", FoodJpaEntity.class)
                .setParameter("user", user.value())
                .getResultList().stream()
                .map(FoodPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Food> findByBarcode(Barcode barcode) {
        return entityManager
                .createQuery("select f from FoodJpaEntity f where f.source = :source and f.sourceReference = :reference",
                        FoodJpaEntity.class)
                .setParameter("source", FoodSource.OPEN_FOOD_FACTS)
                .setParameter("reference", PRODUCT_REFERENCE_PREFIX + barcode.value())
                .getResultStream()
                .findFirst()
                .map(FoodPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public Food saveProduct(Barcode barcode, Food product) {
        if (product.getSource() != FoodSource.OPEN_FOOD_FACTS) {
            throw new IllegalArgumentException("Only products of the Open Food Facts are saved by barcode");
        }
        lockProduct(barcode);
        return findByBarcode(barcode).orElseGet(() -> {
            FoodJpaEntity entity = toEntity(product);
            entity.setSourceReference(PRODUCT_REFERENCE_PREFIX + barcode.value());
            entityManager.persist(entity);
            return product;
        });
    }

    private void lockProduct(Barcode barcode) {
        entityManager.createNativeQuery("select count(*) from (select pg_advisory_xact_lock(:barcode, :namespace)) as locked")
                .setParameter("barcode", barcode.value().hashCode())
                .setParameter("namespace", PRODUCT_LOCK_NAMESPACE)
                .getSingleResult();
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
