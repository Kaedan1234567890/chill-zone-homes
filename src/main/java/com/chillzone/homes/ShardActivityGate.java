package com.chillzone.homes;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Shard eligibility only; ordinary tracked playtime remains unaffected. */
public final class ShardActivityGate {
    private static final long IDLE_TICKS = 5L * 60L * 20L;
    private static final int PATTERN_THRESHOLD = 20;
    private final Map<UUID, State> states = new HashMap<>();
    private static final class State {
        double x, y, z;
        boolean initialized;
        long lastRealActivity;
        String lastStep = "";
        int repeats;
        boolean blockedByPattern;
        boolean observedRealActivity;
        int recoverySteps;
        final ArrayDeque<String> recent = new ArrayDeque<>();
    }
    public void join(ServerPlayer player, long tick) {
        State state = new State();
        state.lastRealActivity = tick;
        states.put(player.getUUID(), state);
    }
    public void leave(UUID id) { states.remove(id); }
    public void clear() { states.clear(); }
    public void observe(ServerPlayer player, long tick) {
        UUID id = player.getUUID();
        State s = states.computeIfAbsent(id, k -> new State());
        double x = player.getX(), y = player.getY(), z = player.getZ();
        if (!s.initialized) { s.x=x; s.y=y; s.z=z; s.initialized=true; s.lastRealActivity=tick; return; }
        double dx = x-s.x, dy = y-s.y, dz = z-s.z;
        s.x=x; s.y=y; s.z=z;
        double distance = Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (distance < 0.08) return; // idle or pressing a key against a wall
        if (distance > 16) return; // teleports do not count as natural activity
        // Discretize directions. Constant straight-line/loop macros earn no Shards.
        String move = (dx>0.12?"E":dx< -0.12?"W":"_") +
                      (dz>0.12?"S":dz< -0.12?"N":"_") +
                      (dy>0.3?"U":dy< -0.3?"D":"_");
        if (move.equals(s.lastStep)) s.repeats++; else {s.lastStep=move;s.repeats=1;}
        s.recent.addLast(move);
        if (s.recent.size()>80) s.recent.removeFirst();
        boolean macro = s.repeats >= PATTERN_THRESHOLD;
        if (!macro && s.recent.size()>=60) {
            ArrayList<String> r = new ArrayList<>(s.recent);
            for (int period=2;period<=8;period++) {
                boolean repeated=true;
                for (int i=r.size()-period*5;i<r.size();i++) {
                    if (i<0 || !r.get(i).equals(r.get(i-period))) {repeated=false;break;}
                }
                if (repeated) {macro=true;break;}
            }
        }
        if (macro) { s.blockedByPattern = true; s.recoverySteps = 0; }
        else if (s.blockedByPattern) {
            // Require a sustained change of direction/activity rather than one macro pause.
            s.recoverySteps++;
            if (s.recoverySteps >= 30) {
                s.blockedByPattern = false;
                s.recoverySteps = 0;
                s.recent.clear();
            }
        }
        if (!s.blockedByPattern) { s.lastRealActivity = tick; s.observedRealActivity = true; }
    }
    public boolean eligible(UUID id, long tick) {
        State s=states.get(id);
        return s!=null && s.observedRealActivity && tick-s.lastRealActivity<IDLE_TICKS && !s.blockedByPattern;
    }
}
