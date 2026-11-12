package store.account;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import store.account.api.AccountIn;
import store.account.api.AccountOut;
import store.account.api.LoginIn;

/**
 * Business rules for accounts. The HTTP layer (AccountResource) and persistence
 * (AccountModel, AccountRepository, Flyway) are ready; every method below is yours.
 *
 * <p>Until implemented, each method answers 501 Not Implemented. The disabled tests in
 * AccountContractTest describe the expected behavior: enable them one by one.
 */
@Service
public class AccountService {

    private final AccountRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AccountOut create(AccountIn in) {
        // TODO(account): implement registration.
        //  1. Normalize the e-mail (trim + lowercase) before checking or saving it.
        //  2. Enforce your password policy (e.g. minimum length) -> 400.
        //  3. E-mail already registered -> 409 CONFLICT.
        //  4. Hash with passwordEncoder.encode(...). Never store or log the plain password.
        //  5. Save new AccountModel(UUID.randomUUID().toString(), ..., OffsetDateTime.now(ZoneOffset.UTC)).
        throw todo("create account");
    }

    @Transactional(readOnly = true)
    public List<AccountOut> findAll() {
        // TODO(account): return every account as AccountOut.
        //  Decide whether this listing should exist outside the cluster (privacy).
        throw todo("list accounts");
    }

    @Transactional(readOnly = true)
    public AccountOut findById(String id) {
        // TODO(account): repository.findById(id) -> 404 NOT_FOUND when absent.
        throw todo("find account by id");
    }

    @Transactional
    public void delete(String id) {
        // TODO(account): delete the account; decide between 404 or idempotent 204 when absent.
        throw todo("delete account");
    }

    @Transactional(readOnly = true)
    public AccountOut login(LoginIn in) {
        // TODO(account): validate credentials for auth-service.
        //  - Look up by the normalized e-mail and use passwordEncoder.matches(raw, hash).
        //  - Unknown e-mail and wrong password must both answer the same 401 UNAUTHORIZED,
        //    so the response never reveals which e-mails are registered.
        throw todo("check credentials");
    }

    private static ResponseStatusException todo(String what) {
        return new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "TODO: " + what);
    }

}
