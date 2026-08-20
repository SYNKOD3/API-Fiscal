package br.com.antigravity.fiscalapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.com.antigravity.fiscalapi.company.CompanyResponse;
import br.com.antigravity.fiscalapi.company.CompanyService;
import br.com.antigravity.fiscalapi.company.CreateCompanyRequest;
import br.com.antigravity.fiscalapi.company.FiscalEnvironment;
import br.com.antigravity.fiscalapi.company.TaxRegime;
import br.com.antigravity.fiscalapi.document.DocumentModel;
import br.com.antigravity.fiscalapi.document.FiscalItemRequest;
import br.com.antigravity.fiscalapi.document.IssueDocumentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
    "debug=false",
    "logging.level.root=WARN",
    "logging.level.org.springframework=WARN",
    "logging.level.br.com.antigravity.fiscalapi=WARN"
})
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "performanceTests", matches = "true")
class PerformanceSmokeTests {

    private static final String API_KEY = "dev-api-key-change-me-123456";
    private static final int WARMUP_RUNS = Integer.getInteger("performanceWarmup", 5);
    private static final int READ_RUNS = Integer.getInteger("performanceReadRuns", 50);
    private static final int WRITE_RUNS = Integer.getInteger("performanceWriteRuns", 20);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyService companyService;

    @Test
    void measuresMainEndpointResponseTimes() throws Exception {
        CompanyResponse company = companyService.create(companyRequest());

        List<BenchmarkResult> results = List.of(
            measure("GET /actuator/health", READ_RUNS, () -> get("/actuator/health")),
            measure("GET /api/v1/companies", READ_RUNS, () -> get("/api/v1/companies").header("X-API-Key", API_KEY)),
            measure("GET /api/v1/documents", READ_RUNS, () -> get("/api/v1/documents")
                .header("X-API-Key", API_KEY)
                .queryParam("companyId", company.id().toString())),
            measure("POST /api/v1/documents", WRITE_RUNS, () -> post("/api/v1/documents")
                .header("X-API-Key", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(documentRequest(company))))
        );

        System.out.println();
        System.out.println("Benchmark de tempo de resposta da Fiscal API");
        System.out.println("Ambiente: MockMvc local, H2 em memoria, provider fiscal STUB");
        System.out.println("Endpoint                  Runs Status      Min    Avg    P50    P95    P99    Max");
        for (BenchmarkResult result : results) {
            System.out.printf(
                "%-25s %4d %-10s %6.2f %6.2f %6.2f %6.2f %6.2f %6.2f ms%n",
                result.endpoint(),
                result.runs(),
                result.statusSummary(),
                result.minMs(),
                result.avgMs(),
                result.p50Ms(),
                result.p95Ms(),
                result.p99Ms(),
                result.maxMs()
            );
        }
        System.out.println();
    }

    private BenchmarkResult measure(String endpoint, int runs, RequestFactory requestFactory) throws Exception {
        for (int index = 0; index < WARMUP_RUNS; index++) {
            mockMvc.perform(requestFactory.create());
        }

        List<Double> times = new ArrayList<>();
        Map<Integer, Integer> statuses = new java.util.TreeMap<>();
        for (int index = 0; index < runs; index++) {
            long startedAt = System.nanoTime();
            int status = mockMvc.perform(requestFactory.create()).andReturn().getResponse().getStatus();
            long elapsedNs = System.nanoTime() - startedAt;
            times.add(elapsedNs / 1_000_000.0);
            statuses.merge(status, 1, Integer::sum);
        }

        times.sort(Comparator.naturalOrder());
        double avg = times.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return new BenchmarkResult(
            endpoint,
            runs,
            summarizeStatuses(statuses),
            times.getFirst(),
            avg,
            percentile(times, 0.50),
            percentile(times, 0.95),
            percentile(times, 0.99),
            times.getLast()
        );
    }

    private String summarizeStatuses(Map<Integer, Integer> statuses) {
        List<String> parts = statuses.entrySet()
            .stream()
            .map(entry -> entry.getKey() + ":" + entry.getValue())
            .toList();
        return String.join(",", parts);
    }

    private double percentile(List<Double> sorted, double percentile) {
        int index = (int) Math.floor((sorted.size() - 1) * percentile);
        return sorted.get(index);
    }

    private CreateCompanyRequest companyRequest() {
        String suffix = Long.toString(System.nanoTime());
        String taxId = ("123456" + suffix).substring(0, 14);
        return new CreateCompanyRequest(
            "tenant-perf",
            "merchant-" + suffix,
            "https://integrator.example/webhooks/fiscal",
            "Empresa Performance LTDA",
            taxId,
            "123456789",
            "BA",
            "Empresa Performance",
            "Rua Fiscal",
            "100",
            null,
            "Centro",
            "2927408",
            "Salvador",
            "40000000",
            "7133334444",
            TaxRegime.SIMPLES_NACIONAL,
            FiscalEnvironment.HOMOLOGATION,
            null,
            null,
            "000001",
            "token-csc",
            null,
            null,
            3,
            10L,
            7,
            200L
        );
    }

    private IssueDocumentRequest documentRequest(CompanyResponse company) {
        return new IssueDocumentRequest(
            company.id(),
            null,
            null,
            DocumentModel.NFCE,
            "PERF-" + System.nanoTime(),
            "Cliente Performance",
            BigDecimal.valueOf(19.90),
            List.of(new FiscalItemRequest(
                "SKU-PERF",
                "Produto Performance",
                "01012100",
                null,
                "SEM GTIN",
                "5102",
                "UN",
                BigDecimal.ONE,
                BigDecimal.valueOf(19.90),
                BigDecimal.valueOf(19.90),
                "0",
                "102",
                "49",
                "49",
                BigDecimal.ZERO
            ))
        );
    }

    private record BenchmarkResult(
        String endpoint,
        int runs,
        String statusSummary,
        double minMs,
        double avgMs,
        double p50Ms,
        double p95Ms,
        double p99Ms,
        double maxMs
    ) {
    }

    @FunctionalInterface
    private interface RequestFactory {
        MockHttpServletRequestBuilder create() throws Exception;
    }
}
