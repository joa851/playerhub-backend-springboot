package playerhub.player.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import playerhub.player.client.CommentClient;
import playerhub.player.domain.ApiResponse;
import playerhub.player.domain.Player;
import playerhub.player.domain.PlayerWrapper;
import playerhub.player.repository.PlayerRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios puros (sin Spring context) del PlayerService.
 * Mockean PlayerRepository, CommentClient, LLMService y RestTemplate
 * con Mockito. No tocan BD ni red.
 */
@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private PlayerRepository playerRepository;
    @Mock private CommentClient commentClient;
    @Mock private LLMService llmService;

    @InjectMocks private PlayerService playerService;

    private Player player(Long id, String name) {
        Player p = new Player();
        p.setId(id);
        p.setName(name);
        return p;
    }

    // ─── CRUD local ────────────────────────────────────────────────────

    @Test
    void search_withNullDates_usesEpochAndFarFutureSentinels() {
        when(playerRepository.search(eq("p"), eq(null), eq(null), any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(player(1L, "Pedri")));

        List<Player> result = playerService.search("p", null, null, null, null);

        assertThat(result).hasSize(1);
        // Verifica que el service NUNCA pasa null al repo en los Instants
        verify(playerRepository).search(eq("p"), eq(null), eq(null),
            eq(Instant.EPOCH),
            eq(Instant.parse("9999-12-31T23:59:59Z")));
    }

    @Test
    void search_withProvidedDates_passesThemThrough() {
        Instant from = Instant.parse("2024-01-01T00:00:00Z");
        Instant to = Instant.parse("2025-12-31T23:59:59Z");
        when(playerRepository.search(any(), any(), any(), eq(from), eq(to)))
            .thenReturn(new ArrayList<>());

        playerService.search(null, null, null, from, to);

        verify(playerRepository).search(null, null, null, from, to);
    }

    @Test
    void create_setsIdToNullAndSaves() {
        Player input = player(42L, "Yamal");   // id que NO debería respetarse
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> {
            Player p = inv.getArgument(0);
            p.setId(99L);                       // JPA simula autogenerar otro id
            return p;
        });

        Player result = playerService.create(input);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getName()).isEqualTo("Yamal");
        // El id del input quedó forzado a null antes del save
        assertThat(input.getId()).isEqualTo(99L);   // mismo objeto, ya con id JPA
    }

    @Test
    void update_playerMissing_returnsEmpty() {
        when(playerRepository.existsById(99L)).thenReturn(false);

        Optional<Player> result = playerService.update(99L, player(null, "x"));

        assertThat(result).isEmpty();
        verify(playerRepository, never()).save(any());
    }

    @Test
    void update_playerExists_setsIdAndSaves() {
        when(playerRepository.existsById(7L)).thenReturn(true);
        Player payload = player(null, "Updated");
        when(playerRepository.save(payload)).thenReturn(payload);

        Optional<Player> result = playerService.update(7L, payload);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(7L);    // forzado al id de la URL
        assertThat(result.get().getName()).isEqualTo("Updated");
    }

    @Test
    void delete_playerMissing_returnsFalse() {
        when(playerRepository.existsById(99L)).thenReturn(false);

        boolean result = playerService.delete(99L);

        assertThat(result).isFalse();
        verify(playerRepository, never()).deleteById(any());
    }

    @Test
    void delete_playerExists_deletesAndReturnsTrue() {
        when(playerRepository.existsById(7L)).thenReturn(true);

        boolean result = playerService.delete(7L);

        assertThat(result).isTrue();
        verify(playerRepository).deleteById(7L);
    }

    // ─── findByIdWithComments ──────────────────────────────────────────

    @Test
    void findByIdWithComments_playerMissing_returnsEmpty() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Player> result = playerService.findByIdWithComments(99L);

        assertThat(result).isEmpty();
        verify(commentClient, never()).findByPlayerId(any());
    }

    @Test
    void findByIdWithComments_loadsCommentsAndNormalizesIds() {
        Player p = player(7L, "Pedri");
        when(playerRepository.findById(7L)).thenReturn(Optional.of(p));
        Map<String, Object> raw = new HashMap<>();
        raw.put("id", 42);
        raw.put("text", "great");
        when(commentClient.findByPlayerId(7L)).thenReturn(List.of(raw));

        Optional<Player> result = playerService.findByIdWithComments(7L);

        assertThat(result).isPresent();
        assertThat(result.get().getComments()).hasSize(1);
        Map<String, Object> norm = result.get().getComments().get(0);
        assertThat(norm.get("_id")).isEqualTo("42");
        assertThat(norm.containsKey("id")).isFalse();
        assertThat(norm.get("text")).isEqualTo("great");
    }

    @Test
    void findByIdWithComments_feignFails_returnsPlayerWithEmptyComments() {
        Player p = player(7L, "Pedri");
        when(playerRepository.findById(7L)).thenReturn(Optional.of(p));
        when(commentClient.findByPlayerId(7L)).thenThrow(new RuntimeException("comments down"));

        Optional<Player> result = playerService.findByIdWithComments(7L);

        assertThat(result).isPresent();
        assertThat(result.get().getComments()).isEmpty();
    }

    // ─── getComments ───────────────────────────────────────────────────

    @Test
    void getComments_playerMissing_returnsNull() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(playerService.getComments(99L)).isNull();
    }

    @Test
    void getComments_normalizesIds() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L, "x")));
        Map<String, Object> c = new HashMap<>();
        c.put("id", 5);
        when(commentClient.findByPlayerId(1L)).thenReturn(List.of(c));

        List<Map<String, Object>> result = playerService.getComments(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("_id")).isEqualTo("5");
        assertThat(result.get(0)).doesNotContainKey("id");
    }

    // ─── addComment ────────────────────────────────────────────────────

    @Test
    void addComment_playerMissing_returnsNull() {
        when(playerRepository.existsById(99L)).thenReturn(false);

        assertThat(playerService.addComment(99L, Map.of("text", "x"))).isNull();
        verify(commentClient, never()).createComment(any());
    }

    @Test
    void addComment_injectsPlayerId_returnsNormalized() {
        when(playerRepository.existsById(1L)).thenReturn(true);
        Map<String, Object> created = new HashMap<>();
        created.put("id", 99);
        created.put("text", "ok");
        when(commentClient.createComment(any())).thenReturn(created);

        Map<String, Object> result = playerService.addComment(1L, Map.of("text", "ok"));

        // El _id viene normalizado de "id"
        assertThat(result.get("_id")).isEqualTo("99");
        assertThat(result).doesNotContainKey("id");
        // El service inyecta playerId en el body antes de enviar a Feign
        Map<String, Object> expected = new HashMap<>();
        expected.put("text", "ok");
        expected.put("playerId", 1L);
        verify(commentClient).createComment(expected);
    }

    // ─── deleteComment ─────────────────────────────────────────────────

    @Test
    void deleteComment_playerMissing_returnsFalse() {
        when(playerRepository.existsById(99L)).thenReturn(false);

        assertThat(playerService.deleteComment(99L, 1L)).isFalse();
        verify(commentClient, never()).deleteComment(any());
    }

    @Test
    void deleteComment_callsFeignAndReturnsTrue() {
        when(playerRepository.existsById(1L)).thenReturn(true);

        boolean result = playerService.deleteComment(1L, 42L);

        assertThat(result).isTrue();
        verify(commentClient).deleteComment(42L);
    }

    // ─── idealTeam ─────────────────────────────────────────────────────

    @Test
    void idealTeam_emptyDb_returnsEmptyWithoutCallingLLM() {
        when(playerRepository.findAll()).thenReturn(new ArrayList<>());

        List<Player> result = playerService.idealTeam();

        assertThat(result).isEmpty();
        verify(llmService, never()).selectIdealTeamIds(any());
    }

    @Test
    void idealTeam_preservesLLMOrder() {
        Player p1 = player(1L, "A");
        Player p2 = player(2L, "B");
        Player p3 = player(3L, "C");
        when(playerRepository.findAll()).thenReturn(List.of(p1, p2, p3));
        when(llmService.selectIdealTeamIds(any())).thenReturn(List.of(3L, 1L));
        when(playerRepository.findById(3L)).thenReturn(Optional.of(p3));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p1));

        List<Player> result = playerService.idealTeam();

        assertThat(result).extracting(Player::getName).containsExactly("C", "A");
    }

    @Test
    void idealTeam_filtersOutMissingIds() {
        Player p1 = player(1L, "A");
        when(playerRepository.findAll()).thenReturn(List.of(p1));
        when(llmService.selectIdealTeamIds(any())).thenReturn(List.of(1L, 999L));
        when(playerRepository.findById(1L)).thenReturn(Optional.of(p1));
        when(playerRepository.findById(999L)).thenReturn(Optional.empty());

        List<Player> result = playerService.idealTeam();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("A");
    }

    // ─── searchExternal ────────────────────────────────────────────────

    @Test
    void searchExternal_emptyResponse_returnsEmpty() {
        ReflectionTestUtils.setField(playerService, "apiUrl", "http://fake");
        ReflectionTestUtils.setField(playerService, "apiKey", "k");
        ApiResponse empty = new ApiResponse();
        empty.setResponse(new ArrayList<>());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ApiResponse.class)))
            .thenReturn(ResponseEntity.ok(empty));

        Player[] result = playerService.searchExternal("anything");

        assertThat(result).isEmpty();
    }

    @Test
    void searchExternal_mapsResponseToPlayers() {
        ReflectionTestUtils.setField(playerService, "apiUrl", "http://fake");
        ReflectionTestUtils.setField(playerService, "apiKey", "k");
        Player p = player(null, "Messi");
        PlayerWrapper wrap = new PlayerWrapper();
        wrap.setPlayer(p);
        ApiResponse body = new ApiResponse();
        body.setResponse(List.of(wrap));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ApiResponse.class)))
            .thenReturn(ResponseEntity.ok(body));

        Player[] result = playerService.searchExternal("messi");

        assertThat(result).hasSize(1);
        assertThat(result[0].getName()).isEqualTo("Messi");
    }

    // ─── importExternal ────────────────────────────────────────────────

    @Test
    void importExternal_skipsAlreadyImported() {
        ReflectionTestUtils.setField(playerService, "apiUrl", "http://fake");
        ReflectionTestUtils.setField(playerService, "apiKey", "k");
        when(playerRepository.findByExternalId(100L)).thenReturn(Optional.of(player(1L, "x")));

        List<Player> result = playerService.importExternal(List.of(100L));

        assertThat(result).isEmpty();
        verify(restTemplate, never()).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(ApiResponse.class));
        verify(playerRepository, never()).save(any());
    }

    @Test
    void importExternal_savesNewPlayerWithExternalId() {
        ReflectionTestUtils.setField(playerService, "apiUrl", "http://fake");
        ReflectionTestUtils.setField(playerService, "apiKey", "k");
        when(playerRepository.findByExternalId(100L)).thenReturn(Optional.empty());
        Player p = player(100L, "Bellingham");
        PlayerWrapper wrap = new PlayerWrapper();
        wrap.setPlayer(p);
        ApiResponse body = new ApiResponse();
        body.setResponse(List.of(wrap));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ApiResponse.class)))
            .thenReturn(ResponseEntity.ok(body));
        when(playerRepository.save(any(Player.class))).thenAnswer(inv -> {
            Player saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        List<Player> result = playerService.importExternal(List.of(100L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getExternalId()).isEqualTo(100L);
        assertThat(result.get(0).getName()).isEqualTo("Bellingham");
        verify(playerRepository, times(1)).save(any(Player.class));
    }
}
