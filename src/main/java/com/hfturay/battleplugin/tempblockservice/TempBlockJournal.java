package com.hfturay.battleplugin.tempblockservice;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * A file that lists every block the service has changed and what it was before. If the server crashes or is
 * killed while temporary blocks exist, the next start reads this file and puts the world back.
 *
 * Written whole (to a temp file, then moved over the real one), so a crash never leaves half a file.
 */
public final class TempBlockJournal {
    private final File file;
    private final Logger logger;
    private final Function<BlockData, String> encode;
    private final Function<String, BlockData> decode;

    public TempBlockJournal(File file, Logger logger, Function<BlockData, String> encode, Function<String, BlockData> decode) {
        this.file = file;
        this.logger = logger;
        this.encode = encode;
        this.decode = decode;
    }

    /** Writes the originals. An empty map removes the file. Failures are logged, never thrown. */
    public void save(Map<BlockPos, BlockData> originals) {
        if (originals.isEmpty()) {
            delete();
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockData> entry : originals.entrySet()) {
            BlockPos p = entry.getKey();
            lines.add(p.world() + "|" + p.x() + "|" + p.y() + "|" + p.z() + "|" + encode.apply(entry.getValue()));
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("blocks", lines);

        File temp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            Files.createDirectories(file.getParentFile().toPath());
            config.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not write the temporary-block journal. A crash now would leave blocks behind.", e);
        }
    }

    /** What the file says to restore. Empty if there is no file or it cannot be read. */
    public Map<BlockPos, BlockData> load() {
        Map<BlockPos, BlockData> result = new LinkedHashMap<>();
        if (!file.isFile()) {
            return result;
        }
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not read the temporary-block journal; blocks from a crash may be left in the world.", e);
            return result;
        }
        for (String line : config.getStringList("blocks")) {
            String[] part = line.split("\\|", 5);
            try {
                BlockPos pos = new BlockPos(UUID.fromString(part[0]), Integer.parseInt(part[1]),
                    Integer.parseInt(part[2]), Integer.parseInt(part[3]));
                result.put(pos, decode.apply(part[4]));
            } catch (RuntimeException e) {
                logger.warning("Skipping an unreadable journal line: " + line);
            }
        }
        return result;
    }

    public void delete() {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not delete the temporary-block journal", e);
        }
    }
}
