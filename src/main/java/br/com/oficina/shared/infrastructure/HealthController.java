package br.com.oficina.shared.infrastructure;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Status da aplicação")
public class HealthController {

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Health check", description = "Verifica se a aplicação está no ar")
    @ApiResponse(responseCode = "200", description = "Aplicação UP")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "oficina-api",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
