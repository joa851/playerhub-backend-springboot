package playerhub.player.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import playerhub.player.client.CommentClient;
import playerhub.player.domain.ApiResponse;
import playerhub.player.domain.Player;
import playerhub.player.domain.PlayerWrapper;
import playerhub.player.repository.PlayerRepository;

@Service
public class PlayerService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CommentClient commentClient;

    @Value("${api.football.base-url}")
    private String apiUrl;

    @Value("${api.football.key}")
    private String apiKey;

    /**
     * Busca jugadores en API-Football por nombre. No toca la BD.
     */
    public Player[] searchExternal(String query) {
        String url = apiUrl + "?search=" + query;
        ResponseEntity<ApiResponse> response = callApiFootball(url);

        ApiResponse body = response.getBody();
        if (body == null || body.getResponse() == null) {
            return new Player[0];
        }
        return body.getResponse().stream()
            .map(PlayerWrapper::getPlayer)
            .toArray(Player[]::new);
    }

    /**
     * Para cada id de API-Football, baja el jugador y lo guarda en la BD local.
     * Salta los que ya estén importados (mismo externalId).
     * Devuelve los jugadores creados.
     */
    public List<Player> importExternal(List<Long> ids) {
        List<Player> imported = new ArrayList<>();
        for (Long extId : ids) {
            if (playerRepository.findByExternalId(extId).isPresent()) {
                continue;
            }
            ResponseEntity<ApiResponse> response = callApiFootball(apiUrl + "?id=" + extId);
            ApiResponse body = response.getBody();
            if (body == null || body.getResponse() == null || body.getResponse().isEmpty()) {
                continue;
            }
            Player p = body.getResponse().get(0).getPlayer();
            // El id que llega del JSON es el de API-Football → va a externalId,
            // y dejamos que JPA autogenere el id local.
            p.setExternalId(p.getId());
            p.setId(null);
            imported.add(playerRepository.save(p));
        }
        return imported;
    }

    /**
     * Genera un "Equipo Ideal" con LLM (Groq / Google AI). Placeholder por ahora.
     */
    public List<Player> idealTeam() {
        // TODO: integrar LLM (Groq / Google AI Studio).
        return new ArrayList<>();
    }

    /**
     * Devuelve los comentarios de un jugador llamando al microservicio
     * comments vía Feign. Si el player no existe localmente, devuelve null
     * (el controller lo traducirá a 404).
     */
    public List<Object> getComments(Long playerId) {
        Optional<Player> player = playerRepository.findById(playerId);
        if (player.isEmpty()) {
            return null;
        }
        return commentClient.findByPlayerId(playerId);
    }

    private ResponseEntity<ApiResponse> callApiFootball(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, ApiResponse.class);
    }
}
