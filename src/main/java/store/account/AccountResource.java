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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import store.account.api.AccountIn;
import store.account.api.AccountOut;
import store.account.api.LoginIn;

/**
 * HTTP contract of the account service (same paths as the course's AccountController).
 * Internal service: the gateway authenticates users; this class only maps HTTP to AccountService.
 *
 * <p>{@code id-account} is set by the gateway (never by the client) on every request that comes from
 * outside; AccountService uses it to restrict reads and deletes to the caller's own account.
 */
@RestController
@RequestMapping("/accounts")
public class AccountResource {

    static final String ID_ACCOUNT = "id-account";

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
    public List<AccountOut> findAll(@RequestHeader(name = ID_ACCOUNT, required = false) String caller) {
        return service.findAll(caller);
    }

    @GetMapping("/{id}")
    public AccountOut findById(@PathVariable String id,
                               @RequestHeader(name = ID_ACCOUNT, required = false) String caller) {
        return service.findById(id, caller);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       @RequestHeader(name = ID_ACCOUNT, required = false) String caller) {
        service.delete(id, caller);
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
