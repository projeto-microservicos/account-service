package store.account;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import store.account.api.AccountIn;
import store.account.api.AccountOut;
import store.account.api.LoginIn;

/**
 * Business rules for accounts.
 *
 * <ul>
 *   <li>E-mails are stored normalized (trim + lowercase), so the UNIQUE column blocks duplicates in any case.</li>
 *   <li>Passwords: at least {@value #MIN_PASSWORD} characters and at most 72 UTF-8 bytes (BCrypt ignores the rest);
 *       only the BCrypt hash is stored.</li>
 *   <li>Login answers the same 401 for an unknown e-mail and a wrong password, and spends about the same
 *       time in both cases, so it never reveals which e-mails are registered.</li>
 *   <li>Ownership: requests that come through the gateway carry {@code id-account}; they may only see or
 *       delete that account (anything else is 404, as order-service does). Calls without the header can only
 *       come from inside the cluster (auth-service), because the gateway always sets it.</li>
 * </ul>
 */
@Service
public class AccountService {

    static final int MIN_PASSWORD = 8;
    static final int MAX_PASSWORD_BYTES = 72;

    private final AccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    /** Hash compared against when the e-mail is unknown, so both 401 paths cost one BCrypt check. */
    private final String dummyHash;

    public AccountService(AccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AccountOut create(AccountIn in) {
        String email = normalize(in.email());
        checkPassword(in.password());
        if (repository.existsByEmail(email)) {
            throw conflict();
        }
        var model = new AccountModel(UUID.randomUUID().toString(), in.name().trim(), email,
            passwordEncoder.encode(in.password()), OffsetDateTime.now(ZoneOffset.UTC));
        try {
            return repository.saveAndFlush(model).toOut();
        } catch (DataIntegrityViolationException e) {
            // Two registrations with the same e-mail at the same time: the UNIQUE constraint decides.
            throw conflict();
        }
    }

    /** Through the gateway, only the caller's own account; inside the cluster, every account. */
    @Transactional(readOnly = true)
    public List<AccountOut> findAll(String caller) {
        if (caller != null) {
            return repository.findById(caller).map(AccountModel::toOut).stream().toList();
        }
        return repository.findAll(Sort.by("createdAt", "id")).stream().map(AccountModel::toOut).toList();
    }

    @Transactional(readOnly = true)
    public AccountOut findById(String id, String caller) {
        return owned(id, caller).toOut();
    }

    @Transactional
    public void delete(String id, String caller) {
        repository.delete(owned(id, caller));
    }

    @Transactional(readOnly = true)
    public AccountOut login(LoginIn in) {
        var account = repository.findByEmail(normalize(in.email()));
        String hash = account.map(AccountModel::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(in.password(), hash);
        if (account.isEmpty() || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        return account.get().toOut();
    }

    private AccountModel owned(String id, String caller) {
        if (caller != null && !caller.equals(id)) {
            throw notFound(id);
        }
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    private static void checkPassword(String password) {
        if (password.length() < MIN_PASSWORD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "password must have at least " + MIN_PASSWORD + " characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "password must have at most " + MAX_PASSWORD_BYTES + " bytes");
        }
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "e-mail already registered");
    }

    private static ResponseStatusException notFound(String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "account " + id + " not found");
    }

}
