package net.minecraft.server;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.StartupQuery;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;

import javax.imageio.ImageIO;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Bootstrap;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.PropertyManager;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

// CraftBukkit start
import java.io.IOException;

import jline.console.ConsoleReader;
import joptsimple.OptionSet;
import net.minecraft.world.chunk.storage.AnvilSaveHandler;
import org.bukkit.World.Environment;
import org.bukkit.craftbukkit.SpigotTimings; // Spigot
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.world.WorldSaveEvent;
// CraftBukkit end
// Cauldron start
import java.util.Map;
import java.lang.reflect.Constructor;
import joptsimple.OptionParser;
import cpw.mods.fml.common.asm.transformers.SideTransformer;
import net.minecraft.command.ServerCommand;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.cauldron.CauldronUtils;
import net.minecraftforge.cauldron.configuration.CauldronConfig;
import net.minecraftforge.cauldron.configuration.TileEntityConfig;
import net.minecraftforge.common.util.EnumHelper;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.block.CraftBlock;
// Cauldron end

public abstract class MinecraftServer implements ICommandSender, Runnable, IPlayerUsage
{
    private static final Logger logger = LogManager.getLogger();
    public static final File field_152367_a = new File("usercache.json");
    private static MinecraftServer mcServer;
    public ISaveFormat anvilConverterForAnvilFile; // CraftBukkit - private final -> public
    private final PlayerUsageSnooper usageSnooper = new PlayerUsageSnooper("server", this, getSystemTimeMillis());
    public File anvilFile; // CraftBukkit - private final -> public
    private final List tickables = new ArrayList();
    private final ICommandManager commandManager;
    public final Profiler theProfiler = new Profiler();
    private NetworkSystem field_147144_o; // Spigot
    private final ServerStatusResponse field_147147_p = new ServerStatusResponse();
    private final Random field_147146_q = new Random();
    @SideOnly(Side.SERVER)
    private String hostname;
    private int serverPort = -1;
    public WorldServer[] worldServers = new WorldServer[0];
    private ServerConfigurationManager serverConfigManager;
    private boolean serverRunning = true;
    private boolean serverStopped;
    private int tickCounter;
    protected final Proxy serverProxy;
    public String currentTask;
    public int percentDone;
    private boolean onlineMode;
    private boolean canSpawnAnimals;
    private boolean canSpawnNPCs;
    private boolean pvpEnabled;
    private boolean allowFlight;
    private String motd;
    private int buildLimit;
    private int field_143008_E = 0;
    public final long[] tickTimeArray = new long[100];
    //public long[][] timeOfLastDimensionTick;
    public Hashtable<Integer, long[]> worldTickTimes = new Hashtable<Integer, long[]>();
    private KeyPair serverKeyPair;
    private String serverOwner;
    private String folderName;
    @SideOnly(Side.CLIENT)
    private String worldName;
    private boolean isDemo;
    private boolean enableBonusChest;
    private boolean worldIsBeingDeleted;
    private String field_147141_M = "";
    private boolean serverIsRunning;
    private long timeOfLastWarning;
    private String userMessage;
    private boolean startProfiling;
    private boolean isGamemodeForced;
    private final YggdrasilAuthenticationService field_152364_T;
    private final MinecraftSessionService field_147143_S;
    private long field_147142_T = 0L;
    private final GameProfileRepository field_152365_W;
    private final PlayerProfileCache field_152366_X;
    // CraftBukkit start
    public List<WorldServer> worlds = new ArrayList<WorldServer>();
    public org.bukkit.craftbukkit.CraftServer server;
    public static OptionSet options; // Cauldron
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
    public final double[] recentTps = new double[ 3 ];
    // Spigot end
    // Cauldron start
    public static CauldronConfig cauldronConfig;
    public static TileEntityConfig tileEntityConfig;
    public static YamlConfiguration configuration;
    public static YamlConfiguration commandsConfiguration;
    public static File configFile;
    public static File commandFile;
    public static double currentTps = 0;
    public static boolean useJline = true;
    public static boolean useConsole = true;
    public static boolean callingForgeTick = false;
    public static List<Class<? extends TileEntity>> bannedTileEntityUpdates = new ArrayList<Class<? extends TileEntity>>();
    // Cauldron end
    private static final String __OBFID = "CL_00001462";

    // Cauldron start - IntegratedServer requires this
    public MinecraftServer(File p_i45281_1_, Proxy p_i45281_2_)
    {
        this.field_152366_X = new PlayerProfileCache(this, field_152367_a);
        mcServer = this;
        this.serverProxy = p_i45281_2_;
        this.anvilFile = p_i45281_1_;
        this.field_147144_o = new NetworkSystem(this);
        this.commandManager = new ServerCommandManager();
        this.anvilConverterForAnvilFile = new AnvilSaveConverter(p_i45281_1_);
        this.field_152364_T = new YggdrasilAuthenticationService(p_i45281_2_, UUID.randomUUID().toString());
        this.field_147143_S = this.field_152364_T.createMinecraftSessionService();
        this.field_152365_W = this.field_152364_T.createProfileRepository();
        this.primaryThread = new Thread(this, "Server thread"); // CraftBukkit
        this.cauldronConfig = new CauldronConfig("cauldron.yml", "cauldron");
        this.tileEntityConfig = new TileEntityConfig("tileentities.yml", "cauldron_te");
    }
    // Cauldron end

    public MinecraftServer(OptionSet options, Proxy proxy)   // CraftBukkit - signature file -> OptionSet
    {
        this.field_152366_X = new PlayerProfileCache(this, field_152367_a);
        mcServer = this;
        this.serverProxy = proxy;
        // this.anvilFile = p_i45281_1_; // CraftBukkit
        // this.field_147144_o = new NetworkSystem(this); // Spigot
        this.commandManager = new ServerCommandManager();
        // this.anvilConverterForAnvilFile = new AnvilSaveConverter(p_i45281_1_);  // CraftBukkit - moved to DedicatedServer.init
        this.field_152364_T = new YggdrasilAuthenticationService(proxy, UUID.randomUUID().toString());
        this.field_147143_S = this.field_152364_T.createMinecraftSessionService();
        this.field_152365_W = this.field_152364_T.createProfileRepository();
        // Cauldron start
        this.cauldronConfig = new CauldronConfig("cauldron.yml", "cauldron");
        this.tileEntityConfig = new TileEntityConfig("tileentities.yml", "cauldron_te");
        // Cauldron end
        // CraftBukkit start
        this.options = options;
        // Try to see if we're actually running in a terminal, disable jline if not
        if (System.console() == null)
        {
            System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
            this.useJline = false; // Cauldron
        }

        try
        {
            this.reader = new ConsoleReader(System.in, System.out);
            this.reader.setExpandEvents(false); // Avoid parsing exceptions for uncommonly used event designators
        }
        catch (Throwable e)
        {
            try
            {
                // Try again with jline disabled for Windows users without C++ 2008 Redistributable
                System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
                System.setProperty("user.language", "en");
                this.useJline = false; // Cauldron
                this.reader = new ConsoleReader(System.in, System.out);
                this.reader.setExpandEvents(false);
            }
            catch (IOException ex)
            {
                logger.warn((String) null, ex);
            }
        }
        net.minecraftforge.cauldron.CauldronHooks.enableThreadContentionMonitoring();
        Runtime.getRuntime().addShutdownHook(new org.bukkit.craftbukkit.util.ServerShutdownThread(this));
        primaryThread = new Thread(this, "Server thread"); // Moved from main
    }
    
    public abstract PropertyManager getPropertyManager();
    // CraftBukkit end

    protected abstract boolean startServer() throws java.net.UnknownHostException; // CraftBukkit - throws UnknownHostException

    protected void convertMapIfNeeded(String p_71237_1_)
    {
        if (this.getActiveAnvilConverter().isOldMapFormat(p_71237_1_))
        {
            logger.info("Converting map!");
            this.setUserMessage("menu.convertingLevel");
            this.getActiveAnvilConverter().convertMapFormat(p_71237_1_, new IProgressUpdate()
            {
                private long field_96245_b = System.currentTimeMillis();
                private static final String __OBFID = "CL_00001417";
                public void displayProgressMessage(String p_73720_1_) {}
                public void setLoadingProgress(int p_73718_1_)
                {
                    if (System.currentTimeMillis() - this.field_96245_b >= 1000L)
                    {
                        this.field_96245_b = System.currentTimeMillis();
                        MinecraftServer.logger.info("Converting... " + p_73718_1_ + "%");
                    }
                }
                
                @SideOnly(Side.CLIENT)
                public void resetProgressAndMessage(String p_73721_1_) {}
                @SideOnly(Side.CLIENT)
                public void func_146586_a() {}
                public void resetProgresAndWorkingMessage(String p_73719_1_) {}
            });
        }
    }

    protected synchronized void setUserMessage(String p_71192_1_)
    {
        this.userMessage = p_71192_1_;
    }

    @SideOnly(Side.CLIENT)

    public synchronized String getUserMessage()
    {
        return this.userMessage;
    }

    protected void loadAllWorlds(String p_71247_1_, String p_71247_2_, long p_71247_3_, WorldType p_71247_5_, String p_71247_6_)
    {
        // Cauldron start - register vanilla server commands
        ServerCommandManager vanillaCommandManager = (ServerCommandManager)this.getCommandManager();
        vanillaCommandManager.registerVanillaCommands();
        // Cauldron end
        this.convertMapIfNeeded(p_71247_1_);
        this.setUserMessage("menu.loadingLevel");
        // Cauldron start - SaveHandler/WorldInfo below are not used and must be disabled to prevent FML receiving different handlers for overworld
        //ISaveHandler isavehandler = this.anvilConverterForAnvilFile.getSaveLoader(p_71247_1_, true);
        //WorldInfo worldinfo = isavehandler.loadWorldInfo();
        // Cauldron end
        /* CraftBukkit start - Removed worldsettings
        WorldSettings worldsettings;

        if (worldinfo == null)
        {
            worldsettings = new WorldSettings(p_71247_3_, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), p_71247_5_);
            worldsettings.func_82750_a(p_71247_6_);
        }
        else
        {
            worldsettings = new WorldSettings(worldinfo);
        }

        if (this.enableBonusChest)
        {
            worldsettings.enableBonusChest();
        }
        // */

        WorldSettings worldsettings = new WorldSettings(p_71247_3_, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), p_71247_5_);
        worldsettings.func_82750_a(p_71247_6_);
        WorldServer world;

        // Cauldron - overworld generator is handled in World after plugins load
        WorldServer overWorld = (isDemo() ? new DemoWorldServer(this, new AnvilSaveHandler(server.getWorldContainer(), p_71247_2_, true), p_71247_2_, 0, theProfiler) : new WorldServer(this, new AnvilSaveHandler(server.getWorldContainer(), p_71247_2_, true), p_71247_2_, 0, worldsettings, theProfiler, Environment.getEnvironment(0), null));

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
                    oldName = p_71247_1_ + "_" + worldType;
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
                worldsettings = new WorldSettings(p_71247_3_, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), p_71247_5_);
                worldsettings.func_82750_a(p_71247_6_);

                CauldronUtils.migrateWorlds(worldType, oldName, p_71247_1_, name);

                this.setUserMessage(name);
            }

            world = (dimension == 0 ? overWorld : new WorldServerMulti(this, new AnvilSaveHandler(server.getWorldContainer(), name, true), name, dimension, worldsettings, overWorld, this.theProfiler, env, gen));
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
        this.func_147139_a(this.func_147135_j());
        this.initialWorldChunkLoad();
        CraftBlock.dumpMaterials();
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
        logger.info("Preparing start region for level " + b0);
        WorldServer worldserver = this.worldServers[b0];
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

    public abstract WorldSettings.GameType getGameType();

    public abstract EnumDifficulty func_147135_j();

    public abstract boolean isHardcore();

    public abstract int getOpPermissionLevel();

    public abstract boolean func_152363_m();

    protected void outputPercentRemaining(String p_71216_1_, int p_71216_2_)
    {
        this.currentTask = p_71216_1_;
        this.percentDone = p_71216_2_;
        logger.info(p_71216_1_ + ": " + p_71216_2_ + "%");
    }

    protected void clearCurrentTask()
    {
        this.currentTask = null;
        this.percentDone = 0;
        this.server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.POSTWORLD); // CraftBukkit
    }

    protected void saveAllWorlds(boolean p_71267_1_) throws MinecraftException   // CraftBukkit - added throws
    {
        if (!this.worldIsBeingDeleted)
        {
            // CraftBukkit start
            for (int j = 0; j < this.worlds.size(); ++j)
            {
                WorldServer worldserver = this.worlds.get(j);

                if (worldserver != null)
                {
                    if (!p_71267_1_)
                    {
                        logger.info("Saving chunks for level \'" + worldserver.getWorldInfo().getWorldName() + "\'/" + worldserver.provider.getDimensionName());
                    }

                    worldserver.saveAllChunks(true, (IProgressUpdate) null);
                    worldserver.flush();
                    WorldSaveEvent event = new WorldSaveEvent(worldserver.getWorld());
                    this.server.getPluginManager().callEvent(event);
                    // Cauldron start - save world configs
                    if (worldserver.cauldronConfig != null)
                    {
                        worldserver.cauldronConfig.save();
                    }
                    if (worldserver.tileentityConfig != null)
                    {
                        worldserver.tileentityConfig.save();
                    }
                    // Cauldron end
                }
            }

            // CraftBukkit end
        }
    }

    public void stopServer() throws MinecraftException // CraftBukkit - added throws
    {
        if (!this.worldIsBeingDeleted && Loader.instance().hasReachedState(LoaderState.SERVER_STARTED) && !serverStopped) // make sure the save is valid and we don't save twice
        {
            logger.info("Stopping server");

            // CraftBukkit start
            if (this.server != null)
            {
                this.server.disablePlugins();
            }

            // CraftBukkit end

            if (this.func_147137_ag() != null)
            {
                this.func_147137_ag().terminateEndpoints();
            }

            if (this.serverConfigManager != null)
            {
                logger.info("Saving players");
                this.serverConfigManager.saveAllPlayerData();
                this.serverConfigManager.removeAllPlayers();
            }

            if (this.worldServers != null)
            {
                logger.info("Saving worlds");
                try
                {
                    this.saveAllWorlds(false);
                }
                catch (MinecraftException e)
                {
                    e.printStackTrace();
                }

                for (int i = 0; i < this.worldServers.length; ++i)
                {
                    WorldServer worldserver = this.worldServers[i];
                    MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(worldserver));
                    worldserver.flush();
                }

                WorldServer[] tmp = worldServers;
                for (WorldServer world : tmp)
                {
                    DimensionManager.setWorld(world.provider.dimensionId, null);
                }
            }

            if (this.usageSnooper.isSnooperRunning())
            {
                this.usageSnooper.stopSnooper();
            }
        }
    }

    public boolean isServerRunning()
    {
        return this.serverRunning;
    }

    public void initiateShutdown()
    {
        this.serverRunning = false;
    }

    // Spigot Start
    private static double calcTps(double avg, double exp, double tps)
    {
        return (avg * exp) + (tps * (1 - exp));
    }
    // Spigot End

    public void run()
    {
        try
        {
            if (this.startServer())
            {
                FMLCommonHandler.instance().handleServerStarted();
                long i = getSystemTimeMillis();
                long l = 0L;
                this.field_147147_p.func_151315_a(new ChatComponentText(this.motd));
                this.field_147147_p.func_151321_a(new ServerStatusResponse.MinecraftProtocolVersionIdentifier("1.7.10", 5));
                this.func_147138_a(this.field_147147_p);
                DedicatedServer.allowPlayerLogins = true; // Cauldron - server is ready, allow player logins
                // Spigot start
                Arrays.fill(recentTps, 20);
                long lastTick = 0, catchupTime = 0, curTime, wait;

                while (this.serverRunning)
                {
                    curTime = System.nanoTime();
                    wait = TICK_TIME - (curTime - lastTick) - catchupTime;

                    if (wait > 0)
                    {
                        Thread.sleep(wait / 1000000);
                        catchupTime = 0;
                        continue;
                    }
                    else
                    {
                        catchupTime = Math.min(1000000000, Math.abs(wait));
                    }

                    if (MinecraftServer.currentTick++ % 100 == 0)
                    {
                        currentTps = 1E9 / (curTime - lastTick);
                        recentTps[0] = calcTps(recentTps[0], 0.92, currentTps);   // 1/exp(5sec/1min)
                        recentTps[1] = calcTps(recentTps[1], 0.9835, currentTps);   // 1/exp(5sec/5min)
                        recentTps[2] = calcTps(recentTps[2], 0.9945, currentTps);   // 1/exp(5sec/15min)
                    }

                    lastTick = curTime;
                    this.tick();
                    this.serverIsRunning = true;
                }

                // Spigot end
                FMLCommonHandler.instance().handleServerStopping();
                FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
            }
            else
            {
                FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
                this.finalTick((CrashReport)null);
            }
        }
        catch (StartupQuery.AbortedException e)
        {
            // ignore silently
            FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
        }
        catch (Throwable throwable1)
        {
            logger.error("Encountered an unexpected exception", throwable1);

            // Spigot Start
            if (throwable1.getCause() != null)
            {
                logger.error("\tCause of unexpected exception was", throwable1.getCause());
            }

            // Spigot End
            CrashReport crashreport = null;

            if (throwable1 instanceof ReportedException)
            {
                crashreport = this.addServerInfoToCrashReport(((ReportedException)throwable1).getCrashReport());
            }
            else
            {
                crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable1));
            }

            File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");

            if (crashreport.saveToFile(file1))
            {
                logger.error("This crash report has been saved to: " + file1.getAbsolutePath());
            }
            else
            {
                logger.error("We were unable to save this crash report to disk.");
            }

            FMLCommonHandler.instance().expectServerStopped(); // has to come before finalTick to avoid race conditions
            this.finalTick(crashreport);
        }
        finally
        {
            try
            {
                org.spigotmc.WatchdogThread.doStop(); // Spigot
                this.stopServer();
                this.serverStopped = true;
            }
            catch (Throwable throwable)
            {
                logger.error("Exception stopping the server", throwable);
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

    private void func_147138_a(ServerStatusResponse p_147138_1_)
    {
        File file1 = this.getFile("server-icon.png");

        if (file1.isFile())
        {
            ByteBuf bytebuf = Unpooled.buffer();

            try
            {
                BufferedImage bufferedimage = ImageIO.read(file1);
                Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide", new Object[0]);
                Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high", new Object[0]);
                ImageIO.write(bufferedimage, "PNG", new ByteBufOutputStream(bytebuf));
                ByteBuf bytebuf1 = Base64.encode(bytebuf);
                p_147138_1_.func_151320_a("data:image/png;base64," + bytebuf1.toString(Charsets.UTF_8));
            }
            catch (Exception exception)
            {
                logger.error("Couldn\'t load server icon", exception);
            }
            finally
            {
                bytebuf.release();
            }
        }
    }

    protected File getDataDirectory()
    {
        return new File(".");
    }

    protected void finalTick(CrashReport p_71228_1_) {}

    protected void systemExitNow() {}

    public void tick()
    {
        SpigotTimings.serverTickTimer.startTiming(); // Spigot
        long i = System.nanoTime();
        callingForgeTick = true; // Cauldron start - handle loadOnProviderRequests during forge tick event
        FMLCommonHandler.instance().onPreServerTick();
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

        if (i - this.field_147142_T >= 5000000000L)
        {
            this.field_147142_T = i;
            this.field_147147_p.func_151319_a(new ServerStatusResponse.PlayerCountData(this.getMaxPlayers(), this.getCurrentPlayerCount()));
            GameProfile[] agameprofile = new GameProfile[Math.min(this.getCurrentPlayerCount(), 12)];
            int j = MathHelper.getRandomIntegerInRange(this.field_147146_q, 0, this.getCurrentPlayerCount() - agameprofile.length);

            for (int k = 0; k < agameprofile.length; ++k)
            {
                agameprofile[k] = ((EntityPlayerMP)this.serverConfigManager.playerEntityList.get(j + k)).getGameProfile();
            }

            Collections.shuffle(Arrays.asList(agameprofile));
            this.field_147147_p.func_151318_b().func_151330_a(agameprofile);
        }

        if ((this.autosavePeriod > 0) && ((this.tickCounter % this.autosavePeriod) == 0))   // CraftBukkit
        {
            SpigotTimings.worldSaveTimer.startTiming(); // Spigot
            this.theProfiler.startSection("save");
            this.serverConfigManager.saveAllPlayerData();
            try
            {
                this.saveAllWorlds(true);
            }
            catch (MinecraftException e)
            {
                e.printStackTrace();
            }
            this.theProfiler.endSection();
            SpigotTimings.worldSaveTimer.stopTiming(); // Spigot
        }

        this.theProfiler.startSection("tallying");
        this.tickTimeArray[this.tickCounter % 100] = System.nanoTime() - i;
        this.theProfiler.endSection();
        this.theProfiler.startSection("snooper");

        if (isSnooperEnabled() && !this.usageSnooper.isSnooperRunning() && this.tickCounter > 100)   // Spigot
        {
            this.usageSnooper.startSnooper();
        }

        if (isSnooperEnabled() && this.tickCounter % 6000 == 0)   // Spigot
        {
            this.usageSnooper.addMemoryStatsToSnooper();
        }

        this.theProfiler.endSection();
        this.theProfiler.endSection();
        callingForgeTick = true; // Cauldron start - handle loadOnProviderRequests during forge tick event
        FMLCommonHandler.instance().onPostServerTick();
        callingForgeTick = false; // Cauldron end
        SpigotTimings.serverTickTimer.stopTiming(); // Spigot
        org.spigotmc.CustomTimingsHandler.tick(); // Spigot
    }

    public void updateTimeLightAndEntities()
    {
        this.theProfiler.startSection("levels");
        SpigotTimings.schedulerTimer.startTiming(); // Spigot
        // CraftBukkit start
        this.server.getScheduler().mainThreadHeartbeat(this.tickCounter);
        SpigotTimings.schedulerTimer.stopTiming(); // Spigot

        // Run tasks that are waiting on processing
        SpigotTimings.processQueueTimer.startTiming(); // Spigot
        while (!processQueue.isEmpty())
        {
            processQueue.remove().run();
        }
        SpigotTimings.processQueueTimer.stopTiming(); // Spigot

        SpigotTimings.chunkIOTickTimer.startTiming(); // Spigot
        net.minecraftforge.common.chunkio.ChunkIOExecutor.tick();
        SpigotTimings.chunkIOTickTimer.stopTiming(); // Spigot

        SpigotTimings.timeUpdateTimer.startTiming(); // Spigot
        // Send time updates to everyone, it will get the right time from the world the player is in.
        if (this.tickCounter % 20 == 0)
        {
            for (int i = 0; i < this.getConfigurationManager().playerEntityList.size(); ++i)
            {
                EntityPlayerMP entityplayermp = (EntityPlayerMP) this.getConfigurationManager().playerEntityList.get(i);
                entityplayermp.playerNetServerHandler.sendPacket(new S03PacketTimeUpdate(entityplayermp.worldObj.getTotalWorldTime(), entityplayermp.getPlayerTime(), entityplayermp.worldObj.getGameRules().getGameRuleBooleanValue("doDaylightCycle"))); // Add support for per player time
            }
        }
        SpigotTimings.timeUpdateTimer.stopTiming(); // Spigot

        int i;

        Integer[] ids = DimensionManager.getIDs(this.tickCounter % 200 == 0);
        for (int x = 0; x < ids.length; x++)
        {
            int id = ids[x];
            long j = System.nanoTime();

            // CraftBukkit start
            //if (id == 0 || this.getAllowNether())
            //{
                WorldServer worldserver = DimensionManager.getWorld(id);
                this.theProfiler.startSection(worldserver.getWorldInfo().getWorldName());
                this.theProfiler.startSection("pools");
                this.theProfiler.endSection();
                /* Drop global time updates
                if (this.tickCounter % 20 == 0)
                {
                    this.theProfiler.startSection("timeSync");
                    this.serverConfigManager.sendPacketToAllPlayersInDimension(new S03PacketTimeUpdate(worldserver.getTotalWorldTime(), worldserver.getWorldTime(), worldserver.getGameRules().getGameRuleBooleanValue("doDaylightCycle")), worldserver.provider.dimensionId);
                    this.theProfiler.endSection();
                }
                // CraftBukkit end */

                this.theProfiler.startSection("tick");
                FMLCommonHandler.instance().onPreWorldTick(worldserver);
                CrashReport crashreport;

                try
                {
                    worldserver.timings.doTick.startTiming(); // Spigot
                    worldserver.tick();
                    worldserver.timings.doTick.stopTiming(); // Spigot
                }
                catch (Throwable throwable1)
                {
                    // Spigot Start
                    try
                    {
                        crashreport = CrashReport.makeCrashReport(throwable1, "Exception ticking world");
                    }
                    catch (Throwable t)
                    {
                        throw new RuntimeException("Error generating crash report", t);
                    }
    
                    // Spigot End
                    worldserver.addWorldInfoToCrashReport(crashreport);
                    throw new ReportedException(crashreport);
                }

                try
                {
                    worldserver.timings.tickEntities.startTiming(); // Spigot
                    worldserver.updateEntities();
                    worldserver.timings.tickEntities.stopTiming(); // Spigot
                }
                catch (Throwable throwable)
                {
                    // Spigot Start
                    try
                    {
                        crashreport = CrashReport.makeCrashReport(throwable, "Exception ticking world entities");
                    }
                    catch (Throwable t)
                    {
                        throw new RuntimeException("Error generating crash report", t);
                    }
    
                    // Spigot End
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
            // } // CraftBukkit

            worldTickTimes.get(id)[this.tickCounter % 100] = System.nanoTime() - j;
        }

        this.theProfiler.endStartSection("dim_unloading");
        DimensionManager.unloadWorlds(worldTickTimes);
        this.theProfiler.endStartSection("connection");
        SpigotTimings.connectionTimer.startTiming(); // Spigot
        this.func_147137_ag().networkTick();
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
        StartupQuery.reset();
        (new Thread("Server thread")
        {
            private static final String __OBFID = "CL_00001418";
            public void run()
            {
                MinecraftServer.this.run();
            }
        }).start();
    }

    public File getFile(String p_71209_1_)
    {
        return new File(this.getDataDirectory(), p_71209_1_);
    }

    public void logWarning(String p_71236_1_)
    {
        logger.warn(p_71236_1_);
    }

    public WorldServer worldServerForDimension(int p_71218_1_)
    {
        // Cauldron start - this is required for MystCraft agebooks to teleport correctly
        // verify the nether or the end is allowed, and if not return overworld
        if ((p_71218_1_ == -1 && !this.getAllowNether()) || (p_71218_1_ == 1 && !this.server.getAllowEnd()))
        {
            return DimensionManager.getWorld(0);
        }
        // Cauldron end
        WorldServer ret = DimensionManager.getWorld(p_71218_1_);
        if (ret == null)
        {
            DimensionManager.initDimension(p_71218_1_);
            ret = DimensionManager.getWorld(p_71218_1_);
        }
        return ret;
    }

    public String getMinecraftVersion()
    {
        return "1.7.10";
    }

    public int getCurrentPlayerCount()
    {
        return this.serverConfigManager.getCurrentPlayerCount();
    }

    public int getMaxPlayers()
    {
        return this.serverConfigManager.getMaxPlayers();
    }

    public String[] getAllUsernames()
    {
        return this.serverConfigManager.getAllUsernames();
    }

    public GameProfile[] func_152357_F()
    {
        return this.serverConfigManager.func_152600_g();
    }

    public String getServerModName()
    {
        return FMLCommonHandler.instance().getModName();
    }

    public CrashReport addServerInfoToCrashReport(CrashReport p_71230_1_)
    {
        p_71230_1_.getCategory().addCrashSectionCallable("Profiler Position", new Callable()
        {
            private static final String __OBFID = "CL_00001419";
            public String call()
            {
                return MinecraftServer.this.theProfiler.profilingEnabled ? MinecraftServer.this.theProfiler.getNameOfLastSection() : "N/A (disabled)";
            }
        });

        if (this.worldServers != null && this.worldServers.length > 0 && this.worldServers[0] != null)
        {
            p_71230_1_.getCategory().addCrashSectionCallable("Vec3 Pool Size", new Callable()
            {
                private static final String __OBFID = "CL_00001420";
                public String call()
                {
                    byte b0 = 0;
                    int i = 56 * b0;
                    int j = i / 1024 / 1024;
                    byte b1 = 0;
                    int k = 56 * b1;
                    int l = k / 1024 / 1024;
                    return b0 + " (" + i + " bytes; " + j + " MB) allocated, " + b1 + " (" + k + " bytes; " + l + " MB) used";
                }
            });
        }

        if (this.serverConfigManager != null)
        {
            p_71230_1_.getCategory().addCrashSectionCallable("Player Count", new Callable()
            {
                private static final String __OBFID = "CL_00001780";
                public String call()
                {
                    return MinecraftServer.this.serverConfigManager.getCurrentPlayerCount() + " / " + MinecraftServer.this.serverConfigManager.getMaxPlayers() + "; " + MinecraftServer.this.serverConfigManager.playerEntityList;
                }
            });
        }

        return p_71230_1_;
    }

    public List getPossibleCompletions(ICommandSender p_71248_1_, String p_71248_2_)
    {
        // Cauldron start - add mod commands to list then pass to bukkit
        java.util.HashSet arraylist = new java.util.HashSet(); // use a set here to avoid duplicates

        if (p_71248_2_.startsWith("/"))
        {
            String char1 = p_71248_2_.substring(1); // rename var to avoid removing slash from passed message
            boolean flag = !char1.contains(" ");
            List list = this.commandManager.getPossibleCommands(p_71248_1_, char1);

            if (list != null)
            {
                Iterator iterator = list.iterator();

                while (iterator.hasNext())
                {
                    String command = (String)iterator.next();

                    if (flag)
                    {
                        arraylist.add("/" + command);
                    }
                    else
                    {
                        arraylist.add(command);
                    }
                }
            }
        }

        arraylist.addAll(this.server.tabComplete(p_71248_1_, p_71248_2_));  // add craftbukkit commands
        ArrayList completions = new ArrayList(arraylist);
        Collections.sort(completions); // sort the final list
        return completions;
        // Cauldron end
    }

    public static MinecraftServer getServer()
    {
        return mcServer;
    }

    public String getCommandSenderName()
    {
        return "Server";
    }

    public void addChatMessage(IChatComponent p_145747_1_)
    {
        logger.info(p_145747_1_.getUnformattedText());
    }

    public boolean canCommandSenderUseCommand(int p_70003_1_, String p_70003_2_)
    {
        return true;
    }

    public ICommandManager getCommandManager()
    {
        return this.commandManager;
    }

    public KeyPair getKeyPair()
    {
        return this.serverKeyPair;
    }

    public String getServerOwner()
    {
        return this.serverOwner;
    }

    public void setServerOwner(String p_71224_1_)
    {
        this.serverOwner = p_71224_1_;
    }

    public boolean isSinglePlayer()
    {
        return this.serverOwner != null;
    }

    public String getFolderName()
    {
        return this.folderName;
    }

    public void setFolderName(String p_71261_1_)
    {
        this.folderName = p_71261_1_;
    }

    @SideOnly(Side.CLIENT)
    public void setWorldName(String p_71246_1_)
    {
        this.worldName = p_71246_1_;
    }

    @SideOnly(Side.CLIENT)
    public String getWorldName()
    {
        return this.worldName;
    }

    public void setKeyPair(KeyPair p_71253_1_)
    {
        this.serverKeyPair = p_71253_1_;
    }

    public void func_147139_a(EnumDifficulty p_147139_1_)
    {
        for (int i = 0; i < this.worldServers.length; ++i)
        {
            WorldServer worldserver = this.worldServers[i];

            if (worldserver != null)
            {
                if (worldserver.getWorldInfo().isHardcoreModeEnabled())
                {
                    worldserver.difficultySetting = EnumDifficulty.HARD;
                    worldserver.setAllowedSpawnTypes(true, true);
                }
                else if (this.isSinglePlayer())
                {
                    worldserver.difficultySetting = p_147139_1_;
                    worldserver.setAllowedSpawnTypes(worldserver.difficultySetting != EnumDifficulty.PEACEFUL, true);
                }
                else
                {
                    worldserver.difficultySetting = p_147139_1_;
                    worldserver.setAllowedSpawnTypes(this.allowSpawnMonsters(), this.canSpawnAnimals);
                }
            }
        }
    }

    protected boolean allowSpawnMonsters()
    {
        return true;
    }

    public boolean isDemo()
    {
        return this.isDemo;
    }

    public void setDemo(boolean p_71204_1_)
    {
        this.isDemo = p_71204_1_;
    }

    public void canCreateBonusChest(boolean p_71194_1_)
    {
        this.enableBonusChest = p_71194_1_;
    }

    public ISaveFormat getActiveAnvilConverter()
    {
        return this.anvilConverterForAnvilFile;
    }

    public void deleteWorldAndStopServer()
    {
        this.worldIsBeingDeleted = true;
        this.getActiveAnvilConverter().flushCache();

        for (int i = 0; i < this.worldServers.length; ++i)
        {
            WorldServer worldserver = this.worldServers[i];

            if (worldserver != null)
            {
                MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(worldserver));
                worldserver.flush();
            }
        }

        this.getActiveAnvilConverter().deleteWorldDirectory(this.worldServers[0].getSaveHandler().getWorldDirectoryName());
        this.initiateShutdown();
    }

    public String getTexturePack()
    {
        return this.field_147141_M;
    }

    public void addServerStatsToSnooper(PlayerUsageSnooper p_70000_1_)
    {
        p_70000_1_.func_152768_a("whitelist_enabled", Boolean.valueOf(false));
        p_70000_1_.func_152768_a("whitelist_count", Integer.valueOf(0));
        p_70000_1_.func_152768_a("players_current", Integer.valueOf(this.getCurrentPlayerCount()));
        p_70000_1_.func_152768_a("players_max", Integer.valueOf(this.getMaxPlayers()));
        p_70000_1_.func_152768_a("players_seen", Integer.valueOf(this.serverConfigManager.getAvailablePlayerDat().length));
        p_70000_1_.func_152768_a("uses_auth", Boolean.valueOf(this.onlineMode));
        p_70000_1_.func_152768_a("gui_state", this.getGuiEnabled() ? "enabled" : "disabled");
        p_70000_1_.func_152768_a("run_time", Long.valueOf((getSystemTimeMillis() - p_70000_1_.getMinecraftStartTimeMillis()) / 60L * 1000L));
        p_70000_1_.func_152768_a("avg_tick_ms", Integer.valueOf((int)(MathHelper.average(this.tickTimeArray) * 1.0E-6D)));
        int i = 0;

        for (int j = 0; j < this.worldServers.length; ++j)
        {
            if (this.worldServers[j] != null)
            {
                WorldServer worldserver = this.worldServers[j];
                WorldInfo worldinfo = worldserver.getWorldInfo();
                p_70000_1_.func_152768_a("world[" + i + "][dimension]", Integer.valueOf(worldserver.provider.dimensionId));
                p_70000_1_.func_152768_a("world[" + i + "][mode]", worldinfo.getGameType());
                p_70000_1_.func_152768_a("world[" + i + "][difficulty]", worldserver.difficultySetting);
                p_70000_1_.func_152768_a("world[" + i + "][hardcore]", Boolean.valueOf(worldinfo.isHardcoreModeEnabled()));
                p_70000_1_.func_152768_a("world[" + i + "][generator_name]", worldinfo.getTerrainType().getWorldTypeName());
                p_70000_1_.func_152768_a("world[" + i + "][generator_version]", Integer.valueOf(worldinfo.getTerrainType().getGeneratorVersion()));
                p_70000_1_.func_152768_a("world[" + i + "][height]", Integer.valueOf(this.buildLimit));
                p_70000_1_.func_152768_a("world[" + i + "][chunks_loaded]", Integer.valueOf(worldserver.getChunkProvider().getLoadedChunkCount()));
                ++i;
            }
        }

        p_70000_1_.func_152768_a("worlds", Integer.valueOf(i));
    }

    public void addServerTypeToSnooper(PlayerUsageSnooper p_70001_1_)
    {
        p_70001_1_.func_152767_b("singleplayer", Boolean.valueOf(this.isSinglePlayer()));
        p_70001_1_.func_152767_b("server_brand", this.getServerModName());
        p_70001_1_.func_152767_b("gui_supported", GraphicsEnvironment.isHeadless() ? "headless" : "supported");
        p_70001_1_.func_152767_b("dedicated", Boolean.valueOf(this.isDedicatedServer()));
    }

    public boolean isSnooperEnabled()
    {
        return true;
    }

    public abstract boolean isDedicatedServer();

    public boolean isServerInOnlineMode()
    {
        return this.server.getOnlineMode(); // CraftBukkit
    }

    public void setOnlineMode(boolean p_71229_1_)
    {
        this.onlineMode = p_71229_1_;
    }

    public boolean getCanSpawnAnimals()
    {
        return this.canSpawnAnimals;
    }

    public void setCanSpawnAnimals(boolean p_71251_1_)
    {
        this.canSpawnAnimals = p_71251_1_;
    }

    public boolean getCanSpawnNPCs()
    {
        return this.canSpawnNPCs;
    }

    public void setCanSpawnNPCs(boolean p_71257_1_)
    {
        this.canSpawnNPCs = p_71257_1_;
    }

    public boolean isPVPEnabled()
    {
        return this.pvpEnabled;
    }

    public void setAllowPvp(boolean p_71188_1_)
    {
        this.pvpEnabled = p_71188_1_;
    }

    public boolean isFlightAllowed()
    {
        return this.allowFlight;
    }

    public void setAllowFlight(boolean p_71245_1_)
    {
        this.allowFlight = p_71245_1_;
    }

    public abstract boolean isCommandBlockEnabled();

    public String getMOTD()
    {
        return this.motd;
    }

    public void setMOTD(String p_71205_1_)
    {
        this.motd = p_71205_1_;
    }

    public int getBuildLimit()
    {
        return this.buildLimit;
    }

    public void setBuildLimit(int p_71191_1_)
    {
        this.buildLimit = p_71191_1_;
    }

    public ServerConfigurationManager getConfigurationManager()
    {
        return this.serverConfigManager;
    }

    public void func_152361_a(ServerConfigurationManager p_152361_1_)
    {
        this.serverConfigManager = p_152361_1_;
    }

    public void setGameType(WorldSettings.GameType p_71235_1_)
    {
        for (int i = 0; i < this.worldServers.length; ++i)
        {
            getServer().worldServers[i].getWorldInfo().setGameType(p_71235_1_);
        }
    }

    public NetworkSystem func_147137_ag()
    {
        return (this.field_147144_o) == null ? this.field_147144_o = new NetworkSystem(this) : this.field_147144_o;     // Spigot
    }

    @SideOnly(Side.CLIENT)
    public boolean serverIsInRunLoop()
    {
        return this.serverIsRunning;
    }

    public boolean getGuiEnabled()
    {
        return false;
    }

    public abstract String shareToLAN(WorldSettings.GameType p_71206_1_, boolean p_71206_2_);

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

    public ChunkCoordinates getPlayerCoordinates()
    {
        return new ChunkCoordinates(0, 0, 0);
    }

    public World getEntityWorld()
    {
        return this.worldServers[0];
    }

    public int getSpawnProtectionSize()
    {
        return 16;
    }

    public boolean isBlockProtected(World p_96290_1_, int p_96290_2_, int p_96290_3_, int p_96290_4_, EntityPlayer p_96290_5_)
    {
        return false;
    }

    public boolean getForceGamemode()
    {
        return this.isGamemodeForced;
    }

    public Proxy getServerProxy()
    {
        return this.serverProxy;
    }

    public static long getSystemTimeMillis()
    {
        return System.currentTimeMillis();
    }

    public int func_143007_ar()
    {
        return this.field_143008_E;
    }

    public void func_143006_e(int p_143006_1_)
    {
        this.field_143008_E = p_143006_1_;
    }

    public IChatComponent func_145748_c_()
    {
        return new ChatComponentText(this.getCommandSenderName());
    }

    public boolean func_147136_ar()
    {
        return true;
    }

    public MinecraftSessionService func_147130_as()
    {
        return this.field_147143_S;
    }

    public GameProfileRepository func_152359_aw()
    {
        return this.field_152365_W;
    }

    public PlayerProfileCache func_152358_ax()
    {
        return this.field_152366_X;
    }

    public ServerStatusResponse func_147134_at()
    {
        return this.field_147147_p;
    }

    public void func_147132_au()
    {
        this.field_147142_T = 0L;
    }

    @SideOnly(Side.SERVER)
    public String getServerHostname()
    {
        return this.hostname;
    }

    @SideOnly(Side.SERVER)
    public void setHostname(String p_71189_1_)
    {
        this.hostname = p_71189_1_;
    }

    @SideOnly(Side.SERVER)
    public void func_82010_a(IUpdatePlayerListBox p_82010_1_)
    {
        this.tickables.add(p_82010_1_);
    }

    @SideOnly(Side.SERVER)
    public static void main(String[] p_main_0_)
    {
        Bootstrap.func_151354_b();

        OptionSet options = loadOptions(p_main_0_);

        try
        {
            /* CraftBukkit start - Replace everything
            boolean flag = true;
            String s = null;
            String s1 = ".";
            String s2 = null;
            boolean flag1 = false;
            boolean flag2 = false;
            int i = -1;

            for (int j = 0; j < p_main_0_.length; ++j)
            {
                String s3 = p_main_0_[j];
                String s4 = j == p_main_0_.length - 1 ? null : p_main_0_[j + 1];
                boolean flag3 = false;

                if (!s3.equals("nogui") && !s3.equals("--nogui"))
                {
                    if (s3.equals("--port") && s4 != null)
                    {
                        flag3 = true;

                        try
                        {
                            i = Integer.parseInt(s4);
                        }
                        catch (NumberFormatException numberformatexception)
                        {
                            ;
                        }
                    }
                    else if (s3.equals("--singleplayer") && s4 != null)
                    {
                        flag3 = true;
                        s = s4;
                    }
                    else if (s3.equals("--universe") && s4 != null)
                    {
                        flag3 = true;
                        s1 = s4;
                    }
                    else if (s3.equals("--world") && s4 != null)
                    {
                        flag3 = true;
                        s2 = s4;
                    }
                    else if (s3.equals("--demo"))
                    {
                        flag1 = true;
                    }
                    else if (s3.equals("--bonusChest"))
                    {
                        flag2 = true;
                    }
                }
                else
                {
                    flag = false;
                }

                if (flag3)
                {
                    ++j;
                }
            }

            final DedicatedServer dedicatedserver = new DedicatedServer(new File(s1));

            if (s != null)
            {
                dedicatedserver.setServerOwner(s);
            }

            if (s2 != null)
            {
                dedicatedserver.setFolderName(s2);
            }

            if (i >= 0)
            {
                dedicatedserver.setServerPort(i);
            }

            if (flag1)
            {
                dedicatedserver.setDemo(true);
            }

            if (flag2)
            {
                dedicatedserver.canCreateBonusChest(true);
            }

            if (flag && !GraphicsEnvironment.isHeadless())
            {
                dedicatedserver.setGuiEnabled();
            }
            // */
            // CraftBukkit end
            if (CauldronUtils.deobfuscatedEnvironment()) useJline = false; // Cauldron
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
            // Runtime.getRuntime().addShutdownHook(new ThreadShutdown("Server Shutdown Thread", dedicatedserver));
            // CraftBukkit end
        }
        catch (Exception exception)
        {
            logger.fatal("Failed to start the minecraft server", exception);
        }
    }

    @SideOnly(Side.SERVER)
    public void logInfo(String p_71244_1_)
    {
        logger.info(p_71244_1_);
    }

    @SideOnly(Side.SERVER)
    public String getHostname()
    {
        return this.hostname;
    }

    @SideOnly(Side.SERVER)
    public int getPort()
    {
        return this.serverPort;
    }

    @SideOnly(Side.SERVER)
    public String getMotd()
    {
        return this.motd;
    }

    @SideOnly(Side.SERVER)
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

    @SideOnly(Side.SERVER)
    public String handleRConCommand(final String par1Str)
    {
        Waitable<String> waitable = new Waitable<String>()
        {
            @Override
            protected String evaluate()
            {
                RConConsoleSource.instance.resetLog();
                // Event changes start
                RemoteServerCommandEvent event = new RemoteServerCommandEvent(MinecraftServer.this.remoteConsole, par1Str);
                MinecraftServer.this.server.getPluginManager().callEvent(event);
                // Event changes end
                ServerCommand servercommand = new ServerCommand(event.getCommand(), RConConsoleSource.instance);
                MinecraftServer.this.server.dispatchServerCommand(MinecraftServer.this.remoteConsole, servercommand); // CraftBukkit
                // this.n.a(RemoteControlCommandListener.instance, s);
                return RConConsoleSource.instance.getLogContents();
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

    @SideOnly(Side.SERVER)
    public boolean isDebuggingEnabled()
    {
        return false;
    }

    @SideOnly(Side.SERVER)
    public void logSevere(String p_71201_1_)
    {
        logger.error(p_71201_1_);
    }

    @SideOnly(Side.SERVER)
    public void logDebug(String p_71198_1_)
    {
        if (this.isDebuggingEnabled())
        {
            logger.info(p_71198_1_);
        }
    }

    @SideOnly(Side.SERVER)
    public int getServerPort()
    {
        return this.serverPort;
    }

    @SideOnly(Side.SERVER)
    public void setServerPort(int p_71208_1_)
    {
        this.serverPort = p_71208_1_;
    }

    @SideOnly(Side.SERVER)
    public void func_155759_m(String p_155759_1_)
    {
        this.field_147141_M = p_155759_1_;
    }

    public boolean isServerStopped()
    {
        return this.serverStopped;
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

                acceptsAll(Arrays.asList("C", "commands-settings"), "File for command settings")
                         .withRequiredArg()
                         .ofType(File.class)
                         .defaultsTo(new File("commands.yml"))
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
            logger.log(org.apache.logging.log4j.Level.ERROR, ex.getLocalizedMessage());
        }

        if ((options == null) || (options.has("?"))) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException ex) {
                logger.log(org.apache.logging.log4j.Level.ERROR, ex);
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
                commandFile = (File)options.valueOf("commands-settings");
                configuration = YamlConfiguration.loadConfiguration(configFile);
                configuration.options().copyDefaults(true);
                configuration.setDefaults(YamlConfiguration.loadConfiguration(MinecraftServer.class.getClassLoader().getResourceAsStream("configurations/bukkit.yml")));
                ConfigurationSection legacyAlias = null;
                if (!configuration.isString("aliases")) {
                    legacyAlias = configuration.getConfigurationSection("aliases");
                    configuration.set("aliases", "now-in-commands.yml");
                }
                try {
                    configuration.save(configFile);
                    } catch (IOException ex) {
                        logger.log(org.apache.logging.log4j.Level.ERROR, "Could not save " + configFile, ex);
                }
                if (commandFile.isFile()) {
                    legacyAlias = null;
                }
                commandsConfiguration = YamlConfiguration.loadConfiguration(commandFile);
                commandsConfiguration.options().copyDefaults(true);
                commandsConfiguration.setDefaults(YamlConfiguration.loadConfiguration(MinecraftServer.class.getClassLoader().getResourceAsStream("configurations/commands.yml")));
                try {
                    commandsConfiguration.save(commandFile);
                    } catch (IOException ex) {
                        logger.log(org.apache.logging.log4j.Level.ERROR, "Could not save " + commandFile, ex);
                }

                // Migrate aliases from old file and add previously implicit $1- to pass all arguments
                if (legacyAlias != null) {
                    ConfigurationSection aliases = commandsConfiguration.createSection("aliases");
                    for (String key : legacyAlias.getKeys(false)) {
                        ArrayList<String> commands = new ArrayList<String>();

                        if (legacyAlias.isList(key)) {
                            for (String command : legacyAlias.getStringList(key)) {
                                commands.add(command + " $1-");
                            }
                        } else {
                            commands.add(legacyAlias.getString(key) + " $1-");
                        }

                        aliases.set(key, commands);
                    }
                }

                try {
                    commandsConfiguration.save(commandFile);
                    } catch (IOException ex) {
                        logger.log(org.apache.logging.log4j.Level.ERROR, "Could not save " + commandFile, ex);
                }

                return options;
                // Cauldron end
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return null; // Cauldron
    }

    @SideOnly(Side.SERVER)
    public void setForceGamemode(boolean p_104055_1_)
    {
        this.isGamemodeForced = p_104055_1_;
    }

    // CraftBukkit start
    public static Logger getLogger()
    {
        return logger;
    }
    // CraftBukkit end
}