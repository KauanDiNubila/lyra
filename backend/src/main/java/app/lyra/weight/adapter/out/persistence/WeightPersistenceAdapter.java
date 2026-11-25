package app.lyra.weight.adapter.out.persistence;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.application.port.out.WeightRepository;
import app.lyra.weight.domain.WeightEntry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
class WeightPersistenceAdapter implements WeightRepository {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void save(UserId owner, WeightEntry entry) {
        entityManager.createNativeQuery("""
                        insert into weight_entry (user_id, entry_date, kilograms)
                        values (:user, :date, :kilograms)
                        on conflict (user_id, entry_date) do update set kilograms = excluded.kilograms
                        """)
                .setParameter("user", owner.value())
                .setParameter("date", entry.date())
                .setParameter("kilograms", entry.kilograms())
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WeightEntry> findAll(UserId user) {
        return entityManager
                .createQuery("select w from WeightEntryJpaEntity w where w.key.userId = :user order by w.key.entryDate",
                        WeightEntryJpaEntity.class)
                .setParameter("user", user.value())
                .getResultList().stream()
                .map(entity -> new WeightEntry(entity.getKey().entryDate(), entity.getKilograms()))
                .toList();
    }

    @Override
    @Transactional
    public boolean delete(UserId user, LocalDate date) {
        int deleted = entityManager
                .createQuery("delete from WeightEntryJpaEntity w where w.key.userId = :user and w.key.entryDate = :date")
                .setParameter("user", user.value())
                .setParameter("date", date)
                .executeUpdate();
        return deleted > 0;
    }
}
