package playerhub.player.client;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente Feign al microservicio comments.
 *
 * Usamos Map<String,Object> como cuerpo y respuesta para no acoplar
 * el dominio Comment entre microservicios: el player no necesita
 * conocer su forma exacta, solo pasarlo de la API al backend.
 */
@FeignClient(name = "comments", url = "${comments.service.url:http://localhost:8082}")
public interface CommentClient {

	@GetMapping("/")
	List<Map<String, Object>> findByPlayerId(@RequestParam("playerId") Long playerId);

	@PostMapping("/")
	Map<String, Object> createComment(@RequestBody Map<String, Object> comment);

	@DeleteMapping("/{id}")
	void deleteComment(@PathVariable("id") Long id);
}
