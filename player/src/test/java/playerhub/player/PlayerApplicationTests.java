package playerhub.player;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PlayerApplicationTests {

	@Autowired
	private ApplicationContext context;

	/**
	 * Smoke test idiomático de Spring Boot: si @SpringBootTest consigue
	 * arrancar el ApplicationContext sin lanzar excepciones, todos los
	 * beans (controllers, services, repositorios, Feign clients) están
	 * correctamente configurados y conectados entre sí.
	 *
	 * Comprobamos un bean concreto para que el método no quede vacío
	 * (Sonar squid:S2187) y para validar de forma explícita que el
	 * context arrancó.
	 */
	@Test
	void contextLoads() {
		assertThat(context).isNotNull();
		assertThat(context.getBean(playerhub.player.service.PlayerService.class)).isNotNull();
	}

}
