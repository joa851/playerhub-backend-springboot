package playerhub.player.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import playerhub.player.domain.Player;

@Repository
public interface PlayerRepository extends CrudRepository<Player, Long>{
	public Optional<Player> findById(Long id);
}
