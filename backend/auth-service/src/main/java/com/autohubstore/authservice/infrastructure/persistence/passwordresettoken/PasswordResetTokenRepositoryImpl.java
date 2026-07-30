package com.autohubstore.authservice.infrastructure.persistence.passwordresettoken;

import com.autohubstore.authservice.domain.model.PasswordResetToken;
import com.autohubstore.authservice.domain.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpa;

    @Override
    @Transactional
    public PasswordResetToken save(PasswordResetToken token) {
        jpa.save(toEntity(token));
        return token;
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpa.findByToken(token).map(this::toDomain);
    }

    private PasswordResetTokenJpaEntity toEntity(PasswordResetToken domain) {
        return new PasswordResetTokenJpaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getToken(),
                domain.getExpiresAt(),
                domain.getCreatedAt(),
                domain.isUsed()
        );
    }

    private PasswordResetToken toDomain(PasswordResetTokenJpaEntity entity) {
        return new PasswordResetToken(
                entity.getId(),
                entity.getUserId(),
                entity.getToken(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.isUsed()
        );
    }

}
