package playerhub.player.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import playerhub.player.domain.Player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios del LLMService. Mockean RestTemplate (la llamada
 * a la API de Gemini) y rellenan los @Value mediante reflection.
 */
@ExtendWith(MockitoExtension.class)
class LLMServiceTest {

    @Mock private RestTemplate restTemplate;

    @InjectMocks private LLMService llmService;

    @BeforeEach
    void setupConfig() {
        ReflectionTestUtils.setField(llmService, "baseUrl", "http://fake-llm");
        ReflectionTestUtils.setField(llmService, "model", "gemini-test");
        ReflectionTestUtils.setField(llmService, "apiKey", "fake-key");
    }

    private Player player(Long id, String name) {
        Player p = new Player();
        p.setId(id);
        p.setName(name);
        return p;
    }

    @Test
    void selectIdealTeamIds_missingApiKey_throws() {
        ReflectionTestUtils.setField(llmService, "apiKey", "");

        assertThatThrownBy(() -> llmService.selectIdealTeamIds(List.of(player(1L, "x"))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("LLM_KEY");
    }

    @Test
    void selectIdealTeamIds_emptyCandidates_returnsEmptyWithoutCallingApi() {
        List<Long> result = llmService.selectIdealTeamIds(new ArrayList<>());

        assertThat(result).isEmpty();
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void selectIdealTeamIds_parsesGeminiResponse() {
        String geminiBody =
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"team\\\":[10,20,30]}\"}]}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok(geminiBody));

        List<Long> result = llmService.selectIdealTeamIds(List.of(
            player(10L, "A"), player(20L, "B"), player(30L, "C")
        ));

        assertThat(result).containsExactly(10L, 20L, 30L);
    }

    @Test
    void selectIdealTeamIds_malformedResponseBody_returnsEmpty() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok("not-a-json"));

        List<Long> result = llmService.selectIdealTeamIds(List.of(player(1L, "x")));

        assertThat(result).isEmpty();
    }

    @Test
    void selectIdealTeamIds_textWithoutTeamArray_returnsEmpty() {
        String geminiBody =
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"other\\\":1}\"}]}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok(geminiBody));

        List<Long> result = llmService.selectIdealTeamIds(List.of(player(1L, "x")));

        assertThat(result).isEmpty();
    }

    @Test
    void selectIdealTeamIds_skipsNonNumericIdsInTeamArray() {
        String geminiBody =
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"team\\\":[10,\\\"bad\\\",20]}\"}]}}]}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(ResponseEntity.ok(geminiBody));

        List<Long> result = llmService.selectIdealTeamIds(List.of(player(1L, "x")));

        assertThat(result).containsExactly(10L, 20L);
    }
}
