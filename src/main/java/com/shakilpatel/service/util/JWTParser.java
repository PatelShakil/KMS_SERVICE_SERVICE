package com.shakilpatel.service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

public class JWTParser {

    private static final Logger LOGGER = Logger.getLogger(JWTParser.class.getName());
    private static PublicKey publicKey;

    static {
        try {
            loadPublicKey();
        } catch (Exception e) {
            LOGGER.severe("❌ CRITICAL: Failed to load public key!");
            e.printStackTrace();
            throw new RuntimeException("Cannot start service without public key", e);
        }
    }

    private static void loadPublicKey() throws Exception {
        LOGGER.info("Loading public key from /keys/publicKey.pem...");

        InputStream keyStream = JWTParser.class.getResourceAsStream("/keys/publicKey.pem");

        if (keyStream == null) {
            throw new IllegalStateException("❌ Public key not found at /keys/publicKey.pem");
        }

        String keyContent = new String(keyStream.readAllBytes())
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        LOGGER.info("Public key content length: " + keyContent.length() + " chars");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        publicKey = keyFactory.generatePublic(spec);

        LOGGER.info("✅ Public key loaded successfully!");
        LOGGER.info("Algorithm: " + publicKey.getAlgorithm());
        LOGGER.info("Format: " + publicKey.getFormat());

        keyStream.close();
    }

    public static Claims parseToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                LOGGER.warning("❌ Token is null or empty");
                return null;
            }

            LOGGER.info("Parsing JWT token (length: " + token.length() + ")...");

            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            LOGGER.info("✅ Token parsed successfully!");
            LOGGER.info("Subject: " + claims.getSubject());
            LOGGER.info("Issuer: " + claims.getIssuer());
            LOGGER.info("Audience: " + claims.getAudience());
            LOGGER.info("Expiration: " + claims.getExpiration());

            return claims;

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            LOGGER.warning("❌ Token expired: " + e.getMessage());
            return null;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            LOGGER.severe("❌ Invalid signature: " + e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.severe("❌ Failed to parse token: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static Long getUserId(Claims claims) {
        if (claims == null) return null;
        String subject = claims.getSubject();
        return subject != null ? Long.parseLong(subject) : null;
    }

    public static Long getOrgId(Claims claims) {
        if (claims == null) return null;
        Object orgId = claims.get("orgId");

        if (orgId instanceof Number) {
            return ((Number) orgId).longValue();
        } else if (orgId instanceof String) {
            return Long.parseLong((String) orgId);
        }
        return null;
    }

    public static String getEmail(Claims claims) {
        if (claims == null) return null;
        return claims.get("email", String.class);
    }

    public static String getName(Claims claims) {
        if (claims == null) return null;
        return claims.get("name", String.class);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getRoles(Claims claims) {
        if (claims == null) return null;
        return claims.get("roles", List.class);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getGroups(Claims claims) {
        if (claims == null) return null;
        return claims.get("groups", List.class);
    }

    public static boolean hasRole(Claims claims, String role) {
        List<String> roles = getRoles(claims);
        return roles != null && roles.contains(role);
    }

    public static boolean hasAnyRole(Claims claims, String... roles) {
        List<String> userRoles = getRoles(claims);
        if (userRoles == null) return false;

        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPlatformAdmin(Claims claims) {
        return hasRole(claims, "PLATFORM_ADMIN");
    }

    public static boolean isOrgOwner(Claims claims) {
        return hasRole(claims, "ORG_OWNER");
    }

    public static boolean isOrgAdmin(Claims claims) {
        return hasRole(claims, "ORG_ADMIN");
    }
}
