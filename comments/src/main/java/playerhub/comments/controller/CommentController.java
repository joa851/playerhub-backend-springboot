package playerhub.comments.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import playerhub.comments.domain.Comment;
import playerhub.comments.service.CommentService;

/**
 * Controller del microservicio comments. Toda la lógica pasa por
 * CommentService — el controller se limita a traducir HTTP ↔ service.
 */
@RestController
@Tag(name = "Comments", description = "CRUD de comentarios sobre jugadores")
public class CommentController {

	@Autowired
	private CommentService commentService;

	// Inyectado desde el Config Server (comments.properties en el repo de configs).
	@Value("${app.welcome-message:welcome (fallback)}")
	private String welcomeMessage;

	@Operation(summary = "Mensaje de bienvenida (servido por el Config Server)")
	@GetMapping("/welcome")
	public ResponseEntity<String> welcome() {
		return ResponseEntity.ok(welcomeMessage);
	}

	@Operation(summary = "Lista comentarios. Filtro opcional por playerId.")
	@GetMapping("/")
	public ResponseEntity<List<Comment>> getComments(@RequestParam(required = false) Long playerId) {
		List<Comment> result = playerId != null
			? commentService.findByPlayerId(playerId)
			: commentService.findAll();
		return ResponseEntity.ok(result);
	}

	@Operation(summary = "Devuelve un comentario por su id")
	@GetMapping("/{id}")
	public ResponseEntity<Comment> getComment(@PathVariable Long id) {
		return commentService.findById(id)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Operation(summary = "Crea un comentario (autor, texto, rating 0-5, geolocalización)")
	@PostMapping("/")
	public ResponseEntity<Comment> createComment(@RequestBody Comment comment) {
		Comment saved = commentService.create(comment);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@Operation(summary = "Borra un comentario por id (admin)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
		if (!commentService.delete(id)) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.noContent().build();
	}
}
