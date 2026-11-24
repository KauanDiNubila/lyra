package app.lyra.plan.adapter.out.persistence;

import jakarta.persistence.CascadeType;
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

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "planned_meal")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PlannedMealJpaEntity {

    @Id
    private UUID id;
    private int mealOrder;
    private String name;
    private LocalTime mealTime;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @JoinColumn(name = "meal_id", nullable = false)
    @OrderBy("itemOrder")
    private List<PlannedItemJpaEntity> items = new ArrayList<>();
}
