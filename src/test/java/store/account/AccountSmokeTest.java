package store.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Verifies the wiring that is already done: HTTP, validation, Flyway/JPA, actuator, OpenAPI. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "management.server.port=0",
    "spring.datasource.url=jdbc:h2:mem:accounts-smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="})
class AccountSmokeTest {

    @Value("${local.server.port}") int port;
    @Value("${local.management.port}") int managementPort;
    @Autowired AccountRepository repository;

    RestClient api() { return RestClient.create("http://localhost:" + port); }

    @Test
    void healthCheckAnswers200() {
        var response = api().get().uri("/accounts/health-check").retrieve().toBodilessEntity();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void readinessProbeIsUpOnManagementPort() {
        var body = RestClient.create("http://localhost:" + managementPort)
            .get().uri("/actuator/health/readiness").retrieve().body(String.class);
        assertThat(body).contains("UP");
    }

    @Test
    void flywaySchemaMatchesTheEntity() {
        // ddl-auto=validate would have failed startup otherwise; this also proves queries run.
        assertThat(repository.count()).isZero();
    }

    @Test
    void invalidBodyIsRejectedBeforeReachingTheService() {
        assertThatThrownBy(() -> api().post().uri("/accounts")
                .body(Map.of("name", "", "email", "not-an-email", "password", ""))
                .retrieve().toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void openApiDocumentsTheContract() {
        var docs = api().get().uri("/v3/api-docs").retrieve().body(String.class);
        assertThat(docs).contains("/accounts", "/accounts/{id}", "/accounts/login");
    }

}
