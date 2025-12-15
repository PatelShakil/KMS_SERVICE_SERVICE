package com.shakilpatel.service.service.config;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import org.eclipse.microprofile.auth.LoginConfig;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

import java.io.InputStream;

@ApplicationPath("/api")
@LoginConfig(authMethod = "MP-JWT", realmName = "boloindia-realm")
@DeclareRoles({"PLATFORM_ADMIN", "ORG_OWNER", "ORG_ADMIN"})
@SecurityScheme(
        securitySchemeName = "jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@OpenAPIDefinition(
        info = @Info(
                title = "BoloIndia Service Service API",
                version = "1.0",
                description = "Service Management Service",
                contact = @Contact(
                        name = "MuhammadShakil Patel",
                        email = "shakil@boloindia.in"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8087/service-service", description = "Development")
        },
        security = {
                @SecurityRequirement(name = "jwt")
        }
)
public class BootStrap extends jakarta.ws.rs.core.Application {

    public BootStrap() {
        // Verify public key exists
        InputStream stream = getClass().getResourceAsStream("/keys/publicKey.pem");
        System.out.println("========================================");
        System.out.println("PUBLIC KEY VERIFICATION");
        System.out.println("========================================");
        System.out.println("PUBLIC KEY FOUND: " + (stream != null));

        if (stream != null) {
            try {
                byte[] keyBytes = stream.readAllBytes();
                System.out.println("PUBLIC KEY SIZE: " + keyBytes.length + " bytes");
                String keyContent = new String(keyBytes);
                System.out.println("PUBLIC KEY PREVIEW:");
                System.out.println(keyContent.substring(0, Math.min(100, keyContent.length())));
                System.out.println("...");
                stream.close();
            } catch (Exception e) {
                System.err.println("ERROR READING PUBLIC KEY: " + e.getMessage());
            }
        } else {
            System.err.println("❌ PUBLIC KEY NOT FOUND AT: /keys/publicKey.pem");
        }
        System.out.println("========================================");
    }
}
