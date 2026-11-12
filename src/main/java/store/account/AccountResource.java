package store.account;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.account.api.AccountIn;
import store.account.api.AccountOut;
import store.account.api.LoginIn;

/**
 * HTTP contract of the account service (same paths as the course's AccountController).
 * Internal service: the gateway authenticates users; this class only maps HTTP to AccountService.
 */
@RestController
@RequestMapping("/accounts")
public class AccountResource {

    private final AccountService service;

    public AccountResource(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AccountOut> create(@Valid @RequestBody AccountIn in) {
        AccountOut created = service.create(in);
        return ResponseEntity.created(URI.create("/accounts/" + created.id())).body(created);
    }

    @GetMapping
    public List<AccountOut> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AccountOut findById(@PathVariable String id) {
        return service.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Credential check used by auth-service. The gateway must not expose it publicly. */
    @PostMapping("/login")
    public AccountOut login(@Valid @RequestBody LoginIn in) {
        return service.login(in);
    }

    @GetMapping("/health-check")
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }

}
