package playerhub.comments.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import playerhub.comments.domain.Comment;

@Repository
public interface CommentRepository extends CrudRepository<Comment, Long> {

	public Optional<Comment> findById(Long id);

	public List<Comment> findByPlayerId(Long playerId);
}
