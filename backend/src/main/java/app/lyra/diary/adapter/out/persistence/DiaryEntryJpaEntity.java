package app.lyra.diary.adapter.out.persistence;

import app.lyra.food.domain.MeasurementUnit;
import jakarta.persistence.Column;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "diary_entry")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DiaryEntryJpaEntity {

    @Id
    private UUID id;
    @Column(insertable = false, updatable = false)
    private Long entryOrder;
    private LocalDate entryDate;
    private String mealName;
    private UUID foodId;
    private String foodName;
    private BigDecimal referenceAmount;
    @Enumerated(EnumType.STRING)
    private MeasurementUnit referenceUnit;
    private BigDecimal kcal;
    private BigDecimal protein;
    private BigDecimal carbohydrate;
    private BigDecimal fat;
    private BigDecimal fiber;
    private BigDecimal sodium;
    private BigDecimal quantityAmount;
    @Enumerated(EnumType.STRING)
    private MeasurementUnit quantityUnit;
    private UUID plannedFoodId;
    private String plannedFoodName;
    private BigDecimal plannedQuantityAmount;
    @Enumerated(EnumType.STRING)
    private MeasurementUnit plannedQuantityUnit;
    private BigDecimal plannedKcal;
    private BigDecimal plannedProtein;
    private BigDecimal plannedCarbohydrate;
    private BigDecimal plannedFat;
    private Integer plannedSlot;
}
