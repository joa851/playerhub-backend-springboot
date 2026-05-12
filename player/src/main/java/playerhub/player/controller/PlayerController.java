package playerhub.player.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@GetMapping("/")
	public ResponseEntity<Player[]> getPlayers() {
		Player[] players = playerService.getAllPlayers();
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
}
