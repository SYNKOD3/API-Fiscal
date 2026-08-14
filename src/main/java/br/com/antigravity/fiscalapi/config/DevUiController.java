package br.com.antigravity.fiscalapi.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnProperty(prefix = "app.dev-console", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DevUiController {

    @GetMapping("/dev")
    public String devUi() {
        return "redirect:/dev/index.html";
    }
}
