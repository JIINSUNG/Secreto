package com.pjg.secreto.user.common.entity;

import jakarta.persistence.Id;
import jakarta.persistence.Index;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RedisHash(value = "emailCheck" )
public class EmailCheck {
    @Id
    private Long id;

    @Indexed
    private String email;

    private String validationCode;

    @Value("${email.expiration}")
    private Long timeToLive;

}