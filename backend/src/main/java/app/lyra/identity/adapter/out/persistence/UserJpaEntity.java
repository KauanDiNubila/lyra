package app.lyra.identity.adapter.out.persistence;

import app.lyra.identity.domain.AuthProvider;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class UserJpaEntity {

    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    private AuthProvider provider;
    private String providerSubject;
    private String email;
    private String name;
    private Instant createdAt;
}
