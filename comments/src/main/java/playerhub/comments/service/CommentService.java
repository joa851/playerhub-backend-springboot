package playerhub.comments.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import playerhub.comments.domain.Comment;
import playerhub.comments.repository.CommentRepository;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    public List<Comment> findByPlayerId(Long playerId) {
        return commentRepository.findByPlayerId(playerId);
    }
}
