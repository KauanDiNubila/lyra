package app.lyra.identity.application.service;

import app.lyra.identity.application.port.out.UserRepository;
import app.lyra.identity.domain.AuthProvider;
import app.lyra.identity.domain.User;
import app.lyra.shared.domain.UserId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

class InMemoryUserRepository implements UserRepository {

    private final Map<UserId, User> users = new LinkedHashMap<>();

    @Override
    public void save(User user) {
        boolean subjectTaken = users.values().stream().anyMatch(other -> !other.equals(user)
                && other.getProvider() == user.getProvider()
                && other.getProviderSubject().equals(user.getProviderSubject()));
        if (subjectTaken) {
            throw new IllegalStateException("The subject of the provider is already used");
        }
        users.put(user.getId(), user);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByProvider(AuthProvider provider, String providerSubject) {
        return users.values().stream()
                .filter(user -> user.getProvider() == provider && user.getProviderSubject().equals(providerSubject))
                .findFirst();
    }

    int count() {
        return users.size();
    }
}
