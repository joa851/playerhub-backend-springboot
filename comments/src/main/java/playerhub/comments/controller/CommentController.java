package playerhub.comments.controller;

import java.util.List;
import java.util.Optional;

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
import playerhub.comments.repository.CommentRepository;
import playerhub.comments.service.CommentService;

@RestController
@Tag(name = "Comments", description = "CRUD de comentarios sobre jugadores")
public class CommentController {
	@Autowired
	CommentRepository commentRepository;

	@Autowired
	CommentService commentService;

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
		if (playerId != null) {
			return ResponseEntity.ok(commentService.findByPlayerId(playerId));
		}
		List<Comment> all = (List<Comment>) commentRepository.findAll();
		return ResponseEntity.ok(all);
	}

	@Operation(summary = "Devuelve un comentario por su id")
	@GetMapping("/{id}")
	public ResponseEntity<Comment> getComment(@PathVariable Long id) {
		Optional<Comment> comment = commentRepository.findById(id);

		if (comment.isPresent()) {
			return ResponseEntity.ok(comment.get());
		}
		return ResponseEntity.notFound().build();
	}

	@Operation(summary = "Crea un comentario (autor, texto, rating 0-5, geolocalización)")
	@PostMapping("/")
	public ResponseEntity<Comment> createComment(@RequestBody Comment comment) {
		comment.setId(null);
		Comment saved = commentRepository.save(comment);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@Operation(summary = "Borra un comentario por id (admin)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
		if (!commentRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		commentRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
