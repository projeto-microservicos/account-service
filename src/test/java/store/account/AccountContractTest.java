package store.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import store.account.api.AccountIn;
import store.account.api.AccountOut;
import store.account.api.LoginIn;

/**
 * Executable specification of AccountService (implemented on 17/11/2026, see the README's "Uso de IA").
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "management.server.port=0",
    "spring.datasource.url=jdbc:h2:mem:accounts-contract;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password="})
class AccountContractTest {

    @Value("${local.server.port}") int port;
    @Autowired AccountRepository repository;

    RestClient api() { return RestClient.create("http://localhost:" + port); }

    static String email() { return "user-" + UUID.randomUUID() + "@example.com"; }

    AccountOut create(String email, String password) {
        return api().post().uri("/accounts").body(new AccountIn("Ana", email, password))
            .retrieve().body(AccountOut.class);
    }

    @Test
    void createReturns201WithLocationAndStoresOnlyAHash() {
        var response = api().post().uri("/accounts")
            .body(new AccountIn("Ana", email(), "s3cret-pass"))
            .retrieve().toEntity(AccountOut.class);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        var created = response.getBody();
        assertThat(response.getHeaders().getLocation()).hasToString("/accounts/" + created.id());
        var stored = repository.findById(created.id()).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("s3cret-pass").startsWith("$2");
    }

    @Test
    void duplicateEmailIsRejectedEvenWithDifferentCase() {
        var email = email();
        create(email, "s3cret-pass");
        assertThatThrownBy(() -> create(email.toUpperCase(), "s3cret-pass"))
            .isInstanceOf(HttpClientErrorException.Conflict.class);
    }

    @Test
    void loginReturnsTheAccountOnlyForTheRightPassword() {
        var email = email();
        var created = create(email, "s3cret-pass");

        var logged = api().post().uri("/accounts/login").body(new LoginIn(email, "s3cret-pass"))
            .retrieve().body(AccountOut.class);
        assertThat(logged).isEqualTo(created);

        assertThatThrownBy(() -> api().post().uri("/accounts/login").body(new LoginIn(email, "wrong-pass"))
                .retrieve().toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.Unauthorized.class);
        assertThatThrownBy(() -> api().post().uri("/accounts/login").body(new LoginIn(email(), "s3cret-pass"))
                .retrieve().toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void findAndDeleteById() {
        var created = create(email(), "s3cret-pass");
        assertThat(api().get().uri("/accounts/{id}", created.id()).retrieve().body(AccountOut.class))
            .isEqualTo(created);

        var deleted = api().delete().uri("/accounts/{id}", created.id()).retrieve().toBodilessEntity();
        assertThat(deleted.getStatusCode().value()).isEqualTo(204);
        assertThatThrownBy(() -> api().get().uri("/accounts/{id}", created.id()).retrieve().toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    void passwordPolicyIsEnforcedBeforeHashing() {
        assertThatThrownBy(() -> create(email(), "short"))
            .isInstanceOf(HttpClientErrorException.BadRequest.class);
        // 24 × "ção" = 72 characters (passes @Size), but 120 bytes in UTF-8: more than BCrypt's 72.
        assertThatThrownBy(() -> create(email(), "ção".repeat(24)))
            .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }

    @Test
    void emailIsNormalizedForStorageAndLogin() {
        var email = email();
        var created = create(email.toUpperCase(), "s3cret-pass"); // @Email already rejects blanks around it
        assertThat(created.email()).isEqualTo(email);
        var logged = api().post().uri("/accounts/login").body(new LoginIn(" " + email.toUpperCase(), "s3cret-pass"))
            .retrieve().body(AccountOut.class);
        assertThat(logged.id()).isEqualTo(created.id());
    }

    @Test
    void throughTheGatewayOnlyTheCallersOwnAccountIsVisible() {
        var ana = create(email(), "s3cret-pass");
        var bob = create(email(), "s3cret-pass");

        var mine = api().get().uri("/accounts").header("id-account", ana.id()).retrieve()
            .body(AccountOut[].class);
        assertThat(mine).containsExactly(ana);
        assertThat(api().get().uri("/accounts/{id}", ana.id()).header("id-account", ana.id())
            .retrieve().body(AccountOut.class)).isEqualTo(ana);

        assertThatThrownBy(() -> api().get().uri("/accounts/{id}", bob.id()).header("id-account", ana.id())
                .retrieve().toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThatThrownBy(() -> api().delete().uri("/accounts/{id}", bob.id()).header("id-account", ana.id())
                .retrieve().toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThat(repository.existsById(bob.id())).isTrue();
    }

    @Test
    void deletingAnUnknownAccountIs404() {
        assertThatThrownBy(() -> api().delete().uri("/accounts/{id}", UUID.randomUUID()).retrieve().toBodilessEntity())
            .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

}
