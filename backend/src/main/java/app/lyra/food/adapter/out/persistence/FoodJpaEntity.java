package app.lyra.food.adapter.out.persistence;

import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "food")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class FoodJpaEntity {

    @Id
    private UUID id;
    private String name;
    private String searchName;
    private String sourceReference;
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private FoodSource source;
    private BigDecimal referenceAmount;
    @Enumerated(EnumType.STRING)
    private MeasurementUnit referenceUnit;
    private BigDecimal kcal;
    private BigDecimal protein;
    private BigDecimal carbohydrate;
    private BigDecimal fat;
    private BigDecimal fiber;
    private BigDecimal sodium;
}
