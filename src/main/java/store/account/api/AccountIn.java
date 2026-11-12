package store.account.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /accounts}. Same JSON shape as the course's {@code AccountIn}.
 *
 * <p>Only structural limits live here (column sizes, BCrypt's 72-byte input limit).
 * Business rules such as a minimum password length belong in {@code AccountService}.
 */
public record AccountIn(
    @NotBlank @Size(max = 256) String name,
    @NotBlank @Email @Size(max = 256) String email,
    @NotBlank @Size(max = 72) String password
) {}
