package com.pjg.secreto.user.common.Repository;

import com.pjg.secreto.user.common.entity.RefreshToken;
import com.pjg.secreto.user.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUser(User user);
    void deleteByUser(User user);
}
