package net.minecraft.server;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.ServerLaunchWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.dispenser.DispenserBehaviors;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.logging.ILogAgent;
import net.minecraft.network.NetworkListenThread;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet4UpdateTime;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.PropertyManager;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.stats.StatList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
// CraftBukkit start
import net.minecraft.command.ServerCommand;
import net.minecraft.entity.player.EntityPlayerMP;
import jline.console.ConsoleReader;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.world.WorldSaveEvent;
// CraftBukkit end
// Cauldron start
import java.util.Map;
import cpw.mods.fml.common.asm.transformers.SideTransformer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.cauldron.CauldronUtils;
import net.minecraftforge.common.EnumHelper;
// Cauldron end

public abstract class MinecraftServer implements ICommandSender, Runnable, IPlayerUsage
{
    /** Instance of Minecraft Server. */
    private static MinecraftServer mcServer;
    public ISaveFormat anvilConverterForAnvilFile; // CraftBukkit - private final -> public

    /** The PlayerUsageSnooper instance. */
    private final PlayerUsageSnooper usageSnooper = new PlayerUsageSnooper("server", this, getSystemTimeMillis());
    public File anvilFile; // CraftBukkit - private final -> public

    /**
     * Collection of objects to update every tick. Type: List<IUpdatePlayerListBox>
     */
    private final List tickables = new ArrayList();
    private final ICommandManager commandManager;
    public final Profiler theProfiler = new Profiler();

    /** The server's hostname. */
    private String hostname;

    /** The server's port. */
    private int serverPort = -1;

    /** The server world instances. */
    public WorldServer[] worldServers = new WorldServer[0]; // Cauldron - vanilla compatibility

    /** The ServerConfigurationManager instance. */
    private ServerConfigurationManager serverConfigManager;

    /**
     * Indicates whether the server is running or not. Set to false to initiate a shutdown.
     */
    private boolean serverRunning = true;

    /** Indicates to other classes that the server is safely stopped. */
    private boolean serverStopped;

    /** Incremented every tick. */
    private int tickCounter;
    protected Proxy serverProxy;

    /**
     * The task the server is currently working on(and will output on outputPercentRemaining).
     */
    public String currentTask;

    /** The percentage of the current task finished so far. */
    public int percentDone;

    /** True if the server is in online mode. */
    private boolean onlineMode;

    /** True if the server has animals turned on. */
    private boolean canSpawnAnimals;
    private boolean canSpawnNPCs;

    /** Indicates whether PvP is active on the server or not. */
    private boolean pvpEnabled;

    /** Determines if flight is allowed or not. */
    private boolean allowFlight;

    /** The server MOTD string. */
    private String motd;

    /** Maximum build height. */
    private int buildLimit;
    private int field_143008_E;
    private long lastSentPacketID;
    private long lastSentPacketSize;
    private long lastReceivedID;
    private long lastReceivedSize;
    public final long[] sentPacketCountArray;
    public final long[] sentPacketSizeArray;
    public final long[] receivedPacketCountArray;
    public final long[] receivedPacketSizeArray;
    public final long[] tickTimeArray;

    /** Stats are [dimension][tick%100] system.nanoTime is stored. */
    //public long[][] timeOfLastDimensionTick;
    public Hashtable<Integer, long[]> worldTickTimes = new Hashtable<Integer, long[]>();
    private KeyPair serverKeyPair;

    /** Username of the server owner (for integrated servers) */
    private String serverOwner;
    private String folderName;
    @SideOnly(Side.CLIENT)
    private String worldName;
    private boolean isDemo;
    private boolean enableBonusChest;

    /**
     * If true, there is no need to save chunks or stop the server, because that is already being done.
     */
    private boolean worldIsBeingDeleted;
    private String texturePack;
    private boolean serverIsRunning;

    /**
     * Set when warned for "Can't keep up", which triggers again after 15 seconds.
     */
    private long timeOfLastWarning;
    private String userMessage;
    private boolean startProfiling;
    private boolean isGamemodeForced;

    // CraftBukkit start
    public List<WorldServer> worlds = new ArrayList<WorldServer>();
    public org.bukkit.craftbukkit.CraftServer server;
    public static OptionSet options;
    public org.bukkit.command.ConsoleCommandSender console;
    public org.bukkit.command.RemoteConsoleCommandSender remoteConsole;
    public ConsoleReader reader;
    public static int currentTick = (int)(System.currentTimeMillis() / 50);
    public final Thread primaryThread;
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
    public int autosavePeriod;
    // CraftBukkit end
    // Spigot start
    private static final int TPS = 20;
    private static final int TICK_TIME = 1000000000 / TPS;
    public static double currentTPS = 0;
    private static long catchupTime = 0;
    // Spigot end
    // Cauldron start
    public static YamlConfiguration configuration;
    public static File configFile;
    public static boolean useJline = true;
    public static boolean useConsole = true;
    public static boolean callingForgeTick = false;
    public static List<Class<? extends TileEntity>> bannedTileEntityUpdates = new ArrayList<Class<? extends TileEntity>>();
    // Cauldron end

    // Cauldron start - vanilla compatibility
    public MinecraftServer(File par1File)
    {
        mcServer = this;
        this.anvilFile = par1File;
        this.commandManager = new ServerCommandManager();
        this.anvilConverterForAnvilFile = new AnvilSaveConverter(par1File);
        this.registerDispenseBehaviors();
        primaryThread = null;
        this.serverProxy = Proxy.NO_PROXY;
        this.sentPacketCountArray = new long[100];
        this.sentPacketSizeArray = new long[100];
        this.receivedPacketCountArray = new long[100];
        this.receivedPacketSizeArray = new long[100];
        this.tickTimeArray = new long[100];
    }
    // Cauldron end

    public MinecraftServer(OptionSet options)   // CraftBukkit - signature file -> OptionSet
    {
        this.serverProxy = Proxy.NO_PROXY;
        this.field_143008_E = 0;
        this.sentPacketCountArray = new long[100];
        this.sentPacketSizeArray = new long[100];
        this.receivedPacketCountArray = new long[100];
        this.receivedPacketSizeArray = new long[100];
        this.tickTimeArray = new long[100];
        this.texturePack = "";
        mcServer = this;
        // this.universe = file1; // CraftBukkit
        this.commandManager = new ServerCommandManager();
        // this.convertable = new WorldLoaderServer(server.getWorldContainer()); // CraftBukkit - moved to DedicatedServer.init
        this.registerDispenseBehaviors();
        // CraftBukkit start
        this.options = options;
        // Try to see if we're actually running in a terminal, disable jline if not
        if (System.console() == null) {
            System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
            this.useJline = false;
        }

        try
        {
            this.reader = new ConsoleReader(System.in, System.out);
            this.reader.setExpandEvents(false); // Avoid parsing exceptions for uncommonly used event designators
        }
        catch (Exception e)
        {
            try
            {
                // Try again with jline disabled for Windows users without C++ 2008 Redistributable
                System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
                System.setProperty("user.language", "en");
                this.useJline = false;
                this.reader = new ConsoleReader(System.in, System.out);
                this.reader.setExpandEvents(false);
            }
            catch (IOException ex)
            {
                Logger.getLogger(MinecraftServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        net.minecraftforge.cauldron.CauldronHooks.enableThreadContentionMonitoring();
        Runtime.getRuntime().addShutdownHook(new org.bukkit.craftbukkit.util.ServerShutdownThread(this));
        primaryThread = new ThreadMinecraftServer(this, "Server thread"); // Moved from main
    }

    public abstract PropertyManager getPropertyManager();
    // CraftBukkit end

    /**
     * Register all dispense behaviors.
     */
    private void registerDispenseBehaviors()
    {
        DispenserBehaviors.registerDispenserBehaviours();
    }

    /**
     * Initialises the server and starts it.
     */
    protected abstract boolean startServer() throws java.net.UnknownHostException; // CraftBukkit - throws UnknownHostException

    protected void convertMapIfNeeded(String par1Str)
    {
        if (this.getActiveAnvilConverter().isOldMapFormat(par1Str))
        {
            this.getLogAgent().logInfo("Converting map!");
            this.setUserMessage("menu.convertingLevel");
            this.getActiveAnvilConverter().convertMapFormat(par1Str, new ConvertingProgressUpdate(this));
        }
    }

    /**
     * Typically "menu.convertingLevel", "menu.loadingLevel" or others.
     */
    protected synchronized void setUserMessage(String par1Str)
    {
        this.userMessage = par1Str;
    }

    @SideOnly(Side.CLIENT)

    public synchronized String getUserMessage()
    {
        return this.userMessage;
    }

    protected void loadAllWorlds(String par1Str, String par2Str, long par3, WorldType par5WorldType, String par6Str)
    {
        // Cauldron start - register vanilla server commands
        ServerCommandManager vanillaCommandManager = (ServerCommandManager)this.getCommandManager();
        vanillaCommandManager.registerVanillaCommands();
        // Cauldron end
        this.convertMapIfNeeded(par1Str);
        this.setUserMessage("menu.loadingLevel");
        // CraftBukkit - Removed ticktime arrays
        ISaveHandler isavehandler = this.anvilConverterForAnvilFile.getSaveLoader(par1Str, true);
        WorldInfo worldinfo = isavehandler.loadWorldInfo();
        // CraftBukkit start - Removed worldsettings
        
        WorldSettings worldsettings = new WorldSettings(par3, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), par5WorldType);
        worldsettings.func_82750_a(par6Str);
        WorldServer world;

        org.bukkit.generator.ChunkGenerator overWorldGen = this.server.getGenerator(par1Str);
        WorldServer overWorld = (isDemo() ? new DemoWorldServer(this, new AnvilSaveHandler(server.getWorldContainer(), par2Str, true), par2Str, 0, theProfiler, this.getLogAgent()) : new WorldServer(this, new AnvilSaveHandler(server.getWorldContainer(), par2Str, true), par2Str, 0, worldsettings, theProfiler, this.getLogAgent(), Environment.getEnvironment(0), overWorldGen));
        if (overWorldGen != null)
        {
            overWorld.getWorld().getPopulators().addAll(overWorldGen.getDefaultPopulators(overWorld.getWorld()));
        }

        for (int dimension : DimensionManager.getStaticDimensionIDs())
        {
            String worldType = "";
            String name = "";
            String oldName = "";
            org.bukkit.generator.ChunkGenerator gen = null;
            // Cauldron start
            Environment env = Environment.getEnvironment(dimension);
            if (dimension != 0)
            {
                if ((dimension == -1 && !this.getAllowNether()) || (dimension == 1 && !this.server.getAllowEnd()))
                    continue;

                if (env == null)
                {
                    WorldProvider provider = WorldProvider.getProviderForDimension(dimension);
                    worldType = provider.getClass().getSimpleName().toLowerCase();
                    worldType = worldType.replace("worldprovider", "");
                    oldName = "world_" + worldType.toLowerCase();
                    worldType = worldType.replace("provider", "");
                    env = Environment.getEnvironment(DimensionManager.getProviderType(provider.getClass()));
                    name = provider.getSaveFolder();
                    if (name == null) name = "DIM0";
                }
                else 
                {
                    worldType = env.toString().toLowerCase();
                    name = "DIM" + dimension;
                    oldName = par1Str + "_" + worldType;
                    oldName = oldName.replaceAll(" ", "_");
                }

                // check if the world is enabled or not
                if (!configuration.isBoolean("world-settings." + worldType + ".enabled")) {
                    configuration.set("world-settings." + worldType + ".enabled", true);
                }
                boolean enabled = configuration.getBoolean("world-settings." + worldType + ".enabled");
                try {
                    configuration.save(MinecraftServer.configFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (!enabled)
                    continue;
                // end world enabled check

                gen = this.server.getGenerator(name);
                worldsettings = new WorldSettings(par3, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), par5WorldType);
                worldsettings.func_82750_a(par6Str);

                CauldronUtils.migrateWorlds(worldType, oldName, par1Str, name);

                this.setUserMessage(name);
            }

            world = (dimension == 0 ? overWorld : new WorldServerMulti(this, new AnvilSaveHandler(server.getWorldContainer(), name, true), name, dimension, worldsettings, overWorld, this.theProfiler, this.getLogAgent(), env, gen));
            // Cauldron end
            if (gen != null)
            {
                world.getWorld().getPopulators().addAll(gen.getDefaultPopulators(world.getWorld()));
            }

            this.server.scoreboardManager = new org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager(this, world.getScoreboard());
            this.server.getPluginManager().callEvent(new org.bukkit.event.world.WorldInitEvent(world.getWorld()));
            world.addWorldAccess(new WorldManager(this, world));

            if (!this.isSinglePlayer())
            {
                world.getWorldInfo().setGameType(this.getGameType());
            }

            this.serverConfigManager.setPlayerManager(this.worlds.toArray(new WorldServer[this.worlds.size()]));
            // CraftBukkit end
            MinecraftForge.EVENT_BUS.post(new WorldEvent.Load((World)world)); // Forge
        }
        this.setDifficultyForAllWorlds(this.getDifficulty());
        this.initialWorldChunkLoad();
        CraftBlock.dumpMaterials(); // Cauldron
        // Cauldron start
        SideTransformer.allowInvalidSide = true;
        for (Object obj : TileEntity.classToNameMap.entrySet())
        {
            Map.Entry<Class<? extends TileEntity>, String> tileEntry = (Map.Entry<Class<? extends TileEntity>, String>)obj;
            if (tileEntry.getKey() == null)
                continue;
            // verify if TE's should tick
            if (!CauldronUtils.isOverridingUpdateEntity(tileEntry.getKey()) && CauldronUtils.canTileEntityUpdate(tileEntry.getKey()))
            {
                bannedTileEntityUpdates.add(tileEntry.getKey());
            }
            //  register TE's for inventory events
            EnumHelper.addInventoryType(tileEntry.getKey(), tileEntry.getValue());
        }
        CauldronUtils.dumpAndSortClassList(bannedTileEntityUpdates);
        SideTransformer.allowInvalidSide = false;
        // Cauldron end
    }

    protected void initialWorldChunkLoad()
    {
        boolean flag = true;
        boolean flag1 = true;
        boolean flag2 = true;
        boolean flag3 = true;
        int i = 0;
        this.setUserMessage("menu.generatingTerrain");
        byte b0 = 0;
        // Cauldron start - we now handle CraftBukkit's keepSpawnInMemory logic in DimensionManager. Prevents crashes with mods such as DivineRPG and speeds up server startup time by a ton.
        WorldServer worldserver = this.worldServers[b0];
        this.getLogAgent().logInfo("Preparing start region for level " + b0 + " (Dimension: " + worldserver.provider.dimensionId + ", Seed: " + worldserver.getSeed() + ")"); // Cauldron
        ChunkCoordinates chunkcoordinates = worldserver.getSpawnPoint();
        boolean before = worldserver.theChunkProviderServer.loadChunkOnProvideRequest;
        worldserver.theChunkProviderServer.loadChunkOnProvideRequest = true;
        long j = getSystemTimeMillis();

        for (int k = -192; k <= 192 && this.isServerRunning(); k += 16)
        {
            for (int l = -192; l <= 192 && this.isServerRunning(); l += 16)
            {
                long i1 = getSystemTimeMillis();

                if (i1 - j > 1000L)
                {
                    this.outputPercentRemaining("Preparing spawn area", i * 100 / 625);
                    j = i1;
                }

                ++i;
                worldserver.theChunkProviderServer.loadChunk(chunkcoordinates.posX + k >> 4, chunkcoordinates.posZ + l >> 4);
            }
        }
        worldserver.theChunkProviderServer.loadChunkOnProvideRequest = before;
        // Cauldron end
        this.clearCurrentTask();
    }

    public abstract boolean canStructuresSpawn();

    public abstract EnumGameType getGameType();

    /**
     * Defaults to "1" (Easy) for the dedicated server, defaults to "2" (Normal) on the client.
     */
    public abstract int getDifficulty();

    /**
     * Defaults to false.
     */
    public abstract boolean isHardcore();

    public abstract int func_110455_j();

    /**
     * Used to display a percent remaining given text and the percentage.
     */
    protected void outputPercentRemaining(String par1Str, int par2)
    {
        this.currentTask = par1Str;
        this.percentDone = par2;
        this.getLogAgent().logInfo(par1Str + ": " + par2 + "%");
    }

    /**
     * Set current task to null and set its percentage to 0.
     */
    protected void clearCurrentTask()
    {
        this.currentTask = null;
        this.percentDone = 0;
        this.server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD); // CraftBukkit
    }

    /**
     * par1 indicates if a log message should be output.
     */
    protected void saveAllWorlds(boolean par1) throws MinecraftException   // CraftBukkit - added throws
    {
        if (!this.worldIsBeingDeleted)
        {
            // CraftBukkit start
            for (int j = 0; j < this.worlds.size(); ++j)
            {
                WorldServer worldserver = this.worlds.get(j);

                if (worldserver != null)
                {
                    if (!par1)
                    {
                        this.getLogAgent().logInfo("Saving chunks for level \'" + worldserver.getWorldInfo().getWorldName() + "\'/" + worldserver.provider.getDimensionName());
                    }

                    worldserver.saveAllChunks(true, (IProgressUpdate) null);
                    worldserver.flush();
                    WorldSaveEvent event = new WorldSaveEvent(worldserver.getWorld());
                    this.server.getPluginManager().callEvent(event);
                    // Cauldron start - save world config
                    if (worldserver.cauldronConfig != null)
                    {
                        worldserver.cauldronConfig.save();
                    }
                    // Cauldron end
                }
            }

            // CraftBukkit end
        }
    }

    /**
     * Saves all necessary data as preparation for stopping the server.
     */
    public void stopServer() throws MinecraftException   // CraftBukkit - added throws
    {
        if (!this.worldIsBeingDeleted)
        {
            this.getLogAgent().logInfo("Stopping server");

            // CraftBukkit start
            if (this.server != null)
            {
                this.server.disablePlugins();
            }

            // CraftBukkit end

            if (this.getNetworkThread() != null)
            {
                this.getNetworkThread().stopListening();
            }

            if (this.serverConfigManager != null)
            {
                this.getLogAgent().logInfo("Saving players");
                this.serverConfigManager.saveAllPlayerData();
                this.serverConfigManager.removeAllPlayers();
            }

            this.getLogAgent().logInfo("Saving worlds");
            this.saveAllWorlds(false);

            for (int i = 0; i < this.worlds.size(); ++i)
            {
                WorldServer worldserver = this.worlds.get(i);
                MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(worldserver)); // Forge
                DimensionManager.setWorld(worldserver.provider.dimensionId, (WorldServer)null);
            }

            if (this.usageSnooper != null && this.usageSnooper.isSnooperRunning())
            {
                this.usageSnooper.stopSnooper();
            }
        }
    }

    /**
     * "getHostname" is already taken, but both return the hostname.
     */
    public String getServerHostname()
    {
        return this.hostname;
    }

    public void setHostname(String par1Str)
    {
        this.hostname = par1Str;
    }

    public boolean isServerRunning()
    {
        return this.serverRunning;
    }

    /**
     * Sets the serverRunning variable to false, in order to get the server to shut down.
     */
    public void initiateShutdown()
    {
        this.serverRunning = false;
    }

    public void run()
    {
        try
        {
            if (this.startServer())
            {
                FMLCommonHandler.instance().handleServerStarted();

                long i = getSystemTimeMillis();

                FMLCommonHandler.instance().onWorldLoadTick(this.worlds.toArray(new WorldServer[this.worlds.size()]));

                // Spigot start
                for (long lastTick = 0L; this.serverRunning; this.serverIsRunning = true)
                {
                    long curTime = System.nanoTime();
                    long wait = TICK_TIME - (curTime - lastTick) - catchupTime;

                    if (wait > 0)
                    {
                        Thread.sleep(wait / 1000000);
                        catchupTime = 0;
                        continue;
                    }
                    else
                    {
                        catchupTime = Math.min(TICK_TIME * TPS, Math.abs(wait));
                    }

                    currentTPS = (currentTPS * 0.95) + (1E9 / (curTime - lastTick) * 0.05);
                    lastTick = curTime;
                    MinecraftServer.currentTick++;
                    SpigotTimings.serverTickTimer.startTiming(); // Spigot
                    this.tick();
                    SpigotTimings.serverTickTimer.stopTiming(); // Spigot
                    org.spigotmc.CustomTimingsHandler.tick(); // Spigot
                    org.spigotmc.WatchdogThread.tick();
                }

                // Spigot end
                FMLCommonHandler.instance().handleServerStopping();
            }
            else
            {
                this.finalTick((CrashReport)null);
            }
        }
        catch (Throwable throwable)
        {
            if (FMLCommonHandler.instance().shouldServerBeKilledQuietly())
            {
                return;
            }
            throwable.printStackTrace();
            this.getLogAgent().logSevereException("Encountered an unexpected exception " + throwable.getClass().getSimpleName(), throwable);
            CrashReport crashreport = null;

            if (throwable instanceof ReportedException)
            {
                crashreport = this.addServerInfoToCrashReport(((ReportedException)throwable).getCrashReport());
            }
            else
            {
                crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable));
            }

            File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (crashreport.saveToFile(file1, this.getLogAgent()))
            {
                this.getLogAgent().logSevere("This crash report has been saved to: " + file1.getAbsolutePath());
            }
            else
            {
                this.getLogAgent().logSevere("We were unable to save this crash report to disk.");
            }

            this.finalTick(crashreport);
        }
        finally
        {
            org.spigotmc.WatchdogThread.doStop(); // Spigot

            try
            {
                if (FMLCommonHandler.instance().shouldServerBeKilledQuietly())
                {
                    return;
                }
                this.stopServer();
                this.serverStopped = true;
            }
            catch (Throwable throwable1)
            {
                throwable1.printStackTrace();
            }
            finally
            {
                // CraftBukkit start - Restore terminal to original settings
                try
                {
                    this.reader.getTerminal().restore();
                }
                catch (Exception e)
                {
                }

                // CraftBukkit end
                FMLCommonHandler.instance().handleServerStopped();
                this.serverStopped = true;
                this.systemExitNow();
            }
        }
    }

    protected File getDataDirectory()
    {
        return new File(".");
    }

    /**
     * Called on exit from the main run() loop.
     */
    protected void finalTick(CrashReport par1CrashReport) {}

    /**
     * Directly calls System.exit(0), instantly killing the program.
     */
    protected void systemExitNow() {}

    /**
     * Main function called by run() every loop.
     */
    public void tick() throws MinecraftException   // CraftBukkit - added throws // Cauldron - protected -> public for Forge
    {
        FMLCommonHandler.instance().rescheduleTicks(Side.SERVER); // Forge
        long i = System.nanoTime();
        AxisAlignedBB.getAABBPool().cleanPool();
        callingForgeTick = true; // Cauldron start - handle loadOnProviderRequests during forge tick event
        FMLCommonHandler.instance().onPreServerTick(); // Forge
        callingForgeTick = false; // Cauldron end
        ++this.tickCounter;

        if (this.startProfiling)
        {
            this.startProfiling = false;
            this.theProfiler.profilingEnabled = true;
            this.theProfiler.clearProfiling();
        }

        this.theProfiler.startSection("root");
        this.updateTimeLightAndEntities();

        if ((this.autosavePeriod > 0) && ((this.tickCounter % this.autosavePeriod) == 0))   // CraftBukkit
        {
            this.theProfiler.startSection("save");
            this.serverConfigManager.saveAllPlayerData();
            this.saveAllWorlds(true);
            this.theProfiler.endSection();
        }

        this.theProfiler.startSection("tallying");
        this.tickTimeArray[this.tickCounter % 100] = System.nanoTime() - i;
        this.sentPacketCountArray[this.tickCounter % 100] = Packet.sentID - this.lastSentPacketID;
        this.lastSentPacketID = Packet.sentID;
        this.sentPacketSizeArray[this.tickCounter % 100] = Packet.sentSize - this.lastSentPacketSize;
        this.lastSentPacketSize = Packet.sentSize;
        this.receivedPacketCountArray[this.tickCounter % 100] = Packet.receivedID - this.lastReceivedID;
        this.lastReceivedID = Packet.receivedID;
        this.receivedPacketSizeArray[this.tickCounter % 100] = Packet.receivedSize - this.lastReceivedSize;
        this.lastReceivedSize = Packet.receivedSize;
        this.theProfiler.endSection();
        this.theProfiler.startSection("snooper");

        if (this.isSnooperEnabled() && !this.usageSnooper.isSnooperRunning() && this.tickCounter > 100)
        {
            this.usageSnooper.startSnooper();
        }

        if (this.isSnooperEnabled() && this.tickCounter % 6000 == 0)
        {
            this.usageSnooper.addMemoryStatsToSnooper();
        }

        this.theProfiler.endSection();
        this.theProfiler.endSection();
        callingForgeTick = true; // Cauldron start - handle loadOnProviderRequests during forge tick event
        FMLCommonHandler.instance().onPostServerTick();
        callingForgeTick = false; // Cauldron end
    }

    public void updateTimeLightAndEntities()
    {
        this.theProfiler.startSection("levels");
        SpigotTimings.schedulerTimer.startTiming(); // Spigot
        // CraftBukkit start
        this.server.getScheduler().mainThreadHeartbeat(this.tickCounter);

        // Run tasks that are waiting on processing
        while (!processQueue.isEmpty())
        {
            processQueue.remove().run();
        }

        SpigotTimings.schedulerTimer.stopTiming(); // Spigot
        SpigotTimings.chunkIOTickTimer.startTiming(); // Spigot
        org.bukkit.craftbukkit.chunkio.ChunkIOExecutor.tick();
        SpigotTimings.chunkIOTickTimer.stopTiming(); // Spigot

        // Send time updates to everyone, it will get the right time from the world the player is in.
        if (this.tickCounter % 20 == 0)
        {
            for (int i = 0; i < this.getConfigurationManager().playerEntityList.size(); ++i)
            {
                EntityPlayerMP entityplayermp = (EntityPlayerMP) this.getConfigurationManager().playerEntityList.get(i);
                entityplayermp.playerNetServerHandler.sendPacketToPlayer(new Packet4UpdateTime(entityplayermp.worldObj.getTotalWorldTime(), entityplayermp.getPlayerTime(), entityplayermp.worldObj.getGameRules().getGameRuleBooleanValue("doDaylightCycle"))); // Add support for per player time
            }
        }

        int i;

        Integer[] ids = DimensionManager.getIDs(this.tickCounter % 200 == 0);
        for (int x = 0; x < ids.length; x++)
        {
            int id = ids[x];
            long j = System.nanoTime();
            // if (i == 0 || this.getAllowNether()) {
            WorldServer worldserver = DimensionManager.getWorld(id);
            this.theProfiler.startSection(worldserver.getWorldInfo().getWorldName());
            this.theProfiler.startSection("pools");
            worldserver.getWorldVec3Pool().clear();
            this.theProfiler.endSection();
            /* Drop global time updates
            if (this.tickCounter % 20 == 0)
            {
                this.theProfiler.startSection("timeSync");
                this.serverConfigManager.sendPacketToAllPlayersInDimension(new Packet4UpdateTime(worldserver.getTotalWorldTime(), worldserver.getWorldTime()), worldserver.provider.dimensionId);
                this.theProfiler.endSection();
            }
            // CraftBukkit end */
            this.theProfiler.startSection("tick");
            FMLCommonHandler.instance().onPreWorldTick(worldserver);
            CrashReport crashreport;

            try
            {
                worldserver.tick();
            }
            catch (Throwable throwable)
            {
                crashreport = CrashReport.makeCrashReport(throwable, "Exception ticking world");
                worldserver.addWorldInfoToCrashReport(crashreport);
                throw new ReportedException(crashreport);
            }

            try
            {
                worldserver.updateEntities();
            }
            catch (Throwable throwable1)
            {
                crashreport = CrashReport.makeCrashReport(throwable1, "Exception ticking world entities");
                worldserver.addWorldInfoToCrashReport(crashreport);
                throw new ReportedException(crashreport);
            }

            FMLCommonHandler.instance().onPostWorldTick(worldserver);
            this.theProfiler.endSection();
            this.theProfiler.startSection("tracker");
            worldserver.timings.tracker.startTiming(); // Spigot
            worldserver.getEntityTracker().updateTrackedEntities();
            worldserver.timings.tracker.stopTiming(); // Spigot
            this.theProfiler.endSection();
            this.theProfiler.endSection();

            // Forge start
            ((long[]) this.worldTickTimes.get(id))[this.tickCounter % 100] = System.nanoTime() - j;
        }

        this.theProfiler.endStartSection("dim_unloading");
        DimensionManager.unloadWorlds(this.worldTickTimes);
        // Forge end
        this.theProfiler.endStartSection("connection");
        SpigotTimings.connectionTimer.startTiming(); // Spigot
        this.getNetworkThread().networkTick();
        SpigotTimings.connectionTimer.stopTiming(); // Spigot
        this.theProfiler.endStartSection("players");
        SpigotTimings.playerListTimer.startTiming(); // Spigot
        this.serverConfigManager.sendPlayerInfoToAllPlayers();
        SpigotTimings.playerListTimer.stopTiming(); // Spigot
        this.theProfiler.endStartSection("tickables");
        SpigotTimings.tickablesTimer.startTiming(); // Spigot

        for (i = 0; i < this.tickables.size(); ++i)
        {
            ((IUpdatePlayerListBox)this.tickables.get(i)).update();
        }

        SpigotTimings.tickablesTimer.stopTiming(); // Spigot
        this.theProfiler.endSection();
    }

    public boolean getAllowNether()
    {
        return true;
    }

    public void startServerThread()
    {
        (new ThreadMinecraftServer(this, "Server thread")).start();
    }

    /**
     * Returns a File object from the specified string.
     */
    public File getFile(String par1Str)
    {
        return new File(this.getDataDirectory(), par1Str);
    }

    /**
     * Logs the message with a level of INFO.
     */
    public void logInfo(String par1Str)
    {
        this.getLogAgent().logInfo(par1Str);
    }

    /**
     * Logs the message with a level of WARN.
     */
    public void logWarning(String par1Str)
    {
        this.getLogAgent().logWarning(par1Str);
    }

    /**
     * Gets the worldServer by the given dimension.
     */
    public WorldServer worldServerForDimension(int par1)
    {
        // Cauldron start - this is required for MystCraft agebooks to teleport correctly
        // verify the nether or the end is allowed, and if not return overworld
        if ((par1 == -1 && !this.getAllowNether()) || (par1 == 1 && !this.server.getAllowEnd()))
        {
            return DimensionManager.getWorld(0);
        }

        WorldServer ret = DimensionManager.getWorld(par1);
        if (ret == null)
        {
            DimensionManager.initDimension(par1);
            ret = DimensionManager.getWorld(par1);
        }

        return ret;
        // Cauldron end
    }

    @SideOnly(Side.SERVER)
    public void func_82010_a(IUpdatePlayerListBox par1IUpdatePlayerListBox)
    {
        this.tickables.add(par1IUpdatePlayerListBox);
    }

    /**
     * Returns the server's hostname.
     */
    public String getHostname()
    {
        return this.hostname;
    }

    /**
     * Never used, but "getServerPort" is already taken.
     */
    public int getPort()
    {
        return this.serverPort;
    }

    /**
     * Returns the server message of the day
     */
    public String getServerMOTD()
    {
        return this.motd;
    }

    /**
     * Returns the server's Minecraft version as string.
     */
    public String getMinecraftVersion()
    {
        return "1.6.4";
    }

    /**
     * Returns the number of players currently on the server.
     */
    public int getCurrentPlayerCount()
    {
        return this.serverConfigManager.getCurrentPlayerCount();
    }

    /**
     * Returns the maximum number of players allowed on the server.
     */
    public int getMaxPlayers()
    {
        return this.serverConfigManager.getMaxPlayers();
    }

    /**
     * Returns an array of the usernames of all the connected players.
     */
    public String[] getAllUsernames()
    {
        return this.serverConfigManager.getAllUsernames();
    }

    /**
     * Used by RCon's Query in the form of "MajorServerMod 1.2.3: MyPlugin 1.3; AnotherPlugin 2.1; AndSoForth 1.0".
     */
    public String getPlugins()
    {
        // CraftBukkit start - Whole method
        StringBuilder result = new StringBuilder();
        org.bukkit.plugin.Plugin[] plugins = server.getPluginManager().getPlugins();
        result.append(server.getName());
        result.append(" on Bukkit ");
        result.append(server.getBukkitVersion());

        if (plugins.length > 0 && this.server.getQueryPlugins())
        {
            result.append(": ");

            for (int i = 0; i < plugins.length; i++)
            {
                if (i > 0)
                {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
        // CraftBukkit end
    }

    // CraftBukkit start
    public String executeCommand(final String par1Str)   // CraftBukkit - final parameter
    {
        Waitable<String> waitable = new Waitable<String>()
        {
            @Override
            protected String evaluate()
            {
                RConConsoleSource.consoleBuffer.resetLog();
                // Event changes start
                RemoteServerCommandEvent event = new RemoteServerCommandEvent(MinecraftServer.this.remoteConsole, par1Str);
                MinecraftServer.this.server.getPluginManager().callEvent(event);
                // Event changes end
                ServerCommand servercommand = new ServerCommand(event.getCommand(), RConConsoleSource.consoleBuffer);
                // this.q.a(RemoteControlCommandListener.instance, s);
                MinecraftServer.this.server.dispatchServerCommand(MinecraftServer.this.remoteConsole, servercommand); // CraftBukkit
                return RConConsoleSource.consoleBuffer.getChatBuffer();
            }
        };
        processQueue.add(waitable);

        try
        {
            return waitable.get();
        }
        catch (java.util.concurrent.ExecutionException e)
        {
            throw new RuntimeException("Exception processing rcon command " + par1Str, e.getCause());
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt(); // Maintain interrupted state
            throw new RuntimeException("Interrupted processing rcon command " + par1Str, e);
        }

        // CraftBukkit end
    }

    /**
     * Returns true if debugging is enabled, false otherwise.
     */
    public boolean isDebuggingEnabled()
    {
        return this.getPropertyManager().getBooleanProperty("debug", false); // CraftBukkit - don't hardcode
    }

    /**
     * Logs the error message with a level of SEVERE.
     */
    public void logSevere(String par1Str)
    {
        this.getLogAgent().logSevere(par1Str);
    }

    /**
     * If isDebuggingEnabled(), logs the message with a level of INFO.
     */
    public void logDebug(String par1Str)
    {
        if (this.isDebuggingEnabled())
        {
            this.getLogAgent().logInfo(par1Str);
        }
    }

    public String getServerModName()
    {
        return FMLCommonHandler.instance().getModName();
    }

    /**
     * Adds the server info, including from theWorldServer, to the crash report.
     */
    public CrashReport addServerInfoToCrashReport(CrashReport par1CrashReport)
    {
        par1CrashReport.getCategory().addCrashSectionCallable("Profiler Position", new CallableIsServerModded(this));

        if (this.worlds != null && this.worlds.size() > 0 && this.worlds.get(0) != null)
        {
            par1CrashReport.getCategory().addCrashSectionCallable("Vec3 Pool Size", new CallableServerProfiler(this));
        }

        if (this.serverConfigManager != null)
        {
            par1CrashReport.getCategory().addCrashSectionCallable("Player Count", new CallableServerMemoryStats(this));
        }

        return par1CrashReport;
    }

    /**
     * If par2Str begins with /, then it searches for commands, otherwise it returns players.
     */
    public List getPossibleCompletions(ICommandSender par1ICommandSender, String par2Str)
    {
        // Cauldron start - add mod commands to list then pass to bukkit
        java.util.HashSet arraylist = new java.util.HashSet(); // use a set here to avoid duplicates

        if (par2Str.startsWith("/"))
        {
            String char1 = par2Str.substring(1); // rename var to avoid removing slash from passed message
            boolean flag = !char1.contains(" ");
            List list = this.commandManager.getPossibleCommands(par1ICommandSender, char1);

            if (list != null)
            {
                java.util.Iterator iterator = list.iterator();

                while (iterator.hasNext())
                {
                    String command = (String)iterator.next();

                    if (flag)
                    {
                        arraylist.add("/" + command);
                    }
                }
            }
        }

        arraylist.addAll(this.server.tabComplete(par1ICommandSender, par2Str));  // add craftbukkit commands
        return new ArrayList(arraylist);
        // Cauldron end
    }

    /**
     * Gets mcServer.
     */
    public static MinecraftServer getServer()
    {
        return mcServer;
    }

    /**
     * Gets the name of this command sender (usually username, but possibly "Rcon")
     */
    public String getCommandSenderName()
    {
        return "Server";
    }

    public void sendChatToPlayer(ChatMessageComponent par1ChatMessageComponent)
    {
        this.getLogAgent().logInfo(par1ChatMessageComponent.toString());
    }

    /**
     * Returns true if the command sender is allowed to use the given command.
     */
    public boolean canCommandSenderUseCommand(int par1, String par2Str)
    {
        return true;
    }

    public ICommandManager getCommandManager()
    {
        return this.commandManager;
    }

    /**
     * Gets KeyPair instanced in MinecraftServer.
     */
    public KeyPair getKeyPair()
    {
        return this.serverKeyPair;
    }

    /**
     * Gets serverPort.
     */
    public int getServerPort()
    {
        return this.serverPort;
    }

    public void setServerPort(int par1)
    {
        this.serverPort = par1;
    }

    /**
     * Returns the username of the server owner (for integrated servers)
     */
    public String getServerOwner()
    {
        return this.serverOwner;
    }

    /**
     * Sets the username of the owner of this server (in the case of an integrated server)
     */
    public void setServerOwner(String par1Str)
    {
        this.serverOwner = par1Str;
    }

    public boolean isSinglePlayer()
    {
        return this.serverOwner != null;
    }

    public String getFolderName()
    {
        return this.folderName;
    }

    public void setFolderName(String par1Str)
    {
        this.folderName = par1Str;
    }

    @SideOnly(Side.CLIENT)
    public void setWorldName(String par1Str)
    {
        this.worldName = par1Str;
    }

    @SideOnly(Side.CLIENT)
    public String getWorldName()
    {
        return this.worldName;
    }

    public void setKeyPair(KeyPair par1KeyPair)
    {
        this.serverKeyPair = par1KeyPair;
    }

    public void setDifficultyForAllWorlds(int par1)
    {
        // CraftBukkit start
        for (int j = 0; j < this.worlds.size(); ++j)
        {
            WorldServer worldserver = this.worlds.get(j);
            // CraftBukkit end

            if (worldserver != null)
            {
                if (worldserver.getWorldInfo().isHardcoreModeEnabled())
                {
                    worldserver.difficultySetting = 3;
                    worldserver.setAllowedSpawnTypes(true, true);
                }
                else if (this.isSinglePlayer())
                {
                    worldserver.difficultySetting = par1;
                    worldserver.setAllowedSpawnTypes(worldserver.difficultySetting > 0, true);
                }
                else
                {
                    worldserver.difficultySetting = par1;
                    worldserver.setAllowedSpawnTypes(this.allowSpawnMonsters(), this.canSpawnAnimals);
                }
            }
        }
    }

    protected boolean allowSpawnMonsters()
    {
        return true;
    }

    /**
     * Gets whether this is a demo or not.
     */
    public boolean isDemo()
    {
        return this.isDemo;
    }

    /**
     * Sets whether this is a demo or not.
     */
    public void setDemo(boolean par1)
    {
        this.isDemo = par1;
    }

    public void canCreateBonusChest(boolean par1)
    {
        this.enableBonusChest = par1;
    }

    public ISaveFormat getActiveAnvilConverter()
    {
        return this.anvilConverterForAnvilFile;
    }

    /**
     * WARNING : directly calls
     * getActiveAnvilConverter().deleteWorldDirectory(theWorldServer[0].getSaveHandler().getWorldDirectoryName());
     */
    public void deleteWorldAndStopServer()
    {
        this.worldIsBeingDeleted = true;
        this.getActiveAnvilConverter().flushCache();

        for (int i = 0; i < this.worlds.size(); ++i)
        {
            WorldServer worldserver = this.worlds.get(i);

            if (worldserver != null)
            {
                MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(worldserver));
                worldserver.flush();
            }
        }

        this.getActiveAnvilConverter().deleteWorldDirectory(this.worlds.get(0).getSaveHandler().getWorldDirectoryName()); // CraftBukkit
        this.initiateShutdown();
    }

    public String getTexturePack()
    {
        return this.texturePack;
    }

    public void setTexturePack(String par1Str)
    {
        this.texturePack = par1Str;
    }

    public void addServerStatsToSnooper(PlayerUsageSnooper par1PlayerUsageSnooper)
    {
        par1PlayerUsageSnooper.addData("whitelist_enabled", Boolean.valueOf(false));
        par1PlayerUsageSnooper.addData("whitelist_count", Integer.valueOf(0));
        par1PlayerUsageSnooper.addData("players_current", Integer.valueOf(this.getCurrentPlayerCount()));
        par1PlayerUsageSnooper.addData("players_max", Integer.valueOf(this.getMaxPlayers()));
        par1PlayerUsageSnooper.addData("players_seen", Integer.valueOf(this.serverConfigManager.getAvailablePlayerDat().length));
        par1PlayerUsageSnooper.addData("uses_auth", Boolean.valueOf(this.onlineMode));
        par1PlayerUsageSnooper.addData("gui_state", this.getGuiEnabled() ? "enabled" : "disabled");
        par1PlayerUsageSnooper.addData("run_time", Long.valueOf((getSystemTimeMillis() - par1PlayerUsageSnooper.func_130105_g()) / 60L * 1000L));
        par1PlayerUsageSnooper.addData("avg_tick_ms", Integer.valueOf((int)(MathHelper.average(this.tickTimeArray) * 1.0E-6D)));
        par1PlayerUsageSnooper.addData("avg_sent_packet_count", Integer.valueOf((int)MathHelper.average(this.sentPacketCountArray)));
        par1PlayerUsageSnooper.addData("avg_sent_packet_size", Integer.valueOf((int)MathHelper.average(this.sentPacketSizeArray)));
        par1PlayerUsageSnooper.addData("avg_rec_packet_count", Integer.valueOf((int)MathHelper.average(this.receivedPacketCountArray)));
        par1PlayerUsageSnooper.addData("avg_rec_packet_size", Integer.valueOf((int)MathHelper.average(this.receivedPacketSizeArray)));
        int i = 0;

        // CraftBukkit start
        for (int j = 0; j < this.worlds.size(); ++j)
        {
            // if (this.worldServer[j] != null) {
            WorldServer worldserver = this.worlds.get(j);
            // CraftBukkit end
            WorldInfo worldinfo = worldserver.getWorldInfo();
            par1PlayerUsageSnooper.addData("world[" + i + "][dimension]", Integer.valueOf(worldserver.provider.dimensionId));
            par1PlayerUsageSnooper.addData("world[" + i + "][mode]", worldinfo.getGameType());
            par1PlayerUsageSnooper.addData("world[" + i + "][difficulty]", Integer.valueOf(worldserver.difficultySetting));
            par1PlayerUsageSnooper.addData("world[" + i + "][hardcore]", Boolean.valueOf(worldinfo.isHardcoreModeEnabled()));
            par1PlayerUsageSnooper.addData("world[" + i + "][generator_name]", worldinfo.getTerrainType().getWorldTypeName());
            par1PlayerUsageSnooper.addData("world[" + i + "][generator_version]", Integer.valueOf(worldinfo.getTerrainType().getGeneratorVersion()));
            par1PlayerUsageSnooper.addData("world[" + i + "][height]", Integer.valueOf(this.buildLimit));
            par1PlayerUsageSnooper.addData("world[" + i + "][chunks_loaded]", Integer.valueOf(worldserver.getChunkProvider().getLoadedChunkCount()));
            ++i;
            // } // CraftBukkit
        }

        par1PlayerUsageSnooper.addData("worlds", Integer.valueOf(i));
    }

    public void addServerTypeToSnooper(PlayerUsageSnooper par1PlayerUsageSnooper)
    {
        par1PlayerUsageSnooper.addData("singleplayer", Boolean.valueOf(this.isSinglePlayer()));
        par1PlayerUsageSnooper.addData("server_brand", this.getServerModName());
        par1PlayerUsageSnooper.addData("gui_supported", GraphicsEnvironment.isHeadless() ? "headless" : "supported");
        par1PlayerUsageSnooper.addData("dedicated", Boolean.valueOf(this.isDedicatedServer()));
    }

    /**
     * Returns whether snooping is enabled or not.
     */
    public boolean isSnooperEnabled()
    {
        return true;
    }

    /**
     * This is checked to be 16 upon receiving the packet, otherwise the packet is ignored.
     */
    public int textureSize()
    {
        return 16;
    }

    public abstract boolean isDedicatedServer();

    public boolean isServerInOnlineMode()
    {
        return this.server.getOnlineMode(); // CraftBukkit
    }

    public void setOnlineMode(boolean par1)
    {
        this.onlineMode = par1;
    }

    public boolean getCanSpawnAnimals()
    {
        return this.canSpawnAnimals;
    }

    public void setCanSpawnAnimals(boolean par1)
    {
        this.canSpawnAnimals = par1;
    }

    public boolean getCanSpawnNPCs()
    {
        return this.canSpawnNPCs;
    }

    public void setCanSpawnNPCs(boolean par1)
    {
        this.canSpawnNPCs = par1;
    }

    public boolean isPVPEnabled()
    {
        return this.pvpEnabled;
    }

    public void setAllowPvp(boolean par1)
    {
        this.pvpEnabled = par1;
    }

    public boolean isFlightAllowed()
    {
        return this.allowFlight;
    }

    public void setAllowFlight(boolean par1)
    {
        this.allowFlight = par1;
    }

    /**
     * Return whether command blocks are enabled.
     */
    public abstract boolean isCommandBlockEnabled();

    public String getMOTD()
    {
        return this.motd;
    }

    public void setMOTD(String par1Str)
    {
        this.motd = par1Str;
    }

    public int getBuildLimit()
    {
        return this.buildLimit;
    }

    public void setBuildLimit(int par1)
    {
        this.buildLimit = par1;
    }

    public boolean isServerStopped()
    {
        return this.serverStopped;
    }

    public ServerConfigurationManager getConfigurationManager()
    {
        return this.serverConfigManager;
    }

    public void setConfigurationManager(ServerConfigurationManager par1ServerConfigurationManager)
    {
        this.serverConfigManager = par1ServerConfigurationManager;
    }

    /**
     * Sets the game type for all worlds.
     */
    public void setGameType(EnumGameType par1EnumGameType)
    {
        // CraftBukkit start
        for (int i = 0; i < this.worlds.size(); ++i)
        {
            getServer().worlds.get(i).getWorldInfo().setGameType(par1EnumGameType);
            // CraftBukkit end
        }
    }

    public abstract NetworkListenThread getNetworkThread();

    @SideOnly(Side.CLIENT)
    public boolean serverIsInRunLoop()
    {
        return this.serverIsRunning;
    }

    public boolean getGuiEnabled()
    {
        return false;
    }

    /**
     * On dedicated does nothing. On integrated, sets commandsAllowedForAll, gameType and allows external connections.
     */
    public abstract String shareToLAN(EnumGameType enumgametype, boolean flag);

    public int getTickCounter()
    {
        return this.tickCounter;
    }

    public void enableProfiling()
    {
        this.startProfiling = true;
    }

    @SideOnly(Side.CLIENT)
    public PlayerUsageSnooper getPlayerUsageSnooper()
    {
        return this.usageSnooper;
    }

    /**
     * Return the position for this command sender.
     */
    public ChunkCoordinates getPlayerCoordinates()
    {
        return new ChunkCoordinates(0, 0, 0);
    }

    public World getEntityWorld()
    {
        return this.worlds.get(0); // CraftBukkit
    }

    /**
     * Return the spawn protection area's size.
     */
    public int getSpawnProtectionSize()
    {
        return 16;
    }

    /**
     * Returns true if a player does not have permission to edit the block at the given coordinates.
     */
    public boolean isBlockProtected(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer)
    {
        return false;
    }

    public abstract ILogAgent getLogAgent();

    public void setForceGamemode(boolean par1)
    {
        this.isGamemodeForced = par1;
    }

    public boolean getForceGamemode()
    {
        return this.isGamemodeForced;
    }

    public Proxy getServerProxy()
    {
        return this.serverProxy;
    }

    /**
     * returns the difference, measured in milliseconds, between the current system time and midnight, January 1, 1970
     * UTC.
     */
    public static long getSystemTimeMillis()
    {
        return System.currentTimeMillis();
    }

    public int func_143007_ar()
    {
        return this.field_143008_E;
    }

    public void func_143006_e(int par1)
    {
        this.field_143008_E = par1;
    }

    /**
     * Gets the current player count, maximum player count, and player entity list.
     */
    public static ServerConfigurationManager getServerConfigurationManager(MinecraftServer par0MinecraftServer)
    {
        return par0MinecraftServer.serverConfigManager;
    }

    @SideOnly(Side.SERVER)
    public static void main(String[] par0ArrayOfStr)
    {
        OptionSet options = loadOptions(par0ArrayOfStr);

        if (options == null)
        {
            return;
        }

        cpw.mods.fml.relauncher.FMLLogFormatter.setFormat(options.has("nojline"), options.has("date-format") ? (SimpleDateFormat)options.valueOf("date-format") : null);
        StatList.nopInit();

        try
        {
            if (CauldronUtils.deobfuscatedEnvironment()) useJline = false;
            DedicatedServer dedicatedserver = new DedicatedServer(options);

            if (options.has("port"))
            {
                int port = (Integer) options.valueOf("port");

                if (port > 0)
                {
                    dedicatedserver.setServerPort(port);
                }
            }

            if (options.has("universe"))
            {
                dedicatedserver.anvilFile = (File) options.valueOf("universe");
            }

            if (options.has("world"))
            {
                dedicatedserver.setFolderName((String) options.valueOf("world"));
            }

            dedicatedserver.primaryThread.start();
            // CraftBukkit end
        }
        catch (Exception exception)
        {
            Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to start the minecraft server", exception);
        }
    }

    public static OptionSet loadOptions(String[] args) {
        OptionParser parser = new OptionParser() {
            {
                acceptsAll(Arrays.asList("?", "help"), "Show the help");

                acceptsAll(Arrays.asList("c", "config"), "Properties file to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("server.properties"))
                        .describedAs("Properties file");

                acceptsAll(Arrays.asList("P", "plugins"), "Plugin directory to use")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("plugins"))
                        .describedAs("Plugin directory");

                acceptsAll(Arrays.asList("h", "host", "server-ip"), "Host to listen on")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("Hostname or IP");

                acceptsAll(Arrays.asList("W", "world-dir", "universe", "world-container"), "World container")
                        .withRequiredArg()
                        .ofType(File.class)
                        .describedAs("Directory containing worlds");

                acceptsAll(Arrays.asList("w", "world", "level-name"), "World name")
                        .withRequiredArg()
                        .ofType(String.class)
                        .describedAs("World name");

                acceptsAll(Arrays.asList("p", "port", "server-port"), "Port to listen on")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .describedAs("Port");

                acceptsAll(Arrays.asList("o", "online-mode"), "Whether to use online authentication")
                        .withRequiredArg()
                        .ofType(Boolean.class)
                        .describedAs("Authentication");

                acceptsAll(Arrays.asList("s", "size", "max-players"), "Maximum amount of players")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .describedAs("Server size");

                acceptsAll(Arrays.asList("d", "date-format"), "Format of the date to display in the console (for log entries)")
                        .withRequiredArg()
                        .ofType(SimpleDateFormat.class)
                        .describedAs("Log date format");

                acceptsAll(Arrays.asList("log-pattern"), "Specfies the log filename pattern")
                        .withRequiredArg()
                        .ofType(String.class)
                        .defaultsTo("server.log")
                        .describedAs("Log filename");

                acceptsAll(Arrays.asList("log-limit"), "Limits the maximum size of the log file (0 = unlimited)")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .defaultsTo(0)
                        .describedAs("Max log size");

                acceptsAll(Arrays.asList("log-count"), "Specified how many log files to cycle through")
                        .withRequiredArg()
                        .ofType(Integer.class)
                        .defaultsTo(1)
                        .describedAs("Log count");

                acceptsAll(Arrays.asList("log-append"), "Whether to append to the log file")
                        .withRequiredArg()
                        .ofType(Boolean.class)
                        .defaultsTo(true)
                        .describedAs("Log append");

                acceptsAll(Arrays.asList("log-strip-color"), "Strips color codes from log file");

                acceptsAll(Arrays.asList("b", "bukkit-settings"), "File for bukkit settings")
                        .withRequiredArg()
                        .ofType(File.class)
                        .defaultsTo(new File("bukkit.yml"))
                        .describedAs("Yml file");

                acceptsAll(Arrays.asList("nojline"), "Disables jline and emulates the vanilla console");

                acceptsAll(Arrays.asList("noconsole"), "Disables the console");

                acceptsAll(Arrays.asList("v", "version"), "Show the CraftBukkit Version");

                acceptsAll(Arrays.asList("demo"), "Demo mode");
            }
        };

        OptionSet options = null;

        try {
            options = parser.parse(args);
        } catch (joptsimple.OptionException ex) {
            Logger.getLogger(MinecraftServer.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
        }

        if ((options == null) || (options.has("?"))) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException ex) {
                Logger.getLogger(MinecraftServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                // This trick bypasses Maven Shade's clever rewriting of our getProperty call when using String literals
                String jline_UnsupportedTerminal = new String(new char[] {'j','l','i','n','e','.','U','n','s','u','p','p','o','r','t','e','d','T','e','r','m','i','n','a','l'});
                String jline_terminal = new String(new char[] {'j','l','i','n','e','.','t','e','r','m','i','n','a','l'});

                useJline = !(jline_UnsupportedTerminal).equals(System.getProperty(jline_terminal));

                if (options.has("nojline")) {
                    System.setProperty("user.language", "en");
                    useJline = false;
                }

                if (!useJline) {
                    // This ensures the terminal literal will always match the jline implementation
                    System.setProperty(jline.TerminalFactory.JLINE_TERMINAL, jline.UnsupportedTerminal.class.getName());
                }


                if (options.has("noconsole")) {
                    useConsole = false;
                }
                // Cauldron start - initialize config
                configFile = (File) options.valueOf("bukkit-settings");
                configuration = YamlConfiguration.loadConfiguration(configFile);
                configuration.options().copyDefaults(true);
                configuration.setDefaults(YamlConfiguration.loadConfiguration(MinecraftServer.class.getClassLoader().getResourceAsStream("configurations/bukkit.yml")));
                try {
                    configuration.save(configFile);
                } catch (IOException ex) {
                    Logger.getLogger(MinecraftServer.class.getName()).log(Level.SEVERE, "Could not save " + configFile, ex);
                }
                return options;
                // Cauldron end
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return null; // Cauldron
    }
}