package br.com.antigravity.fiscalapi.sefaz;

import br.com.antigravity.fiscalapi.company.Company;
import br.com.antigravity.fiscalapi.config.AppProperties;
import br.com.antigravity.fiscalapi.document.DocumentModel;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SefazRouter {

    private static final Set<String> SUPPORTED_STATES = Set.of(
        "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
        "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
        "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private final AppProperties properties;

    public SefazRouter(AppProperties properties) {
        this.properties = properties;
    }

    public SefazRouteResponse route(Company company, DocumentModel model) {
        String stateCode = normalize(company.getStateCode());
        boolean supported = SUPPORTED_STATES.contains(stateCode);
        boolean available = supported && !isUnavailable(stateCode);
        String message = supported
            ? "Rota definida pela UF da empresa emitente. A Java-NFe resolve o endpoint estadual ou autorizador compartilhado configurado para a UF."
            : "UF nao suportada pela tabela nacional de estados.";

        return new SefazRouteResponse(
            company.getId(),
            company.getTenantId(),
            company.getMerchantId(),
            company.getTaxId(),
            stateCode,
            model,
            company.getFiscalEnvironment(),
            "JAVA_NFE_BY_EMITTER_UF",
            contingency(model),
            available,
            message
        );
    }

    public boolean isAvailable(String stateCode) {
        String normalized = normalize(stateCode);
        return SUPPORTED_STATES.contains(normalized) && !isUnavailable(normalized);
    }

    public List<SefazStateResponse> states() {
        return SUPPORTED_STATES.stream()
            .sorted()
            .map(state -> new SefazStateResponse(
                state,
                "JAVA_NFE_BY_EMITTER_UF",
                isUnavailable(state)
                    ? "UF marcada como indisponivel por configuracao operacional."
                    : "UF habilitada; endpoint/autorizador resolvido pela Java-NFe."
            ))
            .toList();
    }

    private SefazContingencyStrategy contingency(DocumentModel model) {
        return model == DocumentModel.NFCE
            ? SefazContingencyStrategy.NFCE_OFFLINE
            : SefazContingencyStrategy.NFE_SVC_OR_EPEC;
    }

    private boolean isUnavailable(String stateCode) {
        return properties.getFiscal().getUnavailableStates().stream()
            .map(this::normalize)
            .anyMatch(stateCode::equals);
    }

    private String normalize(String stateCode) {
        return stateCode == null ? "" : stateCode.trim().toUpperCase();
    }
}
