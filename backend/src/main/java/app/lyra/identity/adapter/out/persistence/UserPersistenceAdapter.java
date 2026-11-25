package app.lyra.identity.adapter.out.persistence;

import app.lyra.identity.application.port.out.UserRepository;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.shared.domain.UserId;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class UserPersistenceAdapter implements UserRepository {

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void save(User user) {
        entityManager.merge(toEntity(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(entityManager.find(UserJpaEntity.class, id.value()))
                .map(UserPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByProvider(AuthProvider provider, String providerSubject) {
        return entityManager
                .createQuery("select u from UserJpaEntity u where u.provider = :provider and u.providerSubject = :subject",
                        UserJpaEntity.class)
                .setParameter("provider", provider)
                .setParameter("subject", providerSubject)
                .getResultStream()
                .findFirst()
                .map(UserPersistenceAdapter::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(UserId id) {
        entityManager.createQuery("delete from UserJpaEntity u where u.id = :id")
                .setParameter("id", id.value())
                .executeUpdate();
    }

    private static UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId().value());
        entity.setProvider(user.getProvider());
        entity.setProviderSubject(user.getProviderSubject());
        entity.setEmail(user.getEmail());
        entity.setName(user.getName());
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }

    private static User toDomain(UserJpaEntity entity) {
        return new User(new UserId(entity.getId()), entity.getProvider(), entity.getProviderSubject(),
                entity.getEmail(), entity.getName(), entity.getCreatedAt());
    }
}
