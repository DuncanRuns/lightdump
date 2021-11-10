package com.dariasc.lightdump;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.light.ChunkLightingView;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;

public class Lightdump implements ModInitializer {

    public static final String MOD_NAME = "Lightdump";
    public static Logger LOGGER = LogManager.getLogger();

    public static void log(Level level, String message) {
        LOGGER.log(level, "[" + MOD_NAME + "] " + message);
    }

    @Override
    public void onInitialize() {
        Command<ServerCommandSource> command = context -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            ChunkManager chunkManager = player.getServerWorld().getChunkManager();
            ChunkLightingView block = chunkManager.getLightingProvider().get(LightType.BLOCK);
            ChunkLightingView sky = chunkManager.getLightingProvider().get(LightType.SKY);

            HashMap<Integer, Integer> dump = new HashMap<>();

            log(Level.INFO, "dumping region");
            for (int x = 0; x < 256; x++) {
                for (int z = 0; z < 256; z++) {
                    for (int y = 0; y < 128; y++) {
                        int blockLevel = block.getLightLevel(new BlockPos(x, y, z));
                        int skyLevel = sky.getLightLevel(new BlockPos(x, y, z));
                        int key = (x << 16) + (y << 8) + z;
                        dump.put(key, (blockLevel << 4) + skyLevel);
                    }
                }
            }
            log(Level.INFO, "finished dumping " + dump.size() + " light values");

            String name = "vanilla.light";
            for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                if (mod.getMetadata().getName().equals("Starlight")) {
                    name = "starlight.light";
                }
            }

            File file = new File(name);
            try {
                BufferedWriter fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))) ;
                dump.forEach((key, value) -> {
                    if (value == 0) {
                        return;
                    }
                    try {
                        fileWriter.write(key + " " + value + System.lineSeparator());
                    } catch (IOException exception) {
                        log(Level.ERROR, "failed to write dump to file");
                    }
                });
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException exception) {
                log(Level.ERROR, "failed to write dump to file");
            }

            return 0;
        };

        CommandRegistrationCallback.EVENT.register(((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("lightdump").executes(command));
        }));
    }
}
