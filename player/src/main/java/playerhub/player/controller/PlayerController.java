package playerhub.player.controller;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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

import playerhub.player.domain.Player;
import playerhub.player.repository.PlayerRepository;
import playerhub.player.service.PlayerService;

@RestController
public class PlayerController {
	@Autowired
	PlayerRepository playerRepository;

	@Autowired
	PlayerService playerService;

	// CRUD local

	// Postgres no acepta cast bytea→timestamptz cuando el param es null, así
	// que aquí mandamos sentinels: EPOCH (1970) y una fecha lejana.
	private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

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

	@GetMapping("/{id}")
	public ResponseEntity<Player> getPlayer(@PathVariable Long id) {
		Optional<Player> player = playerRepository.findById(id);

		if (player.isPresent()) {
			return ResponseEntity.ok(player.get());
		}
		return ResponseEntity.notFound().build();
	}

	@PostMapping("/")
	public ResponseEntity<Player> createPlayer(@RequestBody Player player) {
		player.setId(null);
		Player saved = playerRepository.save(player);
		return ResponseEntity.status(HttpStatus.CREATED).body(saved);
	}

	@PutMapping("/{id}")
	public ResponseEntity<Player> updatePlayer(@PathVariable Long id, @RequestBody Player player) {
		if (!playerRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		player.setId(id);
		Player saved = playerRepository.save(player);
		return ResponseEntity.ok(saved);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
		if (!playerRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		playerRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	//Crud API

	@GetMapping("/external")
	public ResponseEntity<Player[]> searchExternal(@RequestParam String query) {
		return ResponseEntity.ok(playerService.searchExternal(query));
	}

	@PostMapping("/external/import")
	public ResponseEntity<List<Player>> importExternal(@RequestBody List<Long> ids) {
		List<Player> imported = playerService.importExternal(ids);
		return ResponseEntity.status(HttpStatus.CREATED).body(imported);
	}

	//TODO

	@PostMapping("/ideal-team")
	public ResponseEntity<List<Player>> idealTeam() {
		return ResponseEntity.ok(playerService.idealTeam());
	}
	//TODO
	@GetMapping("/{id}/comments")
	public ResponseEntity<List<Object>> getComments(@PathVariable Long id) {
		List<Object> comments = playerService.getComments(id);
		if (comments == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(comments);
	}
}
