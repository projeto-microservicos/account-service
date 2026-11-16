package store.account;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import store.account.api.AccountIn;
import store.account.api.AccountOut;
import store.account.api.LoginIn;

/**
 * Account persistence and credential verification; exposed internally through auth.
 */
@Service
public class AccountService {

    private final AccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String dummyHash;

    public AccountService(AccountRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AccountOut create(AccountIn in) {
        if (in.password().length() < 8 || in.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must have at least 8 characters and at most 72 bytes");
        }
        String email = normalize(in.email());
        if (repository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        var model = new AccountModel(UUID.randomUUID().toString(), in.name().trim(), email,
            passwordEncoder.encode(in.password()), OffsetDateTime.now(ZoneOffset.UTC));
        try {
            return repository.saveAndFlush(model).toOut();
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
    }

    @Transactional(readOnly = true)
    public List<AccountOut> findAll() {
        return repository.findAll().stream().map(AccountModel::toOut).toList();
    }

    @Transactional(readOnly = true)
    public AccountOut findById(String id) {
        return find(id).toOut();
    }

    @Transactional
    public void delete(String id) {
        repository.delete(find(id));
    }

    @Transactional(readOnly = true)
    public AccountOut login(LoginIn in) {
        var account = repository.findByEmail(normalize(in.email()));
        String hash = account.map(AccountModel::getPasswordHash).orElse(dummyHash);
        boolean matches = in.password().getBytes(StandardCharsets.UTF_8).length <= 72
            && passwordEncoder.matches(in.password(), hash);
        if (!matches || account.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return account.get().toOut();
    }

    private AccountModel find(String id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

}
