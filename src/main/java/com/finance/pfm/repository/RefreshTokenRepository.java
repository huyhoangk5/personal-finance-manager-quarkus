package com.finance.pfm.repository;

import com.finance.pfm.entity.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepository<RefreshToken> {

    public Optional<RefreshToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    public void revokeAllByUserId(Long userId) {
        update("revoked = true where user.userId = ?1 and revoked = false", userId);
    }

    public void deleteExpiredTokens() {
        delete("expiresAt < ?1 or revoked = true",
                java.time.LocalDateTime.now().minusDays(1));
    }
}
