package br.com.antigravity.fiscalapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "app.security.basic-auth-enabled=true",
    "app.security.api-key-enabled=false",
    "app.security.jwt.enabled=false"
})
@AutoConfigureMockMvc
class BasicAuthFilterTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsProtectedEndpointWithoutLoginAndPassword() throws Exception {
        int status = mockMvc.perform(get("/api/v1/companies"))
            .andReturn()
            .getResponse()
            .getStatus();

        assertThat(status).isEqualTo(401);
    }

    @Test
    void acceptsProtectedEndpointWithLoginAndPassword() throws Exception {
        int status = mockMvc.perform(get("/api/v1/companies")
                .header("Authorization", basic("dev-client", "dev-password-change-me")))
            .andReturn()
            .getResponse()
            .getStatus();

        assertThat(status).isEqualTo(200);
    }

    private String basic(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
