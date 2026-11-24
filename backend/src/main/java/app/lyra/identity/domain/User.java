package app.lyra.identity.domain;

import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.Instant;
import java.util.Locale;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @EqualsAndHashCode.Include
    private final UserId id;
    private final AuthProvider provider;
    private final String providerSubject;
    private final String email;
    private final String name;
    private final Instant createdAt;

    public User(UserId id, AuthProvider provider, String providerSubject, String email, String name, Instant createdAt) {
        if (id == null || provider == null || createdAt == null) {
            throw new InvalidInputException("A user needs an id, a provider and a creation date");
        }
        if (providerSubject == null || providerSubject.isBlank()) {
            throw new InvalidInputException("The subject of the provider is required");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidInputException("The e-mail is required");
        }
        this.id = id;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.name = name == null || name.isBlank() ? this.email : name.trim();
        this.createdAt = createdAt;
    }

    public static User create(AuthProvider provider, String providerSubject, String email, String name) {
        return new User(UserId.random(), provider, providerSubject, email, name, Instant.now());
    }

    public User withProfile(String newEmail, String newName) {
        return new User(id, provider, providerSubject, newEmail, newName, createdAt);
    }
}
