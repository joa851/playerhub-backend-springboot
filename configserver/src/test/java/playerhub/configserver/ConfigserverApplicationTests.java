package playerhub.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConfigserverApplicationTests {

	@Autowired
	private ApplicationContext context;

	/**
	 * Smoke test idiomático de Spring Boot: si @SpringBootTest arranca
	 * el ApplicationContext sin lanzar excepciones, la auto-config de
	 * Spring Cloud Config Server (@EnableConfigServer) está bien
	 * cargada y el repo git remoto es accesible.
	 *
	 * Verificamos que el contexto está vivo para que el método no quede
	 * vacío (Sonar squid:S2187).
	 */
	@Test
	void contextLoads() {
		assertThat(context).isNotNull();
		assertThat(context.getId()).isNotBlank();
	}

}
