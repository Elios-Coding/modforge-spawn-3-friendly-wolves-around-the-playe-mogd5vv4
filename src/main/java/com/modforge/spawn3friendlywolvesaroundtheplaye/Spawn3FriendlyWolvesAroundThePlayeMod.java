package com.modforge.spawn3friendlywolvesaroundtheplaye;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class Spawn3FriendlyWolvesAroundThePlayeMod implements ModInitializer {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("spawn-3-friendly-wolves-around-the-playe-mogd5vv4");

    // 5 seconds at 20 TPS. Kept simple and server-tick based to avoid uncertain APIs.
    private static final int COOLDOWN_TICKS = 20 * 5;

    // IdentityHashMap avoids relying on UUID/name APIs that may vary.
    private final java.util.IdentityHashMap<net.minecraft.server.network.ServerPlayerEntity, Integer> cooldowns = new java.util.IdentityHashMap<>();

    @Override
    public void onInitialize() {
        try {
            ServerTickEvents.END_SERVER_TICK.register(server -> {
                try {
                    // Tick down cooldowns.
                    if (!cooldowns.isEmpty()) {
                        java.util.Iterator<java.util.Map.Entry<net.minecraft.server.network.ServerPlayerEntity, Integer>> it = cooldowns.entrySet().iterator();
                        while (it.hasNext()) {
                            java.util.Map.Entry<net.minecraft.server.network.ServerPlayerEntity, Integer> e = it.next();
                            net.minecraft.server.network.ServerPlayerEntity p = e.getKey();
                            if (p == null) {
                                it.remove();
                                continue;
                            }
                            Integer boxed = e.getValue();
                            int v = boxed == null ? 0 : boxed.intValue();
                            v--;
                            if (v <= 0) it.remove();
                            else e.setValue(Integer.valueOf(v));
                        }
                    }

                    for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        if (player == null) continue;
                        if (!player.isSneaking()) continue;
                        if (!player.getMainHandStack().isOf(net.minecraft.item.Items.BONE)) continue;

                        Integer cd = cooldowns.get(player);
                        if (cd != null && cd.intValue() > 0) continue;

                        net.minecraft.server.world.ServerWorld serverWorld;
                        try {
                            serverWorld = (net.minecraft.server.world.ServerWorld) player.getWorld();
                        } catch (Throwable t) {
                            // Should not happen on server, but keep safe.
                            LOGGER.error("Failed to access ServerWorld from player", t);
                            continue;
                        }

                        spawnThreeFriendlyWolves(serverWorld, player);
                        cooldowns.put(player, Integer.valueOf(COOLDOWN_TICKS));
                    }
                } catch (Throwable t) {
                    LOGGER.error("Error during END_SERVER_TICK handler", t);
                }
            });
        } catch (Throwable t) {
            LOGGER.error("Failed to initialize mod", t);
        }
    }

    private void spawnThreeFriendlyWolves(net.minecraft.server.world.ServerWorld serverWorld, net.minecraft.server.network.ServerPlayerEntity player) {
        try {
            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();

            // Spawn in a small ring around the player.
            double radius = 2.0;

            for (int i = 0; i < 3; i++) {
                double angle = (Math.PI * 2.0) * (i / 3.0);
                double sx = px + Math.cos(angle) * radius;
                double sz = pz + Math.sin(angle) * radius;
                double sy = py;

                // Avoid EntityType#create/spawn overload hazards: use constructor + spawnEntity.
                net.minecraft.entity.passive.WolfEntity wolf = new net.minecraft.entity.passive.WolfEntity(net.minecraft.entity.EntityType.WOLF, serverWorld);
                wolf.refreshPositionAndAngles(sx, sy, sz, 0f, 0f);

                // Make it friendly.
                wolf.setOwner(player);
                wolf.setTame(true);

                serverWorld.spawnEntity(wolf);
            }

            // Feedback kept minimal: particle + sound. If these mappings change, gameplay still works.
            serverWorld.spawnParticles(net.minecraft.particle.ParticleTypes.HEART, px, py + 1.0, pz, 8, 0.6, 0.6, 0.6, 0.0);
            serverWorld.playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_WOLF_AMBIENT, net.minecraft.sound.SoundCategory.NEUTRAL, 1f, 1f);
        } catch (Throwable t) {
            LOGGER.error("Failed to spawn friendly wolves", t);
        }
    }
}
