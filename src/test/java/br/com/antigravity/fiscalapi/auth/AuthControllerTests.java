package br.com.antigravity.fiscalapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.com.antigravity.fiscalapi.security.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.security.jwt.enabled=true")
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void issuesAccessTokenUsingIntegrationCredentials() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "dev-client",
                    "password", "dev-password-change-me",
                    "subject", "integration-test",
                    "tenantId", "tenant-test",
                    "merchantId", "merchant-test",
                    "scopes", List.of("fiscal:documents:issue"),
                    "expiresInMinutes", 30
                ))))
            .andReturn()
            .getResponse()
            .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");

        assertThat(data.get("tokenType")).isEqualTo("Bearer");
        String accessToken = data.get("accessToken").toString();
        var principal = jwtTokenService.validate(accessToken);

        assertThat(principal.subject()).isEqualTo("integration-test");
        assertThat(principal.tenantId()).isEqualTo("tenant-test");
        assertThat(principal.merchantId()).isEqualTo("merchant-test");
        assertThat(principal.hasScope("fiscal:documents:issue")).isTrue();
    }

    @Test
    void rejectsInvalidIntegrationCredentials() throws Exception {
        int status = mockMvc.perform(post("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "dev-client",
                    "password", "senha-errada"
                ))))
            .andReturn()
            .getResponse()
            .getStatus();

        assertThat(status).isEqualTo(401);
    }
}
