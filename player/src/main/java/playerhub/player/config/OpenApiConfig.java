package playerhub.player.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Configuración de Swagger / OpenAPI para el microservicio player.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "PlayerHub – Player microservice",
        version = "0.1.0",
        description = "API REST para la gestión de jugadores de fútbol.",
        contact = @Contact(name = "joa851", email = "joa851@inlumine.ual.es"),
        license = @License(name = "Academic use")
    ),
    servers = {
        @Server(url = "/", description = "Servidor actual")
    }
)
public class OpenApiConfig {
}
