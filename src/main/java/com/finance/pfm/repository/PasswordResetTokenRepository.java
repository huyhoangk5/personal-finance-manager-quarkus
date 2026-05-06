package com.finance.pfm.repository;

import com.finance.pfm.entity.PasswordResetToken;
import com.finance.pfm.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class PasswordResetTokenRepository implements PanacheRepository<PasswordResetToken> {
    public Optional<PasswordResetToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    public void deleteByUser(User user) {
        delete("user", user);
    }
}
