package store.account.api;

/** Public view of an account. Never carries the password or its hash. */
public record AccountOut(
    String id,
    String name,
    String email
) {}
