package net.minecraft.world;

import com.google.common.collect.Lists;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEventData;
import net.minecraft.block.material.Material;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.INpc;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;
import net.minecraft.profiler.Profiler;
import net.minecraft.scoreboard.ScoreboardSaveData;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Vec3;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.feature.WorldGeneratorBonusChest;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.ChestGenHooks;
import static net.minecraftforge.common.ChestGenHooks.BONUS_CHEST;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.BlockJukebox;
import net.minecraft.tileentity.*;
import org.bukkit.WeatherType;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.util.LongHash;

import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

public class WorldServer extends World
{
    // CraftBukkit end

    private static final Logger logger = LogManager.getLogger();
    private final MinecraftServer mcServer;
    public EntityTracker theEntityTracker; // CraftBukkit - private final -> public
    private final PlayerManager thePlayerManager;
    private Set pendingTickListEntriesHashSet;
    private TreeSet pendingTickListEntriesTreeSet;
    public ChunkProviderServer theChunkProviderServer;
    public boolean levelSaving;
    private boolean allPlayersSleeping;
    private int updateEntityTick;
    private final Teleporter worldTeleporter;
    private final SpawnerAnimals animalSpawner = new SpawnerAnimals();
    private WorldServer.ServerBlockEventList[] field_147490_S = new WorldServer.ServerBlockEventList[] {new WorldServer.ServerBlockEventList(null), new WorldServer.ServerBlockEventList(null)};
    private int blockEventCacheIndex;
    public static final WeightedRandomChestContent[] bonusChestContent = new WeightedRandomChestContent[] {new WeightedRandomChestContent(Items.stick, 0, 1, 3, 10), new WeightedRandomChestContent(Item.getItemFromBlock(Blocks.planks), 0, 1, 3, 10), new WeightedRandomChestContent(Item.getItemFromBlock(Blocks.log), 0, 1, 3, 10), new WeightedRandomChestContent(Items.stone_axe, 0, 1, 1, 3), new WeightedRandomChestContent(Items.wooden_axe, 0, 1, 1, 5), new WeightedRandomChestContent(Items.stone_pickaxe, 0, 1, 1, 3), new WeightedRandomChestContent(Items.wooden_pickaxe, 0, 1, 1, 5), new WeightedRandomChestContent(Items.apple, 0, 2, 3, 5), new WeightedRandomChestContent(Items.bread, 0, 2, 3, 3), new WeightedRandomChestContent(Item.getItemFromBlock(Blocks.log2), 0, 1, 3, 10)};
    private List pendingTickListEntriesThisTick = new ArrayList();
    private IntHashMap entityIdMap;
    private static final String __OBFID = "CL_00001437";

    // CraftBukkit start
    public final int dimension;

    /** Stores the recently processed (lighting) chunks */
    protected Set<ChunkCoordIntPair> doneChunks = new HashSet<ChunkCoordIntPair>();
    public List<Teleporter> customTeleporters = new ArrayList<Teleporter>();

    public WorldServer(MinecraftServer p_i45284_1_, ISaveHandler p_i45284_2_, String p_i45284_3_, int p_i45284_4_, WorldSettings p_i45284_5_, Profiler p_i45284_6_)
    {
        super(p_i45284_2_, p_i45284_3_, p_i45284_5_, WorldProvider.getProviderForDimension(p_i45284_4_), p_i45284_6_);
        this.dimension = p_i45284_4_;
        this.mcServer = p_i45284_1_;
        this.theEntityTracker = new EntityTracker(this);
        this.thePlayerManager = new PlayerManager(this);

        if (this.entityIdMap == null)
        {
            this.entityIdMap = new IntHashMap();
        }

        if (this.pendingTickListEntriesHashSet == null)
        {
            this.pendingTickListEntriesHashSet = new HashSet();
        }

        if (this.pendingTickListEntriesTreeSet == null)
        {
            this.pendingTickListEntriesTreeSet = new TreeSet();
        }

        this.worldTeleporter = new Teleporter(this);
        this.worldScoreboard = new ServerScoreboard(p_i45284_1_);
        ScoreboardSaveData scoreboardsavedata = (ScoreboardSaveData)this.mapStorage.loadData(ScoreboardSaveData.class, "scoreboard");

        if (scoreboardsavedata == null)
        {
            scoreboardsavedata = new ScoreboardSaveData();
            this.mapStorage.setData("scoreboard", scoreboardsavedata);
        }

        scoreboardsavedata.func_96499_a(this.worldScoreboard);
        ((ServerScoreboard)this.worldScoreboard).func_96547_a(scoreboardsavedata);
    }

    // Add env and gen to constructor
    public WorldServer(MinecraftServer p_i45284_1_, ISaveHandler p_i45284_2_, String p_i45284_3_, int p_i45284_4_, WorldSettings p_i45284_5_, Profiler p_i45284_6_, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen)
    {
        super(p_i45284_2_, p_i45284_3_, p_i45284_5_, WorldProvider.getProviderForDimension(p_i45284_4_), p_i45284_6_, gen, env);
        this.dimension = p_i45284_4_;
        this.pvpMode = p_i45284_1_.isPVPEnabled();
        // CraftBukkit end
        this.mcServer = p_i45284_1_;
        this.theEntityTracker = new EntityTracker(this);
        this.thePlayerManager = new PlayerManager(this, spigotConfig.viewDistance); // Spigot

        if (this.entityIdMap == null)
        {
            this.entityIdMap = new IntHashMap();
        }

        if (this.pendingTickListEntriesHashSet == null)
        {
            this.pendingTickListEntriesHashSet = new HashSet();
        }

        if (this.pendingTickListEntriesTreeSet == null)
        {
            this.pendingTickListEntriesTreeSet = new TreeSet();
        }

        this.worldTeleporter = new org.bukkit.craftbukkit.CraftTravelAgent(this); // CraftBukkit
        this.worldScoreboard = new ServerScoreboard(p_i45284_1_);
        ScoreboardSaveData scoreboardsavedata = (ScoreboardSaveData)this.mapStorage.loadData(ScoreboardSaveData.class, "scoreboard");

        if (scoreboardsavedata == null)
        {
            scoreboardsavedata = new ScoreboardSaveData();
            this.mapStorage.setData("scoreboard", scoreboardsavedata);
        }

        if (!(this instanceof WorldServerMulti)) //Forge: We fix the global mapStorage, which causes us to share scoreboards early. So don't associate the save data with the temporary scoreboard
        {
            scoreboardsavedata.func_96499_a(this.worldScoreboard);
        }
        ((ServerScoreboard)this.worldScoreboard).func_96547_a(scoreboardsavedata);
        DimensionManager.setWorld(p_i45284_4_, this);
    }

    public WorldServer(MinecraftServer minecraftServer, ISaveHandler saveHandler, String par2String, WorldProvider provider, WorldSettings par4WorldSettings, Profiler theProfiler)
    {
        super(saveHandler, par2String, provider, par4WorldSettings, theProfiler);
        this.dimension = provider.dimensionId;
        this.pvpMode = minecraftServer.isPVPEnabled();
        this.mcServer = minecraftServer;
        this.theEntityTracker = null;
        this.thePlayerManager = null;
        this.worldTeleporter = null;
    }

    private boolean canSpawn(int x, int z)
    {
        if (this.generator != null)
        {
            return this.generator.canSpawn(this.getWorld(), x, z);
        }
        else
        {
            return this.provider.canCoordinateBeSpawn(x, z);
        }
    }
    // CraftBukkit end

    public void tick()
    {
        super.tick();

        if (this.getWorldInfo().isHardcoreModeEnabled() && this.difficultySetting != EnumDifficulty.HARD)
        {
            this.difficultySetting = EnumDifficulty.HARD;
        }

        this.provider.worldChunkMgr.cleanupCache();

        if (this.areAllPlayersAsleep())
        {
            if (this.getGameRules().getGameRuleBooleanValue("doDaylightCycle"))
            {
                long i = this.worldInfo.getWorldTime() + 24000L;
                this.worldInfo.setWorldTime(i - i % 24000L);
            }

            this.wakeAllPlayers();
        }

        this.theProfiler.startSection("mobSpawner");
        // CraftBukkit start - Only call spawner if we have players online and the world allows for mobs or animals
        long time = this.worldInfo.getWorldTotalTime();

        if (this.getGameRules().getGameRuleBooleanValue("doMobSpawning") && (this.spawnHostileMobs || this.spawnPeacefulMobs) && (this instanceof WorldServer && this.playerEntities.size() > 0))
        {
            timings.mobSpawn.startTiming(); // Spigot
            this.animalSpawner.findChunksForSpawning(this, this.spawnHostileMobs && (this.ticksPerMonsterSpawns != 0 && time % this.ticksPerMonsterSpawns == 0L), this.spawnPeacefulMobs && (this.ticksPerAnimalSpawns != 0 && time % this.ticksPerAnimalSpawns == 0L), this.worldInfo.getWorldTotalTime() % 400L == 0L);
            timings.mobSpawn.stopTiming(); // Spigot
        }

        // CraftBukkit end
        timings.doChunkUnload.startTiming(); // Spigot
        this.theProfiler.endStartSection("chunkSource");
        this.chunkProvider.unloadQueuedChunks();
        int j = this.calculateSkylightSubtracted(1.0F);

        if (j != this.skylightSubtracted)
        {
            this.skylightSubtracted = j;
        }

        this.worldInfo.incrementTotalWorldTime(this.worldInfo.getWorldTotalTime() + 1L);

        if (this.getGameRules().getGameRuleBooleanValue("doDaylightCycle"))
        {
            this.worldInfo.setWorldTime(this.worldInfo.getWorldTime() + 1L);
        }

        timings.doChunkUnload.stopTiming(); // Spigot
        this.theProfiler.endStartSection("tickPending");
        timings.doTickPending.startTiming(); // Spigot
        this.tickUpdates(false);
        timings.doTickPending.stopTiming(); // Spigot
        this.theProfiler.endStartSection("tickBlocks");
        timings.doTickTiles.startTiming(); // Spigot
        this.func_147456_g();
        timings.doTickTiles.stopTiming(); // Spigot
        this.theProfiler.endStartSection("chunkMap");
        timings.doChunkMap.startTiming(); // Spigot
        this.thePlayerManager.updatePlayerInstances();
        timings.doChunkMap.stopTiming(); // Spigot
        this.theProfiler.endStartSection("village");
        timings.doVillages.startTiming(); // Spigot
        this.villageCollectionObj.tick();
        this.villageSiegeObj.tick();
        timings.doVillages.stopTiming(); // Spigot
        this.theProfiler.endStartSection("portalForcer");
        timings.doPortalForcer.startTiming(); // Spigot
        this.worldTeleporter.removeStalePortalLocations(this.getTotalWorldTime());
        for (Teleporter tele : customTeleporters)
        {
            tele.removeStalePortalLocations(getTotalWorldTime());
        }
        timings.doPortalForcer.stopTiming(); // Spigot
        this.theProfiler.endSection();
        timings.doSounds.startTiming(); // Spigot
        this.func_147488_Z();
        timings.doSounds.stopTiming(); // Spigot

        timings.doChunkGC.startTiming(); // Spigot
        this.getWorld().processChunkGC(); // CraftBukkit
        timings.doChunkGC.stopTiming(); // Spigot
    }

    public BiomeGenBase.SpawnListEntry spawnRandomCreature(EnumCreatureType p_73057_1_, int p_73057_2_, int p_73057_3_, int p_73057_4_)
    {
        List list = this.getChunkProvider().getPossibleCreatures(p_73057_1_, p_73057_2_, p_73057_3_, p_73057_4_);
        list = ForgeEventFactory.getPotentialSpawns(this, p_73057_1_, p_73057_2_, p_73057_3_, p_73057_4_, list);
        return list != null && !list.isEmpty() ? (BiomeGenBase.SpawnListEntry)WeightedRandom.getRandomItem(this.rand, list) : null;
    }

    public void updateAllPlayersSleepingFlag()
    {
        this.allPlayersSleeping = !this.playerEntities.isEmpty();
        Iterator iterator = this.playerEntities.iterator();

        while (iterator.hasNext())
        {
            EntityPlayer entityplayer = (EntityPlayer)iterator.next();

            if (!entityplayer.isPlayerSleeping() && !entityplayer.fauxSleeping)   // CraftBukkit
            {
                this.allPlayersSleeping = false;
                break;
            }
        }
    }

    protected void wakeAllPlayers()
    {
        this.allPlayersSleeping = false;
        Iterator iterator = this.playerEntities.iterator();

        while (iterator.hasNext())
        {
            EntityPlayer entityplayer = (EntityPlayer)iterator.next();

            if (entityplayer.isPlayerSleeping())
            {
                entityplayer.wakeUpPlayer(false, false, true);
            }
        }

        this.resetRainAndThunder();
    }

    private void resetRainAndThunder()
    {
        // CraftBukkit start
        WeatherChangeEvent weather = new WeatherChangeEvent(this.getWorld(), false);
        this.getServer().getPluginManager().callEvent(weather);
        ThunderChangeEvent thunder = new ThunderChangeEvent(this.getWorld(), false);
        this.getServer().getPluginManager().callEvent(thunder);

        if (!weather.isCancelled())
        {
            this.worldInfo.setRainTime(0);
            this.worldInfo.setRaining(false);
        }

        if (!thunder.isCancelled())
        {
            this.worldInfo.setThunderTime(0);
            this.worldInfo.setThundering(false);
        }

        // CraftBukkit end
        if (!weather.isCancelled() && !thunder.isCancelled()) provider.resetRainAndThunder(); // Cauldron
    }

    public boolean areAllPlayersAsleep()
    {
        if (this.allPlayersSleeping && !this.isRemote)
        {
            Iterator iterator = this.playerEntities.iterator();
            // CraftBukkit - This allows us to assume that some people are in bed but not really, allowing time to pass in spite of AFKers
            boolean foundActualSleepers = false;
            EntityPlayer entityplayer;

            do
            {
                if (!iterator.hasNext())
                {
                    return foundActualSleepers; // CraftBukkit
                }

                entityplayer = (EntityPlayer) iterator.next();

                // CraftBukkit start
                if (entityplayer.isPlayerFullyAsleep())
                {
                    foundActualSleepers = true;
                }
            }
            while (entityplayer.isPlayerFullyAsleep() || entityplayer.fauxSleeping);

            // CraftBukkit end
            return false;
        }
        else
        {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    public void setSpawnLocation()
    {
        if (this.worldInfo.getSpawnY() <= 0)
        {
            this.worldInfo.setSpawnY(64);
        }

        int i = this.worldInfo.getSpawnX();
        int j = this.worldInfo.getSpawnZ();
        int k = 0;

        while (this.getTopBlock(i, j).getMaterial() == Material.air)
        {
            i += this.rand.nextInt(8) - this.rand.nextInt(8);
            j += this.rand.nextInt(8) - this.rand.nextInt(8);
            ++k;

            if (k == 10000)
            {
                break;
            }
        }

        this.worldInfo.setSpawnX(i);
        this.worldInfo.setSpawnZ(j);
    }

    protected void func_147456_g()
    {
        super.func_147456_g();
        int i = 0;
        int j = 0;
        // Iterator iterator = this.activeChunkSet.iterator();

        // Spigot start
        for (gnu.trove.iterator.TLongShortIterator iter = activeChunkSet_CB.iterator(); iter.hasNext();)
        {
            iter.advance();
            long chunkCoord = iter.key();
            int chunkX = World.keyToX(chunkCoord);
            int chunkZ = World.keyToZ(chunkCoord);
            // If unloaded, or in process of being unloaded, drop it
            if ((!this.chunkExists(chunkX, chunkZ)) || (this.theChunkProviderServer.chunksToUnload.contains(chunkX, chunkZ)))
            {
                activeChunkSet.remove(new ChunkCoordIntPair(chunkX, chunkZ)); // Cauldron - vanilla compatibility
                iter.remove();
                continue;
            }
            // Spigot end
            // ChunkCoordIntPair chunkcoordintpair = (ChunkCoordIntPair) iterator.next();
            int k = chunkX * 16;
            int l = chunkZ * 16;

            this.theProfiler.startSection("getChunk");
            Chunk chunk = this.getChunkFromChunkCoords(chunkX, chunkZ);
            this.func_147467_a(k, l, chunk);
            this.theProfiler.endStartSection("tickChunk");
            chunk.func_150804_b(false);
            this.theProfiler.endStartSection("thunder");
            int i1;
            int j1;
            int k1;
            int l1;

            if (provider.canDoLightning(chunk) && this.rand.nextInt(100000) == 0 && this.isRaining() && this.isThundering())
            {
                this.updateLCG = this.updateLCG * 3 + 1013904223;
                i1 = this.updateLCG >> 2;
                j1 = k + (i1 & 15);
                k1 = l + (i1 >> 8 & 15);
                l1 = this.getPrecipitationHeight(j1, k1);

                if (this.canLightningStrikeAt(j1, l1, k1))
                {
                    this.addWeatherEffect(new EntityLightningBolt(this, (double)j1, (double)l1, (double)k1));
                }
            }

            this.theProfiler.endStartSection("iceandsnow");

            if (provider.canDoRainSnowIce(chunk) && this.rand.nextInt(16) == 0)
            {
                this.updateLCG = this.updateLCG * 3 + 1013904223;
                i1 = this.updateLCG >> 2;
                j1 = i1 & 15;
                k1 = i1 >> 8 & 15;
                l1 = this.getPrecipitationHeight(j1 + k, k1 + l);

                if (this.isBlockFreezableNaturally(j1 + k, l1 - 1, k1 + l))
                {
                    // CraftBukkit start
                    BlockState blockState = this.getWorld().getBlockAt(j1 + k, l1 - 1, k1 + l).getState();
                    blockState.setTypeId(Block.getIdFromBlock(Blocks.ice));
                    BlockFormEvent iceBlockForm = new BlockFormEvent(blockState.getBlock(), blockState);
                    this.getServer().getPluginManager().callEvent(iceBlockForm);

                    if (!iceBlockForm.isCancelled())
                    {
                        blockState.update(true);
                    }

                    // CraftBukkit end
                }

                if (this.isRaining() && this.func_147478_e(j1 + k, l1, k1 + l, true))
                {
                    // CraftBukkit start
                    BlockState blockState = this.getWorld().getBlockAt(j1 + k, l1, k1 + l).getState();
                    blockState.setTypeId(Block.getIdFromBlock(Blocks.snow_layer));
                    BlockFormEvent snow = new BlockFormEvent(blockState.getBlock(), blockState);
                    this.getServer().getPluginManager().callEvent(snow);

                    if (!snow.isCancelled())
                    {
                        blockState.update(true);
                    }

                    // CraftBukkit end
                }

                if (this.isRaining())
                {
                    BiomeGenBase biomegenbase = this.getBiomeGenForCoords(j1 + k, k1 + l);

                    if (biomegenbase.canSpawnLightningBolt())
                    {
                        this.getBlock(j1 + k, l1 - 1, k1 + l).fillWithRain(this, j1 + k, l1 - 1, k1 + l);
                    }
                }
            }

            this.theProfiler.endStartSection("tickBlocks");
            ExtendedBlockStorage[] aextendedblockstorage = chunk.getBlockStorageArray();
            j1 = aextendedblockstorage.length;

            for (k1 = 0; k1 < j1; ++k1)
            {
                ExtendedBlockStorage extendedblockstorage = aextendedblockstorage[k1];

                if (extendedblockstorage != null && extendedblockstorage.getNeedsRandomTick())
                {
                    for (int i2 = 0; i2 < 3; ++i2)
                    {
                        this.updateLCG = this.updateLCG * 3 + 1013904223;
                        int j2 = this.updateLCG >> 2;
                        int k2 = j2 & 15;
                        int l2 = j2 >> 8 & 15;
                        int i3 = j2 >> 16 & 15;
                        ++j;
                        Block block = extendedblockstorage.getBlockByExtId(k2, i3, l2);

                        if (block.getTickRandomly())
                        {
                            ++i;
                            this.growthOdds = (iter.value() < 1) ? this.modifiedOdds : 100; // Spigot - grow fast if no players are in this chunk (value = player count)
                            block.updateTick(this, k2 + k, i3 + extendedblockstorage.getYLocation(), l2 + l, this.rand);
                        }
                    }
                }
            }

            this.theProfiler.endSection();
        }

        // Spigot Start
        if (this.getSpigotConfig().clearChunksOnTick) // Cauldron
        {
            activeChunkSet_CB.clear();
            activeChunkSet.clear(); // Cauldron
        }

        // Spigot End
    }

    public boolean isBlockTickScheduledThisTick(int p_147477_1_, int p_147477_2_, int p_147477_3_, Block p_147477_4_)
    {
        NextTickListEntry nextticklistentry = new NextTickListEntry(p_147477_1_, p_147477_2_, p_147477_3_, p_147477_4_);
        return this.pendingTickListEntriesThisTick.contains(nextticklistentry);
    }

    public void scheduleBlockUpdate(int p_147464_1_, int p_147464_2_, int p_147464_3_, Block p_147464_4_, int p_147464_5_)
    {
        this.scheduleBlockUpdateWithPriority(p_147464_1_, p_147464_2_, p_147464_3_, p_147464_4_, p_147464_5_, 0);
    }

    public void scheduleBlockUpdateWithPriority(int p_147454_1_, int p_147454_2_, int p_147454_3_, Block p_147454_4_, int p_147454_5_, int p_147454_6_)
    {
        NextTickListEntry nextticklistentry = new NextTickListEntry(p_147454_1_, p_147454_2_, p_147454_3_, p_147454_4_);
        //Keeping here as a note for future when it may be restored.
        //boolean isForced = getPersistentChunks().containsKey(new ChunkCoordIntPair(nextticklistentry.xCoord >> 4, nextticklistentry.zCoord >> 4));
        //byte b0 = isForced ? 0 : 8;
        byte b0 = 0;

        if (this.scheduledUpdatesAreImmediate && p_147454_4_.getMaterial() != Material.air)
        {
            if (p_147454_4_.func_149698_L())
            {
                b0 = 8;

                if (this.checkChunksExist(nextticklistentry.xCoord - b0, nextticklistentry.yCoord - b0, nextticklistentry.zCoord - b0, nextticklistentry.xCoord + b0, nextticklistentry.yCoord + b0, nextticklistentry.zCoord + b0))
                {
                    Block block1 = this.getBlock(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);

                    if (block1.getMaterial() != Material.air && block1 == nextticklistentry.func_151351_a())
                    {
                        block1.updateTick(this, nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, this.rand);
                    }
                }

                return;
            }

            p_147454_5_ = 1;
        }

        if (this.checkChunksExist(p_147454_1_ - b0, p_147454_2_ - b0, p_147454_3_ - b0, p_147454_1_ + b0, p_147454_2_ + b0, p_147454_3_ + b0))
        {
            if (p_147454_4_.getMaterial() != Material.air)
            {
                nextticklistentry.setScheduledTime((long)p_147454_5_ + this.worldInfo.getWorldTotalTime());
                nextticklistentry.setPriority(p_147454_6_);
            }

            if (!this.pendingTickListEntriesHashSet.contains(nextticklistentry))
            {
                this.pendingTickListEntriesHashSet.add(nextticklistentry);
                this.pendingTickListEntriesTreeSet.add(nextticklistentry);
            }
        }
    }

    public void func_147446_b(int p_147446_1_, int p_147446_2_, int p_147446_3_, Block p_147446_4_, int p_147446_5_, int p_147446_6_)
    {
        NextTickListEntry nextticklistentry = new NextTickListEntry(p_147446_1_, p_147446_2_, p_147446_3_, p_147446_4_);
        nextticklistentry.setPriority(p_147446_6_);

        if (p_147446_4_.getMaterial() != Material.air)
        {
            nextticklistentry.setScheduledTime((long)p_147446_5_ + this.worldInfo.getWorldTotalTime());
        }

        if (!this.pendingTickListEntriesHashSet.contains(nextticklistentry))
        {
            this.pendingTickListEntriesHashSet.add(nextticklistentry);
            this.pendingTickListEntriesTreeSet.add(nextticklistentry);
        }
    }

    public void updateEntities()
    {
        if (this.playerEntities.isEmpty() && getPersistentChunks().isEmpty()) // Cauldron Use Forge logic here
        {
            if (this.updateEntityTick++ >= 1200)
            {
                return;
            }
        }
        else
        {
            this.resetUpdateEntityTick();
        }

        super.updateEntities();
    }

    public void resetUpdateEntityTick()
    {
        this.updateEntityTick = 0;
    }

    public boolean tickUpdates(boolean p_72955_1_)
    {
        int i = this.pendingTickListEntriesTreeSet.size();

        if (i != this.pendingTickListEntriesHashSet.size())
        {
            throw new IllegalStateException("TickNextTick list out of synch");
        }
        else
        {
            if (i > 1000)
            {
                // CraftBukkit start - If the server has too much to process over time, try to alleviate that
                if (i > 20 * 1000)
                {
                    i = i / 20;
                }
                else
                {
                    i = 1000;
                }

                // CraftBukkit end
            }

            this.theProfiler.startSection("cleaning");
            NextTickListEntry nextticklistentry;

            for (int j = 0; j < i; ++j)
            {
                nextticklistentry = (NextTickListEntry)this.pendingTickListEntriesTreeSet.first();

                if (!p_72955_1_ && nextticklistentry.scheduledTime > this.worldInfo.getWorldTotalTime())
                {
                    break;
                }

                this.pendingTickListEntriesTreeSet.remove(nextticklistentry);
                this.pendingTickListEntriesHashSet.remove(nextticklistentry);
                this.pendingTickListEntriesThisTick.add(nextticklistentry);
            }

            this.theProfiler.endSection();
            this.theProfiler.startSection("ticking");
            Iterator iterator = this.pendingTickListEntriesThisTick.iterator();

            while (iterator.hasNext())
            {
                nextticklistentry = (NextTickListEntry)iterator.next();
                iterator.remove();
                //Keeping here as a note for future when it may be restored.
                //boolean isForced = getPersistentChunks().containsKey(new ChunkCoordIntPair(nextticklistentry.xCoord >> 4, nextticklistentry.zCoord >> 4));
                //byte b0 = isForced ? 0 : 8;
                byte b0 = 0;

                if (this.checkChunksExist(nextticklistentry.xCoord - b0, nextticklistentry.yCoord - b0, nextticklistentry.zCoord - b0, nextticklistentry.xCoord + b0, nextticklistentry.yCoord + b0, nextticklistentry.zCoord + b0))
                {
                    Block block = this.getBlock(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);

                    if (block.getMaterial() != Material.air && Block.isEqualTo(block, nextticklistentry.func_151351_a()))
                    {
                        try
                        {
                            block.updateTick(this, nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, this.rand);
                        }
                        catch (Throwable throwable1)
                        {
                            CrashReport crashreport = CrashReport.makeCrashReport(throwable1, "Exception while ticking a block");
                            CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being ticked");
                            int k;

                            try
                            {
                                k = this.getBlockMetadata(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord);
                            }
                            catch (Throwable throwable)
                            {
                                k = -1;
                            }

                            CrashReportCategory.func_147153_a(crashreportcategory, nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, block, k);
                            throw new ReportedException(crashreport);
                        }
                    }
                }
                else
                {
                    this.scheduleBlockUpdate(nextticklistentry.xCoord, nextticklistentry.yCoord, nextticklistentry.zCoord, nextticklistentry.func_151351_a(), 0);
                }
            }

            this.theProfiler.endSection();
            this.pendingTickListEntriesThisTick.clear();
            return !this.pendingTickListEntriesTreeSet.isEmpty();
        }
    }

    public List getPendingBlockUpdates(Chunk p_72920_1_, boolean p_72920_2_)
    {
        ArrayList arraylist = null;
        ChunkCoordIntPair chunkcoordintpair = p_72920_1_.getChunkCoordIntPair();
        int i = (chunkcoordintpair.chunkXPos << 4) - 2;
        int j = i + 16 + 2;
        int k = (chunkcoordintpair.chunkZPos << 4) - 2;
        int l = k + 16 + 2;

        for (int i1 = 0; i1 < 2; ++i1)
        {
            Iterator iterator;

            if (i1 == 0)
            {
                iterator = this.pendingTickListEntriesTreeSet.iterator();
            }
            else
            {
                iterator = this.pendingTickListEntriesThisTick.iterator();

                if (!this.pendingTickListEntriesThisTick.isEmpty())
                {
                    logger.debug("toBeTicked = " + this.pendingTickListEntriesThisTick.size());
                }
            }

            while (iterator.hasNext())
            {
                NextTickListEntry nextticklistentry = (NextTickListEntry)iterator.next();

                if (nextticklistentry.xCoord >= i && nextticklistentry.xCoord < j && nextticklistentry.zCoord >= k && nextticklistentry.zCoord < l)
                {
                    if (p_72920_2_)
                    {
                        this.pendingTickListEntriesHashSet.remove(nextticklistentry);
                        iterator.remove();
                    }

                    if (arraylist == null)
                    {
                        arraylist = new ArrayList();
                    }

                    arraylist.add(nextticklistentry);
                }
            }
        }

        return arraylist;
    }

    public void updateEntityWithOptionalForce(Entity p_72866_1_, boolean p_72866_2_)
    {
        if (!this.mcServer.getCanSpawnAnimals() && (p_72866_1_ instanceof EntityAnimal || p_72866_1_ instanceof EntityWaterMob))
        {
            p_72866_1_.setDead();
        }

        if (!this.mcServer.getCanSpawnNPCs() && p_72866_1_ instanceof INpc)
        {
            p_72866_1_.setDead();
        }

        super.updateEntityWithOptionalForce(p_72866_1_, p_72866_2_);
    }

    protected IChunkProvider createChunkProvider()
    {
        IChunkLoader ichunkloader = this.saveHandler.getChunkLoader(this.provider);
        // Cauldron start - if provider is vanilla, proceed to create a bukkit compatible chunk generator
        if (this.provider.getClass().toString().length() <= 3 || this.provider.getClass().toString().contains("net.minecraft"))
        {
            // CraftBukkit start
            org.bukkit.craftbukkit.generator.InternalChunkGenerator gen;
    
            if (this.generator != null)
            {
                gen = new org.bukkit.craftbukkit.generator.CustomChunkGenerator(this, this.getSeed(), this.generator);
            }
            else if (this.provider instanceof WorldProviderHell)
            {
                gen = new org.bukkit.craftbukkit.generator.NetherChunkGenerator(this, this.getSeed());
            }
            else if (this.provider instanceof WorldProviderEnd)
            {
                gen = new org.bukkit.craftbukkit.generator.SkyLandsChunkGenerator(this, this.getSeed());
            }
            else
            {
                gen = new org.bukkit.craftbukkit.generator.NormalChunkGenerator(this, this.getSeed());
            }
            this.theChunkProviderServer = new ChunkProviderServer(this, ichunkloader, gen);
            // CraftBukkit end
        }
        else // custom provider, load normally for forge compatibility
        {
            this.theChunkProviderServer = new ChunkProviderServer(this, ichunkloader, this.provider.createChunkGenerator());
        }
        // Cauldron end
        return this.theChunkProviderServer;
    }

    public List func_147486_a(int p_147486_1_, int p_147486_2_, int p_147486_3_, int p_147486_4_, int p_147486_5_, int p_147486_6_)
    {
        ArrayList arraylist = new ArrayList();

        // CraftBukkit start - Get tile entities from chunks instead of world
        for (int chunkX = (p_147486_1_ >> 4); chunkX <= ((p_147486_4_ - 1) >> 4); chunkX++)
        {
            for (int chunkZ = (p_147486_3_ >> 4); chunkZ <= ((p_147486_6_ - 1) >> 4); chunkZ++)
            {
                Chunk chunk = getChunkFromChunkCoords(chunkX, chunkZ);

                if (chunk == null)
                {
                    continue;
                }

                for (Object te : chunk.chunkTileEntityMap.values())
                {
                    TileEntity tileentity = (TileEntity) te;

                    if ((tileentity.xCoord >= p_147486_1_) && (tileentity.yCoord >= p_147486_2_) && (tileentity.zCoord >= p_147486_3_) && (tileentity.xCoord < p_147486_4_) && (tileentity.yCoord < p_147486_5_) && (tileentity.zCoord < p_147486_6_))
                    {
                        arraylist.add(tileentity);
                    }
                }
            }
        }

        // CraftBukkit end
        return arraylist;
    }

    public boolean canMineBlock(EntityPlayer p_72962_1_, int p_72962_2_, int p_72962_3_, int p_72962_4_)
    {
        return super.canMineBlock(p_72962_1_, p_72962_2_, p_72962_3_, p_72962_4_);
    }

    public boolean canMineBlockBody(EntityPlayer par1EntityPlayer, int par2, int par3, int par4)
    {
        return !this.mcServer.isBlockProtected(this, par2, par3, par4, par1EntityPlayer);
    }

    protected void initialize(WorldSettings p_72963_1_)
    {
        if (this.entityIdMap == null)
        {
            this.entityIdMap = new IntHashMap();
        }

        if (this.pendingTickListEntriesHashSet == null)
        {
            this.pendingTickListEntriesHashSet = new HashSet();
        }

        if (this.pendingTickListEntriesTreeSet == null)
        {
            this.pendingTickListEntriesTreeSet = new TreeSet();
        }

        this.createSpawnPosition(p_72963_1_);
        super.initialize(p_72963_1_);
    }

    protected void createSpawnPosition(WorldSettings p_73052_1_)
    {
        if (!this.provider.canRespawnHere())
        {
            this.worldInfo.setSpawnPosition(0, this.provider.getAverageGroundLevel(), 0);
        }
        else
        {
            this.findingSpawnPoint = true;
            WorldChunkManager worldchunkmanager = this.provider.worldChunkMgr;
            List list = worldchunkmanager.getBiomesToSpawnIn();
            Random random = new Random(this.getSeed());
            ChunkPosition chunkposition = worldchunkmanager.findBiomePosition(0, 0, 256, list, random);
            int i = 0;
            int j = this.provider.getAverageGroundLevel();
            int k = 0;

            // CraftBukkit start
            if (this.generator != null)
            {
                Random rand = new Random(this.getSeed());
                org.bukkit.Location spawn = this.generator.getFixedSpawnLocation(((WorldServer) this).getWorld(), rand);

                if (spawn != null)
                {
                    if (spawn.getWorld() != ((WorldServer) this).getWorld())
                    {
                        throw new IllegalStateException("Cannot set spawn point for " + this.worldInfo.getWorldName() + " to be in another world (" + spawn.getWorld().getName() + ")");
                    }
                    else
                    {
                        this.worldInfo.setSpawnPosition(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
                        this.findingSpawnPoint = false;
                        return;
                    }
                }
            }

            // CraftBukkit end

            if (chunkposition != null)
            {
                i = chunkposition.chunkPosX;
                k = chunkposition.chunkPosZ;
            }
            else
            {
                logger.warn("Unable to find spawn biome");
            }

            int l = 0;

            while (!this.provider.canCoordinateBeSpawn(i, k))
            {
                i += random.nextInt(64) - random.nextInt(64);
                k += random.nextInt(64) - random.nextInt(64);
                ++l;

                if (l == 1000)
                {
                    break;
                }
            }

            this.worldInfo.setSpawnPosition(i, j, k);
            this.findingSpawnPoint = false;

            if (p_73052_1_.isBonusChestEnabled())
            {
                this.createBonusChest();
            }
        }
    }

    protected void createBonusChest()
    {
        WorldGeneratorBonusChest worldgeneratorbonuschest = new WorldGeneratorBonusChest(ChestGenHooks.getItems(BONUS_CHEST, rand), ChestGenHooks.getCount(BONUS_CHEST, rand));

        for (int i = 0; i < 10; ++i)
        {
            int j = this.worldInfo.getSpawnX() + this.rand.nextInt(6) - this.rand.nextInt(6);
            int k = this.worldInfo.getSpawnZ() + this.rand.nextInt(6) - this.rand.nextInt(6);
            int l = this.getTopSolidOrLiquidBlock(j, k) + 1;

            if (worldgeneratorbonuschest.generate(this, this.rand, j, l, k))
            {
                break;
            }
        }
    }

    public ChunkCoordinates getEntrancePortalLocation()
    {
        return this.provider.getEntrancePortalLocation();
    }

    public void saveAllChunks(boolean p_73044_1_, IProgressUpdate p_73044_2_) throws MinecraftException
    {
        if (this.chunkProvider.canSave())
        {
            if (p_73044_2_ != null)
            {
                p_73044_2_.displayProgressMessage("Saving level");
            }

            this.saveLevel();

            if (p_73044_2_ != null)
            {
                p_73044_2_.resetProgresAndWorkingMessage("Saving chunks");
            }

            this.chunkProvider.saveChunks(p_73044_1_, p_73044_2_);
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Save(this));
            ArrayList arraylist = Lists.newArrayList(this.theChunkProviderServer.func_152380_a());
            Iterator iterator = arraylist.iterator();

            while (iterator.hasNext())
            {
                Chunk chunk = (Chunk)iterator.next();

                if (chunk != null && !this.thePlayerManager.func_152621_a(chunk.xPosition, chunk.zPosition))
                {
                    this.theChunkProviderServer.unloadChunksIfNotNearSpawn(chunk.xPosition, chunk.zPosition);
                }
            }
        }
    }

    public void saveChunkData()
    {
        if (this.chunkProvider.canSave())
        {
            this.chunkProvider.saveExtraData();
        }
    }

    protected void saveLevel() throws MinecraftException
    {
        this.checkSessionLock();
        this.saveHandler.saveWorldInfoWithPlayer(this.worldInfo, this.mcServer.getConfigurationManager().getHostPlayerData());
        this.mapStorage.saveAllData();
        this.perWorldStorage.saveAllData();
    }

    public void onEntityAdded(Entity p_72923_1_)
    {
        super.onEntityAdded(p_72923_1_);
        this.entityIdMap.addKey(p_72923_1_.getEntityId(), p_72923_1_);
        Entity[] aentity = p_72923_1_.getParts();

        if (aentity != null)
        {
            for (int i = 0; i < aentity.length; ++i)
            {
                this.entityIdMap.addKey(aentity[i].getEntityId(), aentity[i]);
            }
        }
    }

    public void onEntityRemoved(Entity p_72847_1_)
    {
        super.onEntityRemoved(p_72847_1_);
        this.entityIdMap.removeObject(p_72847_1_.getEntityId());
        Entity[] aentity = p_72847_1_.getParts();

        if (aentity != null)
        {
            for (int i = 0; i < aentity.length; ++i)
            {
                this.entityIdMap.removeObject(aentity[i].getEntityId());
            }
        }
    }

    public Entity getEntityByID(int p_73045_1_)
    {
        return (Entity)this.entityIdMap.lookup(p_73045_1_);
    }

    public boolean addWeatherEffect(Entity p_72942_1_)
    {
        // Cauldron start - vanilla compatibility
        if (p_72942_1_ instanceof net.minecraft.entity.effect.EntityLightningBolt) 
        {
            // CraftBukkit start
            LightningStrikeEvent lightning = new LightningStrikeEvent(this.getWorld(), (org.bukkit.entity.LightningStrike) p_72942_1_.getBukkitEntity());
            this.getServer().getPluginManager().callEvent(lightning);

            if (lightning.isCancelled())
            {
                return false;
            }
            // CraftBukkit end
        }
        // Cauldron end
        if (super.addWeatherEffect(p_72942_1_))
        {
            this.mcServer.getConfigurationManager().sendToAllNear(p_72942_1_.posX, p_72942_1_.posY, p_72942_1_.posZ, 512.0D, this.provider.dimensionId, new S2CPacketSpawnGlobalEntity(p_72942_1_));
            return true;
        }
        else
        {
            return false;
        }
    }

    public void setEntityState(Entity p_72960_1_, byte p_72960_2_)
    {
        this.getEntityTracker().func_151248_b(p_72960_1_, new S19PacketEntityStatus(p_72960_1_, p_72960_2_));
    }

    public Explosion newExplosion(Entity p_72885_1_, double p_72885_2_, double p_72885_4_, double p_72885_6_, float p_72885_8_, boolean p_72885_9_, boolean p_72885_10_)
    {
        // CraftBukkit start
        Explosion explosion = super.newExplosion(p_72885_1_, p_72885_2_, p_72885_4_, p_72885_6_, p_72885_8_, p_72885_9_, p_72885_10_);

        if (explosion.wasCanceled)
        {
            return explosion;
        }

        /* Remove
        Explosion explosion = new Explosion(this, p_72885_1_, p_72885_2_, p_72885_4_, p_72885_6_, p_72885_8_);
        explosion.isFlaming = p_72885_9_;
        explosion.isSmoking = p_72885_10_;
        explosion.doExplosionA();
        explosion.doExplosionB(false);
        */
        // CraftBukkit end - TODO: Check if explosions are still properly implemented

        if (!p_72885_10_)
        {
            explosion.affectedBlockPositions.clear();
        }

        Iterator iterator = this.playerEntities.iterator();

        while (iterator.hasNext())
        {
            EntityPlayer entityplayer = (EntityPlayer)iterator.next();

            if (entityplayer.getDistanceSq(p_72885_2_, p_72885_4_, p_72885_6_) < 4096.0D)
            {
                ((EntityPlayerMP)entityplayer).playerNetServerHandler.sendPacket(new S27PacketExplosion(p_72885_2_, p_72885_4_, p_72885_6_, p_72885_8_, explosion.affectedBlockPositions, (Vec3)explosion.func_77277_b().get(entityplayer)));
            }
        }

        return explosion;
    }

    public void addBlockEvent(int p_147452_1_, int p_147452_2_, int p_147452_3_, Block p_147452_4_, int p_147452_5_, int p_147452_6_)
    {
        BlockEventData blockeventdata = new BlockEventData(p_147452_1_, p_147452_2_, p_147452_3_, p_147452_4_, p_147452_5_, p_147452_6_);
        Iterator iterator = this.field_147490_S[this.blockEventCacheIndex].iterator();
        BlockEventData blockeventdata1;

        do
        {
            if (!iterator.hasNext())
            {
                this.field_147490_S[this.blockEventCacheIndex].add(blockeventdata);
                return;
            }

            blockeventdata1 = (BlockEventData)iterator.next();
        }
        while (!blockeventdata1.equals(blockeventdata));
    }

    private void func_147488_Z()
    {
        while (!this.field_147490_S[this.blockEventCacheIndex].isEmpty())
        {
            int i = this.blockEventCacheIndex;
            this.blockEventCacheIndex ^= 1;
            Iterator iterator = this.field_147490_S[i].iterator();

            while (iterator.hasNext())
            {
                BlockEventData blockeventdata = (BlockEventData)iterator.next();

                if (this.func_147485_a(blockeventdata))
                {
                    this.mcServer.getConfigurationManager().sendToAllNear((double)blockeventdata.func_151340_a(), (double)blockeventdata.func_151342_b(), (double)blockeventdata.func_151341_c(), 64.0D, this.provider.dimensionId, new S24PacketBlockAction(blockeventdata.func_151340_a(), blockeventdata.func_151342_b(), blockeventdata.func_151341_c(), blockeventdata.getBlock(), blockeventdata.getEventID(), blockeventdata.getEventParameter()));
                }
            }

            this.field_147490_S[i].clear();
        }
    }

    private boolean func_147485_a(BlockEventData p_147485_1_)
    {
        Block block = this.getBlock(p_147485_1_.func_151340_a(), p_147485_1_.func_151342_b(), p_147485_1_.func_151341_c());
        return block == p_147485_1_.getBlock() ? block.onBlockEventReceived(this, p_147485_1_.func_151340_a(), p_147485_1_.func_151342_b(), p_147485_1_.func_151341_c(), p_147485_1_.getEventID(), p_147485_1_.getEventParameter()) : false;
    }

    public void flush()
    {
        this.saveHandler.flush();
    }

    protected void updateWeather()
    {
        boolean flag = this.isRaining();
        super.updateWeather();

        /* CraftBukkit start
        if (this.prevRainingStrength != this.rainingStrength)
        {
            this.mcServer.getConfigurationManager().sendPacketToAllPlayersInDimension(new S2BPacketChangeGameState(7, this.rainingStrength), this.provider.dimensionId);
        }

        if (this.prevThunderingStrength != this.thunderingStrength)
        {
            this.mcServer.getConfigurationManager().sendPacketToAllPlayersInDimension(new S2BPacketChangeGameState(8, this.thunderingStrength), this.provider.dimensionId);
        }

        if (flag != this.isRaining())
        {
            if (flag)
            {
                this.mcServer.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(2, 0.0F));
            }
            else
            {
                this.mcServer.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(1, 0.0F));
            }

            this.mcServer.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(7, this.rainingStrength));
            this.mcServer.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(8, this.thunderingStrength));
        }
        // */
        if (flag != this.isRaining())
        {
            // Only send weather packets to those affected
            for (int i = 0; i < this.playerEntities.size(); ++i)
            {
                if (((EntityPlayerMP) this.playerEntities.get(i)).worldObj == this)
                {
                    ((EntityPlayerMP) this.playerEntities.get(i)).setPlayerWeather((!flag ? WeatherType.DOWNFALL : WeatherType.CLEAR), false);
                }
            }

            // CraftBukkit end
        }
    }

    protected int func_152379_p()
    {
        return this.mcServer.getConfigurationManager().getViewDistance();
    }

    public MinecraftServer func_73046_m()
    {
        return this.mcServer;
    }

    public EntityTracker getEntityTracker()
    {
        return this.theEntityTracker;
    }

    public PlayerManager getPlayerManager()
    {
        return this.thePlayerManager;
    }

    public Teleporter getDefaultTeleporter()
    {
        return this.worldTeleporter;
    }

    public void func_147487_a(String p_147487_1_, double p_147487_2_, double p_147487_4_, double p_147487_6_, int p_147487_8_, double p_147487_9_, double p_147487_11_, double p_147487_13_, double p_147487_15_)
    {
        S2APacketParticles s2apacketparticles = new S2APacketParticles(p_147487_1_, (float)p_147487_2_, (float)p_147487_4_, (float)p_147487_6_, (float)p_147487_9_, (float)p_147487_11_, (float)p_147487_13_, (float)p_147487_15_, p_147487_8_);

        for (int j = 0; j < this.playerEntities.size(); ++j)
        {
            EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playerEntities.get(j);
            ChunkCoordinates chunkcoordinates = entityplayermp.getPlayerCoordinates();
            double d7 = p_147487_2_ - (double)chunkcoordinates.posX;
            double d8 = p_147487_4_ - (double)chunkcoordinates.posY;
            double d9 = p_147487_6_ - (double)chunkcoordinates.posZ;
            double d10 = d7 * d7 + d8 * d8 + d9 * d9;

            if (d10 <= 256.0D)
            {
                entityplayermp.playerNetServerHandler.sendPacket(s2apacketparticles);
            }
        }
    }

    public File getChunkSaveLocation()
    {
        return ((AnvilChunkLoader)theChunkProviderServer.currentChunkLoader).chunkSaveLocation;
    }

    static class ServerBlockEventList extends ArrayList
    {
        private static final String __OBFID = "CL_00001439";

        private ServerBlockEventList() {}

        ServerBlockEventList(Object p_i1521_1_)
        {
            this();
        }
    }

    // CraftBukkit start - Compatibility methods for BlockChangeDelegate
    public boolean setRawTypeId(int x, int y, int z, int typeId)
    {
        return this.setBlock(x, y, z, Block.getBlockById(typeId), 0, 4);
    }

    public boolean setRawTypeIdAndData(int x, int y, int z, int typeId, int data)
    {
        return this.setBlock(x, y, z, Block.getBlockById(typeId), data, 4);
    }

    public boolean setTypeId(int x, int y, int z, int typeId)
    {
        return this.setBlock(x, y, z, Block.getBlockById(typeId), 0, 3);
    }

    public boolean setTypeIdAndData(int x, int y, int z, int typeId, int data)
    {
        return this.setBlock(x, y, z, Block.getBlockById(typeId), data, 3);
    }

    public int getTypeId(int x, int y, int z)
    {
        return Block.getIdFromBlock(getBlock(x, y, z));
    }
    // CraftBukkit end
}