package net.kdy05.skyairban;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener {
    private final Map<Chunk, Optional<byte[]>> cache = new HashMap<>();
    private NamespacedKey skyAirKey;

    @Override
    public void onEnable() {
        skyAirKey = new NamespacedKey(this, "initial_sky_air");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        Chunk chunk = event.getChunk();
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (container.has(skyAirKey)) {
            return;
        }
        World world = chunk.getWorld();
        byte[] data = SkyAir.capture(world.getMinHeight(), world.getMaxHeight(),
                (x, y, z) -> chunk.getBlock(x, y, z).getType().isAir());
        container.set(skyAirKey, PersistentDataType.BYTE_ARRAY, data);
        cache.put(chunk, Optional.of(data));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        cache.remove(event.getChunk());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (isBlocked(event.getBlockPlaced()) || hasBlockedPart(event)) {
            event.setCancelled(true);
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.7f);
        }
    }

    private boolean hasBlockedPart(BlockPlaceEvent event) {
        if (event instanceof BlockMultiPlaceEvent multi) {
            for (BlockState state : multi.getReplacedBlockStates()) {
                if (isBlocked(state.getBlock())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isBlocked(Block block) {
        Chunk chunk = block.getChunk();
        Optional<byte[]> entry = cache.computeIfAbsent(chunk, this::readChunk);
        if (entry.isEmpty()) {
            return false;
        }
        byte[] data = entry.get();
        if (data.length == 0) {
            return true;
        }
        World world = chunk.getWorld();
        return SkyAir.contains(data, world.getMinHeight(), world.getMaxHeight(),
                block.getX(), block.getY(), block.getZ());
    }

    private Optional<byte[]> readChunk(Chunk chunk) {
        PersistentDataContainer container = chunk.getPersistentDataContainer();
        if (!container.has(skyAirKey)) {
            return Optional.empty();
        }
        byte[] data = container.get(skyAirKey, PersistentDataType.BYTE_ARRAY);
        World world = chunk.getWorld();
        if (data == null || !SkyAir.valid(data, world.getMinHeight(), world.getMaxHeight())) {
            getLogger().severe("Invalid initial sky-air data in " + world.getName()
                    + " chunk " + chunk.getX() + "," + chunk.getZ()
                    + "; player placements in this chunk will be rejected.");
            return Optional.of(new byte[0]);
        }
        return Optional.of(data);
    }
}
