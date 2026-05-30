package playerhub.player.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import playerhub.player.domain.Player;

/**
 * Cliente del LLM (Google Gemini vía AI Studio) para generar el "Equipo Ideal".
 *
 * Construye un prompt con los jugadores disponibles, pide a Gemini que
 * seleccione 11 IDs en formato JSON, y devuelve la lista de IDs.
 */
@Service
public class LLMService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${llm.base-url}")
    private String baseUrl;

    @Value("${llm.model}")
    private String model;

    @Value("${llm.key:}")
    private String apiKey;

    private final ObjectMapper json = new ObjectMapper();

    public List<Long> selectIdealTeamIds(List<Player> candidates) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LLM_KEY no configurada");
        }
        if (candidates == null || candidates.isEmpty()) {
            return new ArrayList<>();
        }

        String prompt = buildPrompt(candidates);
        Map<String, Object> body = buildRequestBody(prompt);

        String url = baseUrl + "/" + model + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        return extractTeamIds(response.getBody());
    }

    private String buildPrompt(List<Player> players) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un entrenador de fútbol experto. De la siguiente lista ");
        sb.append("de jugadores, selecciona los 11 que formarían el equipo ideal ");
        sb.append("(formación libre, balanceada). ");
        sb.append("Si hay menos de 11 jugadores disponibles, selecciona todos.\n\n");
        sb.append("Responde EXCLUSIVAMENTE con un JSON con esta forma:\n");
        sb.append("{\"team\": [<id>, <id>, ...]}\n\n");
        sb.append("Lista de jugadores disponibles:\n");

        for (Player p : players) {
            sb.append("- ID ").append(p.getId());
            sb.append(", nombre: ").append(safe(p.getName()));
            if (p.getPosition() != null) sb.append(", posición: ").append(p.getPosition());
            if (p.getTeam() != null) sb.append(", equipo: ").append(p.getTeam());
            if (p.getLeague() != null) sb.append(", liga: ").append(p.getLeague());
            if (p.getAge() != null) sb.append(", edad: ").append(p.getAge());
            if (p.getNationality() != null) sb.append(", nacionalidad: ").append(p.getNationality());
            sb.append("\n");
        }
        return sb.toString();
    }

    private Map<String, Object> buildRequestBody(String prompt) {
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);
        return body;
    }

    private List<Long> extractTeamIds(String responseBody) {
        try {
            JsonNode root = json.readTree(responseBody);
            JsonNode text = root.path("candidates").path(0)
                                .path("content").path("parts").path(0)
                                .path("text");
            if (text.isMissingNode() || text.asText().isBlank()) {
                return new ArrayList<>();
            }
            JsonNode team = json.readTree(text.asText()).path("team");
            List<Long> ids = new ArrayList<>();
            if (team.isArray()) {
                for (JsonNode idNode : team) {
                    if (idNode.canConvertToLong()) {
                        ids.add(idNode.asLong());
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            // Respuesta inesperada: devuelve lista vacía.
            return new ArrayList<>();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
