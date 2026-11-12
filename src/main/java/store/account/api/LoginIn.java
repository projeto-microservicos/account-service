package store.account.api;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /accounts/login}, called internally by auth-service. */
public record LoginIn(
    @NotBlank String email,
    @NotBlank String password
) {}
