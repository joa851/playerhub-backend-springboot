package playerhub.comments.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import playerhub.comments.domain.Comment;
import playerhub.comments.repository.CommentRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios puros (sin Spring context) del CommentService.
 * Mockea CommentRepository con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @InjectMocks private CommentService commentService;

    private Comment comment(Long id, String text) {
        Comment c = new Comment();
        c.setId(id);
        c.setText(text);
        return c;
    }

    @Test
    void findAll_iteratesRepoAndReturnsList() {
        when(commentRepository.findAll()).thenReturn(List.of(
            comment(1L, "a"), comment(2L, "b")
        ));

        List<Comment> result = commentService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Comment::getText).containsExactly("a", "b");
    }

    @Test
    void findByPlayerId_delegatesToRepo() {
        when(commentRepository.findByPlayerId(42L)).thenReturn(List.of(comment(1L, "x")));

        List<Comment> result = commentService.findByPlayerId(42L);

        assertThat(result).hasSize(1);
        verify(commentRepository).findByPlayerId(42L);
    }

    @Test
    void findById_existing_returnsPresent() {
        when(commentRepository.findById(7L)).thenReturn(Optional.of(comment(7L, "x")));

        Optional<Comment> result = commentService.findById(7L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(7L);
    }

    @Test
    void findById_missing_returnsEmpty() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Comment> result = commentService.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void create_setsIdToNullAndSaves() {
        Comment input = comment(42L, "should be reset");   // id que NO debería respetarse
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(99L);                                  // JPA simula autogenerar
            return c;
        });

        Comment result = commentService.create(input);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getText()).isEqualTo("should be reset");
        // El id del input quedó forzado a null antes del save → JPA puso 99
        assertThat(input.getId()).isEqualTo(99L);
    }

    @Test
    void delete_missing_returnsFalse() {
        when(commentRepository.existsById(99L)).thenReturn(false);

        boolean result = commentService.delete(99L);

        assertThat(result).isFalse();
        verify(commentRepository, never()).deleteById(any());
    }

    @Test
    void delete_existing_deletesAndReturnsTrue() {
        when(commentRepository.existsById(7L)).thenReturn(true);

        boolean result = commentService.delete(7L);

        assertThat(result).isTrue();
        verify(commentRepository).deleteById(7L);
    }
}
