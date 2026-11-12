package store.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import store.account.api.AccountOut;

/** JPA mapping of {@code accounts.accounts} (see db/migration/V1__accounts.sql). */
@Entity
@Table(name = "accounts")
public class AccountModel {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(nullable = false, unique = true, length = 256)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected AccountModel() {}

    public AccountModel(String id, String name, String email, String passwordHash, OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public AccountOut toOut() {
        return new AccountOut(id, name, email);
    }

}
