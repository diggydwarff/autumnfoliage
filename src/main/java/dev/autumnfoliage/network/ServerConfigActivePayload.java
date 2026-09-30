package dev.autumnfoliage.network;

/** Zero-data marker telling a compatible client that Autumn Foliage server policy is active. */
public record ServerConfigActivePayload() {
    public static final ServerConfigActivePayload INSTANCE = new ServerConfigActivePayload();
}
