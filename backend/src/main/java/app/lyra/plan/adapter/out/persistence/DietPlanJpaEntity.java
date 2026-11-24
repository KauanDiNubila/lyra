package app.lyra.plan.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "diet_plan")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DietPlanJpaEntity {

    @Id
    private UUID id;
    @Column(insertable = false, updatable = false)
    private Long planOrder;
    private String name;
    private boolean active;
    private BigDecimal targetKcal;
    private BigDecimal targetProtein;
    private BigDecimal targetCarbohydrate;
    private BigDecimal targetFat;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @JoinColumn(name = "plan_id", nullable = false)
    @OrderBy("mealOrder")
    private List<PlannedMealJpaEntity> meals = new ArrayList<>();
}
