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

	// Notas:
	// - Strings: CAST AS String para que Postgres deduzca el tipo cuando viene null.
	// - Instant from/to: NO usamos IS NULL aquí; el controller manda Instant.EPOCH /
	//   fecha lejana cuando faltan. Postgres no acepta cast bytea→timestamptz.
	@Query("SELECT p FROM Player p WHERE "
		+ "(CAST(:name AS String) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:name AS String), '%'))) AND "
		+ "(CAST(:team AS String) IS NULL OR p.team = CAST(:team AS String)) AND "
		+ "(CAST(:league AS String) IS NULL OR p.league = CAST(:league AS String)) AND "
		+ "p.createdAt >= :from AND "
		+ "p.createdAt <= :to")
	public List<Player> search(
		@Param("name") String name,
		@Param("team") String team,
		@Param("league") String league,
		@Param("from") Instant from,
		@Param("to") Instant to);
}
