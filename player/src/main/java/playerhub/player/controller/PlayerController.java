package playerhub.player.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import playerhub.player.domain.Player;
import playerhub.player.repository.PlayerRepository;
import playerhub.player.service.PlayerService;

@RestController
@Tag(name = "Players", description = "CRUD local + búsqueda/import API-Football + LLM + comments")
public class PlayerController {
	@Autowired
	PlayerRepository playerRepository;

	@Autowired
	PlayerService playerService;

	// Inyectado desde el Config Server (player.properties en el repo de configs).
	@Value("${app.welcome-message:welcome (fallback)}")
	private String welcomeMessage;

	@Operation(summary = "Mensaje de bienvenida (servido por el Config Server)")
	@GetMapping("/welcome")
	public ResponseEntity<String> welcome() {
		return ResponseEntity.ok(welcomeMessage);
	}

	// CRUD local

	// Postgres no acepta cast cuando el param es null, así que se mandan fechas lejanas.
	private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

	@Operation(summary = "Lista jugadores locales con filtros opcionales (nombre, equipo, liga, fechas)")
	@GetMapping("/")
	public ResponseEntity<List<Player>> getPlayers(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String team,
			@RequestParam(required = false) String league,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
		Instant fromOrEpoch = from != null ? from : Instant.EPOCH;
		Instant toOrFuture = to != null ? to : FAR_FUTURE;
		List<Player> players = playerRepository.search(name, team, league, fromOrEpoch, toOrFuture);
		return ResponseEntity.ok(players);
	}

	@Operation(summary = "Devuelve un jugador por su id local, con sus comments embebidos (vía Feign)")
	@GetMapping("/{id}")
	public ResponseEntity<Player> getPlayer(@PathVariable Long id) {
		Optional<Player> player = playerService.findByIdWithComments(id);

		if (player.isPresent()) {
			return ResponseEntity.ok(player.get());
		}
		return ResponseEntity.notFound().build();
	}

	@Operation(summary = "Crea un jugador desde formulario (con geolocalización)")
	@PostMapping("/")
	public ResponseEntity<Player> createPlayer(@RequestBody Player player) {
		player.setId(null);
		Player saved = playerRepository.save(player);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@Operation(summary = "Actualiza un jugador existente (admin)")
	@PutMapping("/{id}")
	public ResponseEntity<Player> updatePlayer(@PathVariable Long id, @RequestBody Player player) {
		if (!playerRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		player.setId(id);
		Player saved = playerRepository.save(player);
		return ResponseEntity.ok(saved);
	}

	@Operation(summary = "Borra un jugador por id local (admin)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
		if (!playerRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		playerRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	//Crud API

	@Operation(summary = "Busca jugadores en API-Football (no toca la BD local)")
	@GetMapping("/external")
	public ResponseEntity<Player[]> searchExternal(@RequestParam String query) {
		return ResponseEntity.ok(playerService.searchExternal(query));
	}

	@Operation(summary = "Importa a la BD local los jugadores seleccionados (por id externo)")
	@PostMapping("/external/import")
	public ResponseEntity<List<Player>> importExternal(@RequestBody List<Long> ids) {
		List<Player> imported = playerService.importExternal(ids);
		return ResponseEntity.status(HttpStatus.CREATED).body(imported);
	}

	@Operation(summary = "Genera el 'Equipo Ideal' usando Gemini sobre los jugadores de la BD")
	@PostMapping("/ideal-team")
	public ResponseEntity<List<Player>> idealTeam() {
		return ResponseEntity.ok(playerService.idealTeam());
	}

	@Operation(summary = "Devuelve los comentarios de un jugador (vía Feign al microservicio comments)")
	@GetMapping("/{id}/comments")
	public ResponseEntity<List<Map<String, Object>>> getComments(@PathVariable Long id) {
		List<Map<String, Object>> comments = playerService.getComments(id);
		if (comments == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(comments);
	}

	@Operation(summary = "Añade un comentario a un jugador (proxy vía Feign al microservicio comments)")
	@PostMapping("/{id}/comments")
	public ResponseEntity<Map<String, Object>> addComment(
			@PathVariable Long id,
			@RequestBody Map<String, Object> comment) {
		Map<String, Object> created = playerService.addComment(id, comment);
		if (created == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Operation(summary = "Borra un comentario por id (admin, proxy vía Feign al microservicio comments)")
	@DeleteMapping("/{id}/comments/{commentId}")
	public ResponseEntity<Void> deleteComment(
			@PathVariable Long id,
			@PathVariable Long commentId) {
		boolean ok = playerService.deleteComment(id, commentId);
		if (!ok) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.noContent().build();
	}
}
