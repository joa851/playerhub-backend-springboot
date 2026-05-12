package playerhub.player.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import playerhub.player.domain.ApiResponse;
import playerhub.player.domain.Player;
import playerhub.player.domain.PlayerWrapper;

@Service
public class PlayerService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.football.base-url}")
    private String apiUrl;

    @Value("${api.football.key}")
    private String apiKey;

    public Player[] getAllPlayers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse> response = restTemplate.exchange(
            apiUrl,
            HttpMethod.GET,
            entity,
            ApiResponse.class
        );

        ApiResponse body = response.getBody();
        if (body == null || body.getResponse() == null) {
            return new Player[0];
        }
        return body.getResponse().stream()
            .map(PlayerWrapper::getPlayer)
            .toArray(Player[]::new);
    }
    
    
}