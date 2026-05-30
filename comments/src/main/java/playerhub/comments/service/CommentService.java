package playerhub.comments.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import playerhub.comments.domain.Comment;
import playerhub.comments.repository.CommentRepository;

/**
 * Lógica de negocio del microservicio comments. Encapsula el acceso a
 * CommentRepository para que el controller no toque directamente la
 * capa de persistencia.
 */
@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    /** Lista todos los comentarios (sin filtro). */
    public List<Comment> findAll() {
        List<Comment> out = new ArrayList<>();
        commentRepository.findAll().forEach(out::add);
        return out;
    }

    /** Lista los comentarios de un jugador concreto. */
    public List<Comment> findByPlayerId(Long playerId) {
        return commentRepository.findByPlayerId(playerId);
    }

    /** Busca un comentario por su id local. */
    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    /** Crea un comentario nuevo. Garantiza id=null para que JPA autogenere. */
    public Comment create(Comment comment) {
        comment.setId(null);
        return commentRepository.save(comment);
    }

    /**
     * Borra un comentario por id. Devuelve true si existía y se borró,
     * false si no existía (el controller lo traducirá a 404).
     */
    public boolean delete(Long id) {
        if (!commentRepository.existsById(id)) {
            return false;
        }
        commentRepository.deleteById(id);
        return true;
    }
}
