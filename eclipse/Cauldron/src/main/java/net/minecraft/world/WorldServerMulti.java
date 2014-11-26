package net.minecraft.world;

import net.minecraft.logging.ILogAgent;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.ISaveHandler;

public class WorldServerMulti extends WorldServer
{
    // CraftBukkit start - Changed signature
    public WorldServerMulti(MinecraftServer minecraftserver, ISaveHandler isavehandler, String s, int i, WorldSettings worldsettings, WorldServer worldserver, Profiler profiler, ILogAgent ilogagent, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen)
    {
        super(minecraftserver, isavehandler, s, i, worldsettings, profiler, ilogagent, env, gen);
        // CraftBukkit end
        this.mapStorage = worldserver.mapStorage;
        this.worldScoreboard = worldserver.getScoreboard();
        // this.worldData = new SecondaryWorldData(worldserver.getWorldData()); // CraftBukkit - use unique worlddata
    }

    // Cauldron start - vanilla compatibility
    public WorldServerMulti(MinecraftServer minecraftserver, ISaveHandler isavehandler, String s, int i, WorldSettings worldsettings, WorldServer worldserver, Profiler profiler, ILogAgent ilogagent) {
        super(minecraftserver, isavehandler, s, i, ilogagent, worldsettings, profiler);
        this.mapStorage = worldserver.mapStorage;
        this.worldInfo = new DerivedWorldInfo(worldserver.getWorldInfo());
    }
    // Cauldron end

    /**
     * Saves the chunks to disk.
     */
    // Cauldron start - we handle all saving including perWorldStorage in WorldServer.saveLevel. This needs to be disabled since we follow
    // bukkit's world saving methods by using a seperate save handler for each world. Each world folder needs to generate a corresponding 
    // level.dat for plugins that require it such as MultiWorld.
    /*
    protected void saveLevel() throws MinecraftException
    {
        this.perWorldStorage.saveAllData();
    }
    */
    // Cauldron end
}