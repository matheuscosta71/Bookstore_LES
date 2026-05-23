package com.matheusgn.ecommerce;

import com.matheusgn.ecommerce.ai.config.AiProperties;
import com.matheusgn.ecommerce.config.CartItemProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties({CartItemProperties.class, AiProperties.class})
public class EcommerceApplication {

    // #region agent log
    private static final Path DEBUG_LOG =
            Path.of("/home/dell/Documentos/Trabalhos-clientes/Clientes/Matheus-GN/matheus-gn/.cursor/debug-9dce4d.log");

    private static void appendStartupFailureNdjson(Throwable failure) {
        boolean bind = false;
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof BindException) {
                bind = true;
                break;
            }
        }
        try {
            long ts = System.currentTimeMillis();
            String msg = failure.getMessage() != null ? failure.getMessage().replace("\"", "'") : "";
            String line = String.format(
                    "{\"sessionId\":\"9dce4d\",\"hypothesisId\":\"H1\",\"location\":\"EcommerceApplication.main\","
                            + "\"message\":\"ApplicationFailedEvent\","
                            + "\"data\":{\"bindException\":%s,\"rootClass\":\"%s\",\"detail\":\"%s\"},"
                            + "\"timestamp\":%d}%n",
                    bind,
                    failure.getClass().getName().replace("\"", "'"),
                    msg,
                    ts);
            Files.writeString(DEBUG_LOG, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // debug ingest must not affect startup
        }
    }
    // #endregion

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EcommerceApplication.class);
        // #region agent log
        app.addListeners((ApplicationListener<ApplicationFailedEvent>) event -> appendStartupFailureNdjson(event.getException()));
        // #endregion
        app.run(args);
    }
}
