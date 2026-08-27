package org.janus.modules.identity.application.jwt;

import io.quarkus.security.UnauthorizedException;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.janus.modules.identity.application.user.dto.UserDTO;
import org.janus.modules.identity.domain.entity.UserEntity;
import org.janus.modules.identity.ports.in.jwt.ITokenService;
import org.janus.shared.domain.api.TokenResponse;
import org.janus.shared.domain.exception.InternalServerErrorException;
import org.janus.shared.domain.result.Result;
import org.janus.shared.infrastructure.properties.JwtProperties;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;


@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class TokenService implements ITokenService {

    private final JWTParser parser;
    private final JwtProperties properties;
    private final JsonWebToken jwt;

    @Override
    public Result<String> generateToken(UserEntity user, List<String> roles) {
        if (Boolean.FALSE.equals(user.getIsActive()))
            return Result.forb("User is unactive");

        try {
            JwtClaims claims = createBaseClaims(user);

            Set<String> groups = new HashSet<>(roles);
            groups.add("USER");

            claims.setClaim(Claims.groups.name(), groups);
            claims.setClaim("type", "token");
            claims.setExpirationTimeMinutesInTheFuture(properties.exp().token() * 60);

            String token = signClaims(claims);
            log.info("Token gerado: {}", token);

            return Result.success(token);
        } catch (Exception e) {
            log.error("Erro ao gerar token RSA", e);
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    @Override
    public Result<Map<String, Object>> extractAllClaims(String token) {
        JsonWebToken jwt = parseToken(token);
        Map<String, Object> claims = new HashMap<>();
        for (String name : jwt.getClaimNames()) {
            claims.put(name, jwt.getClaim(name));
        }

        return Result.success(claims);
    }

    @Override
    public Result<String> validateToken(String token) {
        return Result.success(parseToken(token).getSubject());
    }

    @Override
    public Result<TokenResponse> makeTokens(UserEntity user, List<String> roles, UserDTO userDTO) {
        return null;
    }

    private JwtClaims createBaseClaims(UserEntity user) {
        JwtClaims claims = new JwtClaims();
        claims.setSubject(user.getId().toString());
        claims.setClaim(Claims.nickname.name(), user.getUsername());
        claims.setClaim(Claims.email.name(), user.getEmail());
        claims.setIssuedAt(NumericDate.fromSeconds(System.currentTimeMillis() / 1000));
        claims.setGeneratedJwtId();
        return claims;
    }

    private String signClaims(JwtClaims claims) throws Exception {
        PrivateKey pk = readPrivateKey();

        JsonWebSignature jws = new JsonWebSignature();

        jws.setPayload(claims.toJson());
        jws.setKey(pk);
        jws.setHeader("typ", "JWT");
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        return jws.getCompactSerialization();
    }

    private JsonWebToken parseToken(String token) {
        if (token == null || token.isBlank()) throw new UnauthorizedException("Token missing");
        try {
            return parser.parse(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid token: " + e.getMessage());
        }
    }

    private PrivateKey readPrivateKey() throws Exception {
        try (InputStream contentIS = TokenService.class.getResourceAsStream("/privateKey.pem")) {
            if (contentIS == null) throw new RuntimeException("Chave privada não encontrada: " + "/privateKey.pem");
            byte[] tmp = contentIS.readAllBytes();
            String pem = new String(tmp, StandardCharsets.UTF_8)
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replace("\r\n", "")
                    .replace("\n", "")
                    .trim();

            byte[] encodedBytes = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        }
    }

}
