package com.finance.pfm.repository;

import com.finance.pfm.entity.User;
import com.finance.pfm.entity.UserQrCode;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class UserQrCodeRepository implements PanacheRepository<UserQrCode> {
    public Optional<UserQrCode> findByQrToken(String qrToken) {
        return find("qrToken", qrToken).firstResultOptional();
    }

    public Optional<UserQrCode> findByUser(User user) {
        return find("user", user).firstResultOptional();
    }
}
