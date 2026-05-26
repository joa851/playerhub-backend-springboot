package playerhub.player.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import playerhub.player.domain.Player;

@Repository
public interface PlayerRepository extends CrudRepository<Player, Long> {

	public Optional<Player> findById(Long id);

	public Optional<Player> findByExternalId(Long externalId);

	@Query("SELECT p FROM Player p WHERE "
		+ "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND "
		+ "(:team IS NULL OR p.team = :team) AND "
		+ "(:league IS NULL OR p.league = :league) AND "
		+ "(:from IS NULL OR p.createdAt >= :from) AND "
		+ "(:to IS NULL OR p.createdAt <= :to)")
	public List<Player> search(
		@Param("name") String name,
		@Param("team") String team,
		@Param("league") String league,
		@Param("from") Instant from,
		@Param("to") Instant to);
}
