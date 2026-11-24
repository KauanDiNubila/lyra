package app.lyra.plan.adapter.out.persistence;

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
@Table(name = "planned_item")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PlannedItemJpaEntity {

    @Id
    private UUID id;
    private int itemOrder;
    private UUID foodId;
    private BigDecimal quantityAmount;
    @Enumerated(EnumType.STRING)
    private MeasurementUnit quantityUnit;
}
