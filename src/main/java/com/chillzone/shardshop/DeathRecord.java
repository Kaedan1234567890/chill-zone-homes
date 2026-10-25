package com.chillzone.shardshop;

/**
 * Gson remains backwards compatible with older files: newly-added primitive fields
 * load as 0/false on old records.
 */
public record DeathRecord(
    String dimension,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    long timestamp,
    String cause,
    int xpBeforeDeath,
    boolean xpRecoveryClaimed
) {
    public DeathRecord withXpRecoveryClaimed(boolean value) {
        return new DeathRecord(dimension, x, y, z, yaw, pitch, timestamp, cause, xpBeforeDeath, value);
    }
}
