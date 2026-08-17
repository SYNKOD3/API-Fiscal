package br.com.antigravity.fiscalapi.security;

import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.shared.ForbiddenException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class JwtSecurityContext {

    public Optional<JwtPrincipal> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public void requireTenantAccess(String tenantId, String merchantId) {
        current().ifPresent(principal -> {
            if (principal.scopes().contains("fiscal:admin")) {
                return;
            }
            if (hasText(principal.tenantId()) && !principal.tenantId().equals(tenantId)) {
                throw new ForbiddenException("JWT não permite acesso ao tenant informado");
            }
            if (hasText(principal.merchantId()) && !principal.merchantId().equals(merchantId)) {
                throw new ForbiddenException("JWT não permite acesso ao merchant informado");
            }
        });
    }

    public void requireCompanyAccess(Company company) {
        current().ifPresent(principal -> {
            if (principal.scopes().contains("fiscal:admin")) {
                return;
            }
            if (hasText(principal.tenantId())
                && !principal.tenantId().equals(company.getTenantId())) {
                throw new ForbiddenException("JWT nao permite acesso a empresa emissora informada");
            }
            if (hasText(principal.merchantId())
                && !principal.merchantId().equals(company.getMerchantId())) {
                throw new ForbiddenException("JWT nao permite acesso a empresa emissora informada");
            }
        });
    }

    public String constrainedTenant(String requestedTenant) {
        return current()
            .filter(principal -> !principal.scopes().contains("fiscal:admin"))
            .map(JwtPrincipal::tenantId)
            .filter(this::hasText)
            .map(tokenTenant -> {
                if (hasText(requestedTenant) && !tokenTenant.equals(requestedTenant)) {
                    throw new ForbiddenException("JWT não permite consultar outro tenant");
                }
                return tokenTenant;
            })
            .orElse(requestedTenant);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
