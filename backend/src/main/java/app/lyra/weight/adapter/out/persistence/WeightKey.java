package app.lyra.weight.adapter.out.persistence;

import jakarta.persistence.Embeddable;

import java.time.LocalDate;
import java.util.UUID;

@Embeddable
record WeightKey(UUID userId, LocalDate entryDate) {
}
