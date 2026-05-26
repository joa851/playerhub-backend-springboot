package playerhub.player.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cliente Feign al microservicio comments.
 */
@FeignClient(name = "comments", url = "${comments.service.url:http://localhost:8082}")
public interface CommentClient {

	@GetMapping("/")
	List<Object> findByPlayerId(@RequestParam("playerId") Long playerId);
}
