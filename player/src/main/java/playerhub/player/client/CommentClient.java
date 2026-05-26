package playerhub.player.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente Feign al microservicio comments.
 *
 * Hoy usa URL directa (sub-bloque C). Cuando Eureka esté activo
 * (sub-bloque D) se quita el "url" y Feign + LoadBalancer resuelven
 * el nombre "comments" contra el registry.
 */
@FeignClient(name = "comments", url = "${comments.service.url}")
public interface CommentClient {

	@GetMapping("/")
	List<Object> findByPlayerId(@RequestParam("playerId") Long playerId);
}
