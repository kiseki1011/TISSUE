package com.tissue.security.config;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Authentication mode for this instance.
 *
 * <p>An instance runs in exactly one mode: {@code LOCAL} (Tissue's own username/email + password login)
 * or {@code OIDC} (login is delegated to an external IdP). The two should never coexist. In {@code OIDC} mode the
 * IdP only authenticates the user. Tissue still issues and validates its own JWT tokens.
 */
@Data
@Component
@ConfigurationProperties(prefix = "tissue.auth")
public class TissueAuthProperties {

    private Mode mode = Mode.LOCAL;
    private Oidc oidc = new Oidc();

    public enum Mode {
        LOCAL,
        OIDC
    }

    @Data
    public static class Oidc {

        /**
         * The IdP issuer URL. OIDC discovery ({@code issuer + /.well-known/openid-configuration})
         * resolves the device/token/jwks endpoints.
         */
        private String issuerUri = "";

        private String clientId = "";

        private String clientSecret = "";

        private List<String> scopes = List.of("openid", "profile", "email");

        private String usernameClaim = "preferred_username";

        private String emailClaim = "email";

        private String nameClaim = "name";

        /**
         * If non-empty, only users whose email domain is in this list may be provisioned.
         */
        private List<String> allowedEmailDomains = List.of();

        /**
         * Whether to auto-create a Member on first successful OIDC login (JIT provisioning).
         */
        private boolean autoProvision = true;
    }
}
