package pl.piomin.services;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.List;
import java.util.Map;

/**
 * Accepts any bearer token and reads it as the name of the realm role the user has, so a test sends
 * {@code Authorization: Bearer orders-editor} or {@code Bearer orders-viewer} instead of signing a JWT. Keycloak's
 * real tokens are checked in the running stack.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestJwtConfig {

    public static final String EDITOR_TOKEN = "Bearer orders-editor";
    public static final String VIEWER_TOKEN = "Bearer orders-viewer";

    @Bean
    JwtDecoder jwtDecoder() {
        return token -> Jwt.withTokenValue(token).header("alg", "none").subject("test-user")
                .claim("realm_access", Map.of("roles", List.of(token))).build();
    }
}
