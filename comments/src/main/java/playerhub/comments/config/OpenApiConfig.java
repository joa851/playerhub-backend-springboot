package playerhub.comments.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Configuración de Swagger / OpenAPI para el microservicio comments.
 *
 * - Swagger UI:  /swagger-ui.html
 * - OpenAPI JSON: /v3/api-docs
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "PlayerHub – Comments microservice",
        version = "0.1.0",
        description = "API REST para la gestión de comentarios sobre jugadores "
                    + "(autor, texto, valoración 0-5, geolocalización).",
        contact = @Contact(name = "joa851", email = "joa851@inlumine.ual.es"),
        license = @License(name = "Academic use")
    ),
    servers = {
        @Server(url = "/", description = "Servidor actual")
    }
)
public class OpenApiConfig {
}
