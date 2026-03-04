package com.example.forbiddenborder;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.BlockItem;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class ForbiddenBorderMod implements ModInitializer {
    private static final double PUSHBACK_DISTANCE = 0.8D;
    private static final int PARTICLE_VIEW_DISTANCE = 128;
    private static final int PARTICLE_POINTS_MIN = 60;

    private static final ParticleEffect BORDER_PARTICLE =
        new BlockStateParticleEffect(ParticleTypes.BLOCK_MARKER, Blocks.BARRIER.getDefaultState());

    private static long tickCounter = 0L;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(this::registerCommands);

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> canModifyAt(world, player, pos));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player.getStackInHand(hand).getItem() instanceof BlockItem)) {
                return ActionResult.PASS;
            }

            BlockPos placePos = hitResult.getBlockPos().offset(hitResult.getSide());
            if (!canModifyAt(world, player, placePos)) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        ForbiddenBorderState state = getState(server);
        if (!state.isEnabled()) {
            return;
        }

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            if (state.isInside(player.getX(), player.getZ())) {
                Vec3d safePosition = computeOutsidePosition(state, player.getPos());
                player.requestTeleport(safePosition.x, player.getY(), safePosition.z);
                player.sendMessage(Text.literal("You cannot enter the forbidden inner border."), true);
            }
        }

        tickCounter++;
        if (tickCounter % 20L == 0L) {
            spawnBorderParticles(server, state);
        }
    }

    private void spawnBorderParticles(MinecraftServer server, ForbiddenBorderState state) {
        int points = Math.max(PARTICLE_POINTS_MIN, (int) (state.getRadius() * 1.5D));

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
                continue;
            }

            double baseY = player.getY();
            for (int i = 0; i < points; i++) {
                double angle = (Math.PI * 2.0D * i) / points;
                double x = state.getCenterX() + Math.cos(angle) * state.getRadius();
                double z = state.getCenterZ() + Math.sin(angle) * state.getRadius();

                if (player.squaredDistanceTo(x, baseY, z) > (double) PARTICLE_VIEW_DISTANCE * PARTICLE_VIEW_DISTANCE) {
                    continue;
                }

                for (int yStep = -2; yStep <= 3; yStep++) {
                    serverWorld.spawnParticles(player, BORDER_PARTICLE, true, x, baseY + yStep, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    private static Vec3d computeOutsidePosition(ForbiddenBorderState state, Vec3d currentPos) {
        double dx = currentPos.x - state.getCenterX();
        double dz = currentPos.z - state.getCenterZ();

        if (dx == 0.0D && dz == 0.0D) {
            return new Vec3d(state.getCenterX() + state.getRadius() + PUSHBACK_DISTANCE, currentPos.y, state.getCenterZ());
        }

        double length = Math.sqrt(dx * dx + dz * dz);
        double scale = (state.getRadius() + PUSHBACK_DISTANCE) / length;
        return new Vec3d(state.getCenterX() + dx * scale, currentPos.y, state.getCenterZ() + dz * scale);
    }

    private static boolean canModifyAt(World world, net.minecraft.entity.player.PlayerEntity player, BlockPos pos) {
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) {
            return true;
        }

        if (player.isCreative() || player.isSpectator()) {
            return true;
        }

        ForbiddenBorderState state = getState(serverWorld.getServer());
        if (!state.isEnabled()) {
            return true;
        }

        boolean allowed = !state.isInside(pos.getX() + 0.5D, pos.getZ() + 0.5D);
        if (!allowed) {
            player.sendMessage(Text.literal("You cannot build inside the forbidden inner border."), true);
        }
        return allowed;
    }

    private static ForbiddenBorderState getState(MinecraftServer server) {
        PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
        PersistentState.Type<ForbiddenBorderState> type = new PersistentState.Type<>(
            ForbiddenBorderState::createDefault,
            ForbiddenBorderState::fromNbt,
            null
        );
        return stateManager.getOrCreate(type, ForbiddenBorderState.KEY);
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("border")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.literal("enable")
                .executes(context -> {
                    ForbiddenBorderState state = getState(context.getSource().getServer());
                    state.setEnabled(true);
                    context.getSource().sendFeedback(() -> Text.literal("Forbidden border enabled."), true);
                    return 1;
                }))
            .then(CommandManager.literal("disable")
                .executes(context -> {
                    ForbiddenBorderState state = getState(context.getSource().getServer());
                    state.setEnabled(false);
                    context.getSource().sendFeedback(() -> Text.literal("Forbidden border disabled."), true);
                    return 1;
                }))
            .then(CommandManager.literal("center")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    ForbiddenBorderState state = getState(context.getSource().getServer());
                    state.setCenter(player.getX(), player.getZ());
                    context.getSource().sendFeedback(() -> Text.literal(String.format("Forbidden border center set to %.1f %.1f", state.getCenterX(), state.getCenterZ())), true);
                    return 1;
                })
                .then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
                    .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
                        .executes(context -> {
                            double x = DoubleArgumentType.getDouble(context, "x");
                            double z = DoubleArgumentType.getDouble(context, "z");
                            ForbiddenBorderState state = getState(context.getSource().getServer());
                            state.setCenter(x, z);
                            context.getSource().sendFeedback(() -> Text.literal(String.format("Forbidden border center set to %.1f %.1f", x, z)), true);
                            return 1;
                        }))))
            .then(CommandManager.literal("radius")
                .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(1.0D, 1000000.0D))
                    .executes(context -> {
                        double value = DoubleArgumentType.getDouble(context, "value");
                        ForbiddenBorderState state = getState(context.getSource().getServer());
                        state.setRadius(value);
                        context.getSource().sendFeedback(() -> Text.literal(String.format("Forbidden border radius set to %.1f", value)), true);
                        return 1;
                    })))
            .then(CommandManager.literal("status")
                .executes(context -> {
                    ForbiddenBorderState state = getState(context.getSource().getServer());
                    String enabledText = state.isEnabled() ? "enabled" : "disabled";
                    context.getSource().sendFeedback(() -> Text.literal(String.format("Forbidden border is %s | center: %.1f %.1f | radius: %.1f", enabledText, state.getCenterX(), state.getCenterZ(), state.getRadius())), false);
                    return 1;
                }))
        );
    }
}
