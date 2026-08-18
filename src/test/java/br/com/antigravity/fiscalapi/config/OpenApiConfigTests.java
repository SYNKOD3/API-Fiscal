package br.com.antigravity.fiscalapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "app.open-api.public-access=true"
})
@AutoConfigureMockMvc
class OpenApiConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void exposesTokenEndpointWithoutSecurityAndProtectedEndpointsWithBasicSecurity() throws Exception {
        String response = mockMvc.perform(get("/v3/api-docs"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        @SuppressWarnings("unchecked")
        Map<String, Object> spec = objectMapper.readValue(response, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> securitySchemes = (Map<String, Object>) components.get("securitySchemes");
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenPath = (Map<String, Object>) paths.get("/api/v1/auth/token");
        @SuppressWarnings("unchecked")
        Map<String, Object> companiesPath = (Map<String, Object>) paths.get("/api/v1/companies");
        @SuppressWarnings("unchecked")
        Map<String, Object> tokenPost = (Map<String, Object>) tokenPath.get("post");
        @SuppressWarnings("unchecked")
        Map<String, Object> companiesGet = (Map<String, Object>) companiesPath.get("get");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> security = (List<Map<String, Object>>) companiesGet.get("security");

        assertThat(securitySchemes).containsOnlyKeys("Login e senha");
        assertThat((List<?>) tokenPost.get("security")).isEmpty();
        assertThat(security).isNotEmpty();
        assertThat(security.getFirst()).containsKey("Login e senha");
        assertThat(security.getFirst()).doesNotContainKeys("Chave da API", "JWT Integrador");
    }
}
