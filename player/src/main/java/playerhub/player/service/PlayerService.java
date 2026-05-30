package playerhub.player.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Postgres no acepta cast cuando el param es null, así que se mandan
     *  fechas extremas como sentinela en lugar de null. */
    private static final Instant FAR_FUTURE = Instant.parse("9999-12-31T23:59:59Z");

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private CommentClient commentClient;

    @Autowired
    private LLMService llmService;

    @Value("${api.football.base-url}")
    private String apiUrl;

    @Value("${api.football.key}")
    private String apiKey;

    // ─── CRUD local ────────────────────────────────────────────────────

    /**
     * Lista jugadores aplicando los filtros opcionales recibidos. Si from
     * o to son null, los sustituye por EPOCH / FAR_FUTURE para que el
     * query SQL nunca reciba null en parámetros de fecha.
     */
    public List<Player> search(String name, String team, String league, Instant from, Instant to) {
        Instant fromOrEpoch = from != null ? from : Instant.EPOCH;
        Instant toOrFuture = to != null ? to : FAR_FUTURE;
        return playerRepository.search(name, team, league, fromOrEpoch, toOrFuture);
    }

    /** Crea un jugador nuevo. Garantiza id=null para que JPA autogenere. */
    public Player create(Player player) {
        player.setId(null);
        return playerRepository.save(player);
    }

    /**
     * Actualiza un jugador existente. Devuelve Optional.empty si el id
     * no existe (el controller lo traducirá a 404).
     */
    public Optional<Player> update(Long id, Player player) {
        if (!playerRepository.existsById(id)) {
            return Optional.empty();
        }
        player.setId(id);
        return Optional.of(playerRepository.save(player));
    }

    /**
     * Borra un jugador por id. Devuelve true si existía y se borró,
     * false si no existía (404 en el controller).
     */
    public boolean delete(Long id) {
        if (!playerRepository.existsById(id)) {
            return false;
        }
        playerRepository.deleteById(id);
        return true;
    }

    // ─── API-Football ──────────────────────────────────────────────────

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
            // API-Football usa el query param `player` para id lookup, no `id`.
            ResponseEntity<ApiResponse> response = callApiFootball(apiUrl + "?player=" + extId);
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
     * Genera un "Equipo Ideal" con Gemini.
     * Pasa todos los jugadores de la BD local al LLM, que devuelve los IDs
     * seleccionados. Luego recuperamos las entidades para devolverlas
     * completas al cliente.
     */
    public List<Player> idealTeam() {
        List<Player> all = (List<Player>) playerRepository.findAll();
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> selectedIds = llmService.selectIdealTeamIds(all);
        List<Player> team = new ArrayList<>();
        for (Long id : selectedIds) {
            playerRepository.findById(id).ifPresent(team::add);
        }
        return team;
    }

    /**
     * Devuelve los comentarios de un jugador llamando al microservicio
     * comments vía Feign. Si el player no existe localmente, devuelve null
     * (el controller lo traducirá a 404).
     *
     * Antes de devolverlos, renombra el campo "id" (Long) a "_id" (String)
     * para que el FE no tenga que conocer la diferencia con MEAN, que ya
     * usa _id de Mongo.
     */
    public List<Map<String, Object>> getComments(Long playerId) {
        Optional<Player> player = playerRepository.findById(playerId);
        if (player.isEmpty()) {
            return null;
        }
        return normalizeIds(commentClient.findByPlayerId(playerId));
    }

    /**
     * findById + carga de comments en una sola llamada para el controller.
     * Si el player existe, su lista `comments` queda poblada (o vacía
     * si no tiene). Si no existe, devuelve Optional.empty.
     */
    public Optional<Player> findByIdWithComments(Long id) {
        Optional<Player> player = playerRepository.findById(id);
        if (player.isEmpty()) {
            return Optional.empty();
        }
        Player p = player.get();
        try {
            p.setComments(normalizeIds(commentClient.findByPlayerId(id)));
        } catch (Exception e) {
            // Si el microservicio comments está caído, devolvemos el
            // jugador con lista vacía en vez de tirar 500.
            p.setComments(new ArrayList<>());
        }
        return Optional.of(p);
    }

    /**
     * Crea un comentario para un jugador. Inyecta el playerId en el
     * body antes de mandarlo al microservicio comments por Feign.
     * Devuelve null si el jugador no existe.
     */
    public Map<String, Object> addComment(Long playerId, Map<String, Object> body) {
        if (!playerRepository.existsById(playerId)) {
            return null;
        }
        Map<String, Object> payload = new HashMap<>(body);
        payload.put("playerId", playerId);
        Map<String, Object> created = commentClient.createComment(payload);
        return normalizeId(created);
    }

    /**
     * Borra un comentario por id en el microservicio comments. Si el
     * jugador no existe localmente, devuelve false (404). El propio
     * comments service devuelve 404 si el comment no existe (Feign lo
     * lanzará como FeignException.NotFound, que se propaga).
     */
    public boolean deleteComment(Long playerId, Long commentId) {
        if (!playerRepository.existsById(playerId)) {
            return false;
        }
        commentClient.deleteComment(commentId);
        return true;
    }

    /** Renombra "id" → "_id" en cada comment para alinear con MEAN. */
    private List<Map<String, Object>> normalizeIds(List<Map<String, Object>> comments) {
        List<Map<String, Object>> out = new ArrayList<>(comments.size());
        for (Map<String, Object> c : comments) {
            out.add(normalizeId(c));
        }
        return out;
    }

    private Map<String, Object> normalizeId(Map<String, Object> comment) {
        if (comment == null) return null;
        Map<String, Object> copy = new HashMap<>(comment);
        Object id = copy.remove("id");
        if (id != null) {
            copy.put("_id", String.valueOf(id));
        }
        return copy;
    }

    private ResponseEntity<ApiResponse> callApiFootball(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, ApiResponse.class);
    }
}
