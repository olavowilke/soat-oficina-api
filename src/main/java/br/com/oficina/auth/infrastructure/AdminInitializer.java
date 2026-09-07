package br.com.oficina.auth.infrastructure;

import br.com.oficina.auth.entities.Role;
import br.com.oficina.auth.entities.Usuario;
import br.com.oficina.auth.gateways.UsuarioGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {

    private final UsuarioGateway usuarioGateway;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(UsuarioGateway usuarioGateway,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.admin.username:admin}") String adminUsername,
                            @Value("${app.admin.password:admin123}") String adminPassword) {
        this.usuarioGateway = usuarioGateway;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!usuarioGateway.existsByUsername(adminUsername)) {
            Usuario admin = Usuario.novo(adminUsername, passwordEncoder.encode(adminPassword), Role.ADMIN);
            usuarioGateway.save(admin);
        }
    }
}
