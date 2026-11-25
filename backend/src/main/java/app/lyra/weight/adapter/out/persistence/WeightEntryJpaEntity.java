package app.lyra.weight.adapter.out.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "weight_entry")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class WeightEntryJpaEntity {

    @EmbeddedId
    private WeightKey key;
    private BigDecimal kilograms;
}
