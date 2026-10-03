package com.chuanqing.service;

import com.chuanqing.config.AuthConfig;
import com.chuanqing.config.AuthPropertiesConfig;
import com.chuanqing.entity.UserEntity;
import com.chuanqing.vo.TokenPairVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AuthPropertiesConfig properties = new AuthPropertiesConfig();
        properties.getJwt().setIssuer("test-issuer");
        properties.getJwt().setPrivateKey(new ClassPathResource("keys/private.pem"));
        properties.getJwt().setPublicKey(new ClassPathResource("keys/public.pem"));
        AuthConfig configuration = new AuthConfig(properties);
        JwtEncoder encoder = configuration.jwtEncoder();
        JwtDecoder decoder = configuration.jwtDecoder();
        jwtService = new JwtService(encoder, decoder, properties);
    }

    @Test
    void issueTokenPairAndDecode() {
        UserEntity user = UserEntity.builder()
                .id(123L)
                .nickname("tester")
                .build();

        TokenPairVO tokenPair = jwtService.issueTokenPair(user);

        assertThat(tokenPair.accessToken()).isNotBlank();
        assertThat(tokenPair.refreshToken()).isNotBlank();
        assertThat(tokenPair.refreshTokenId()).isNotBlank();

        Jwt accessJwt = jwtService.decode(tokenPair.accessToken());
        assertThat(jwtService.extractTokenType(accessJwt)).isEqualTo("access");
        assertThat(jwtService.extractUserId(accessJwt)).isEqualTo(123L);

        Jwt refreshJwt = jwtService.decode(tokenPair.refreshToken());
        assertThat(jwtService.extractTokenType(refreshJwt)).isEqualTo("refresh");
        assertThat(jwtService.extractUserId(refreshJwt)).isEqualTo(123L);
        assertThat(jwtService.extractTokenId(refreshJwt)).isEqualTo(tokenPair.refreshTokenId());
    }
}
