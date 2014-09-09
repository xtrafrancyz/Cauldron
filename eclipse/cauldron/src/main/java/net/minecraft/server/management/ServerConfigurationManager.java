package net.minecraft.server.management;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.FMLOutboundHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.File;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S05PacketSpawnPosition;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.demo.DemoWorldManager;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.chunkio.ChunkIOExecutor;
import net.minecraftforge.common.network.ForgeMessage;
import net.minecraftforge.common.network.ForgeNetworkHandler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.server.network.NetHandlerLoginServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.TravelAgent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
// CraftBukkit end

public abstract class ServerConfigurationManager
{
    public static final File field_152613_a = new File("banned-players.json");
    public static final File field_152614_b = new File("banned-ips.json");
    public static final File field_152615_c = new File("ops.json");
    public static final File field_152616_d = new File("whitelist.json");
    private static final Logger logger = LogManager.getLogger();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd \'at\' HH:mm:ss z");
    private final MinecraftServer mcServer;
    public final List playerEntityList = new ArrayList();
    private final UserListBans bannedPlayers;
    private final BanList bannedIPs;
    private final UserListOps ops;
    private final UserListWhitelist whiteListedPlayers;
    private final Map field_148547_k;
    public IPlayerFileData playerNBTManagerObj; // CraftBukkit - private -> public
    public boolean whiteListEnforced; // CraftBukkit - private -> public
    protected int maxPlayers;
    private int viewDistance;
    private WorldSettings.GameType gameType;
    private boolean commandsAllowedForAll;
    private int playerPingIndex;
    private static final String __OBFID = "CL_00001423";

    // CraftBukkit start
    private CraftServer cserver;

    public ServerConfigurationManager(MinecraftServer p_i1500_1_)
    {
        p_i1500_1_.server = new CraftServer(p_i1500_1_, this);
        p_i1500_1_.console = org.bukkit.craftbukkit.command.ColouredConsoleSender.getInstance();
        p_i1500_1_.reader.addCompleter(new org.bukkit.craftbukkit.command.ConsoleCommandCompleter(p_i1500_1_.server));
        this.cserver = p_i1500_1_.server;
        // CraftBukkit end
        this.bannedPlayers = new UserListBans(field_152613_a);
        this.bannedIPs = new BanList(field_152614_b);
        this.ops = new UserListOps(field_152615_c);
        this.whiteListedPlayers = new UserListWhitelist(field_152616_d);
        this.field_148547_k = Maps.newHashMap();
        this.mcServer = p_i1500_1_;
        this.bannedPlayers.func_152686_a(false);
        this.bannedIPs.func_152686_a(false);
        this.maxPlayers = 8;
    }

    public void initializeConnectionToPlayer(NetworkManager p_72355_1_, EntityPlayerMP p_72355_2_, NetHandlerPlayServer nethandlerplayserver)
    {
        GameProfile gameprofile = p_72355_2_.getGameProfile();
        PlayerProfileCache playerprofilecache = this.mcServer.func_152358_ax();
        GameProfile gameprofile1 = playerprofilecache.func_152652_a(gameprofile.getId());
        String s = gameprofile1 == null ? gameprofile.getName() : gameprofile1.getName();
        playerprofilecache.func_152649_a(gameprofile);
        NBTTagCompound nbttagcompound = this.readPlayerDataFromFile(p_72355_2_);
        
        World playerWorld = this.mcServer.worldServerForDimension(p_72355_2_.dimension);
        if (playerWorld==null)
        {
            p_72355_2_.dimension=0;
            playerWorld=this.mcServer.worldServerForDimension(0);
            ChunkCoordinates spawnPoint = playerWorld.provider.getRandomizedSpawnPoint();
            p_72355_2_.setPosition(spawnPoint.posX, spawnPoint.posY, spawnPoint.posZ);
        }
        
        p_72355_2_.setWorld(playerWorld);
        p_72355_2_.theItemInWorldManager.setWorld((WorldServer)p_72355_2_.worldObj);
        String s1 = "local";

        if (p_72355_1_.getSocketAddress() != null)
        {
            s1 = p_72355_1_.getSocketAddress().toString();
        }

        // CraftBukkit - add world to 'logged in' message.
        logger.info(p_72355_2_.getCommandSenderName() + "[" + s1 + "] logged in with entity id " + p_72355_2_.getEntityId() + " at ([" + p_72355_2_.worldObj.worldInfo.getWorldName() + "] " + p_72355_2_.posX + ", " + p_72355_2_.posY + ", " + p_72355_2_.posZ + ")");
        WorldServer worldserver = this.mcServer.worldServerForDimension(p_72355_2_.dimension);
        ChunkCoordinates chunkcoordinates = worldserver.getSpawnPoint();
        this.func_72381_a(p_72355_2_, (EntityPlayerMP)null, worldserver);
        p_72355_2_.playerNetServerHandler = nethandlerplayserver;

        // CraftBukkit start -- Don't send a higher than 60 MaxPlayer size, otherwise the PlayerInfo window won't render correctly.
        int maxPlayers = this.getMaxPlayers();

        if (maxPlayers > 60)
        {
            maxPlayers = 60;
        }
        // CraftBukkit end

        // Cauldron start - send DimensionRegisterMessage to client before attempting to login to a Bukkit dimension
        if (DimensionManager.isBukkitDimension(p_72355_2_.dimension))
        {
            FMLEmbeddedChannel serverChannel = ForgeNetworkHandler.getServerChannel();
            serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
            serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(p_72355_2_);
            serverChannel.writeOutbound(new ForgeMessage.DimensionRegisterMessage(p_72355_2_.dimension, worldserver.getWorld().getEnvironment().getId()));
        }
        // Cauldron end

        nethandlerplayserver.sendPacket(new S01PacketJoinGame(p_72355_2_.getEntityId(), p_72355_2_.theItemInWorldManager.getGameType(), worldserver.getWorldInfo().isHardcoreModeEnabled(), worldserver.provider.dimensionId, worldserver.difficultySetting, this.getMaxPlayers(), worldserver.getWorldInfo().getTerrainType()));
        p_72355_2_.getBukkitEntity().sendSupportedChannels(); // CraftBukkit
        nethandlerplayserver.sendPacket(new S3FPacketCustomPayload("MC|Brand", this.getServerInstance().getServerModName().getBytes(Charsets.UTF_8)));
        nethandlerplayserver.sendPacket(new S05PacketSpawnPosition(chunkcoordinates.posX, chunkcoordinates.posY, chunkcoordinates.posZ));
        nethandlerplayserver.sendPacket(new S39PacketPlayerAbilities(p_72355_2_.capabilities));
        nethandlerplayserver.sendPacket(new S09PacketHeldItemChange(p_72355_2_.inventory.currentItem));
        p_72355_2_.func_147099_x().func_150877_d();
        p_72355_2_.func_147099_x().func_150884_b(p_72355_2_);
        this.func_96456_a((ServerScoreboard)worldserver.getScoreboard(), p_72355_2_);
        this.mcServer.func_147132_au();
        /* CraftBukkit start - login message is handled in the event
        ChatComponentTranslation chatcomponenttranslation;

        if (!p_72355_2_.getCommandSenderName().equalsIgnoreCase(s))
        {
            chatcomponenttranslation = new ChatComponentTranslation("multiplayer.player.joined.renamed", new Object[] {p_72355_2_.func_145748_c_(), s});
        }
        else
        {
            chatcomponenttranslation = new ChatComponentTranslation("multiplayer.player.joined", new Object[] {p_72355_2_.func_145748_c_()});
        }

        chatcomponenttranslation.getChatStyle().setColor(EnumChatFormatting.YELLOW);
        this.sendChatMsg(chatcomponenttranslation);
        // CraftBukkit end*/
        this.playerLoggedIn(p_72355_2_);
        nethandlerplayserver.setPlayerLocation(p_72355_2_.posX, p_72355_2_.posY, p_72355_2_.posZ, p_72355_2_.rotationYaw, p_72355_2_.rotationPitch);
        this.updateTimeAndWeatherForPlayer(p_72355_2_, worldserver);

        if (this.mcServer.getTexturePack().length() > 0)
        {
            p_72355_2_.requestTexturePackLoad(this.mcServer.getTexturePack());
        }

        Iterator iterator = p_72355_2_.getActivePotionEffects().iterator();

        while (iterator.hasNext())
        {
            PotionEffect potioneffect = (PotionEffect)iterator.next();
            nethandlerplayserver.sendPacket(new S1DPacketEntityEffect(p_72355_2_.getEntityId(), potioneffect));
        }

        p_72355_2_.addSelfToInternalCraftingInventory();

        FMLCommonHandler.instance().firePlayerLoggedIn(p_72355_2_);
        if (nbttagcompound != null && nbttagcompound.hasKey("Riding", 10))
        {
            Entity entity = EntityList.createEntityFromNBT(nbttagcompound.getCompoundTag("Riding"), worldserver);

            if (entity != null)
            {
                entity.forceSpawn = true;
                worldserver.spawnEntityInWorld(entity);
                p_72355_2_.mountEntity(entity);
                entity.forceSpawn = false;
            }
        }
    }

    public void func_96456_a(ServerScoreboard p_96456_1_, EntityPlayerMP p_96456_2_) // CraftBukkit - protected -> public
    {
        HashSet hashset = new HashSet();
        Iterator iterator = p_96456_1_.getTeams().iterator();

        while (iterator.hasNext())
        {
            ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam)iterator.next();
            p_96456_2_.playerNetServerHandler.sendPacket(new S3EPacketTeams(scoreplayerteam, 0));
        }

        for (int i = 0; i < 3; ++i)
        {
            ScoreObjective scoreobjective = p_96456_1_.func_96539_a(i);

            if (scoreobjective != null && !hashset.contains(scoreobjective))
            {
                List list = p_96456_1_.func_96550_d(scoreobjective);
                Iterator iterator1 = list.iterator();

                while (iterator1.hasNext())
                {
                    Packet packet = (Packet)iterator1.next();
                    p_96456_2_.playerNetServerHandler.sendPacket(packet);
                }

                hashset.add(scoreobjective);
            }
        }
    }

    public void setPlayerManager(WorldServer[] p_72364_1_)
    {
        if (this.playerNBTManagerObj != null)
        {
            return;    // CraftBukkit
        }

        this.playerNBTManagerObj = p_72364_1_[0].getSaveHandler().getSaveHandler();
    }

    public void func_72375_a(EntityPlayerMP p_72375_1_, WorldServer p_72375_2_)
    {
        WorldServer worldserver1 = p_72375_1_.getServerForPlayer();

        if (p_72375_2_ != null)
        {
            p_72375_2_.getPlayerManager().removePlayer(p_72375_1_);
        }

        worldserver1.getPlayerManager().addPlayer(p_72375_1_);
        worldserver1.theChunkProviderServer.loadChunk((int)p_72375_1_.posX >> 4, (int)p_72375_1_.posZ >> 4);
    }

    public int getEntityViewDistance()
    {
        return PlayerManager.getFurthestViewableBlock(this.getViewDistance());
    }

    public NBTTagCompound readPlayerDataFromFile(EntityPlayerMP p_72380_1_)
    {
        NBTTagCompound nbttagcompound = this.mcServer.worlds.get(0).getWorldInfo().getPlayerNBTTagCompound();
        NBTTagCompound nbttagcompound1;

        if (p_72380_1_.getCommandSenderName().equals(this.mcServer.getServerOwner()) && nbttagcompound != null)
        {
            p_72380_1_.readFromNBT(nbttagcompound);
            nbttagcompound1 = nbttagcompound;
            logger.debug("loading single player");
            net.minecraftforge.event.ForgeEventFactory.firePlayerLoadingEvent(p_72380_1_, this.playerNBTManagerObj, p_72380_1_.getUniqueID().toString());
        }
        else
        {
            nbttagcompound1 = this.playerNBTManagerObj.readPlayerData(p_72380_1_);
        }

        return nbttagcompound1;
    }

    protected void writePlayerData(EntityPlayerMP p_72391_1_)
    {
        if (p_72391_1_.playerNetServerHandler == null) return;

        this.playerNBTManagerObj.writePlayerData(p_72391_1_);
        StatisticsFile statisticsfile = (StatisticsFile)this.field_148547_k.get(p_72391_1_.getUniqueID());

        if (statisticsfile != null)
        {
            statisticsfile.func_150883_b();
        }
    }

    public void playerLoggedIn(EntityPlayerMP p_72377_1_)
    {
        cserver.detectListNameConflict(p_72377_1_); // CraftBukkit
        // this.sendPacketToAllPlayers(new S38PacketPlayerListItem(p_72377_1_.getCommandSenderName(), true, 1000)); // CraftBukkit - replaced with loop below
        this.playerEntityList.add(p_72377_1_);
        WorldServer worldserver = this.mcServer.worldServerForDimension(p_72377_1_.dimension);
        // CraftBukkit start
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(this.cserver.getPlayer(p_72377_1_), "\u00A7e" + p_72377_1_.getCommandSenderName() + " joined the game.");
        this.cserver.getPluginManager().callEvent(playerJoinEvent);
        String joinMessage = playerJoinEvent.getJoinMessage();

        if ((joinMessage != null) && (joinMessage.length() > 0))
        {
            for (IChatComponent line : org.bukkit.craftbukkit.util.CraftChatMessage.fromString(joinMessage))
            {
                this.mcServer.getConfigurationManager().sendPacketToAllPlayers(new S02PacketChat(line));
            }
        }

        this.cserver.onPlayerJoin(playerJoinEvent.getPlayer());
        ChunkIOExecutor.adjustPoolSize(this.getCurrentPlayerCount());
        // CraftBukkit end

        // CraftBukkit start - Only add if the player wasn't moved in the event
        if (p_72377_1_.worldObj == worldserver && !worldserver.playerEntities.contains(p_72377_1_))
        {
            worldserver.spawnEntityInWorld(p_72377_1_);
            this.func_72375_a(p_72377_1_, (WorldServer) null);
        }

        // CraftBukkit end
        // CraftBukkit start - sendAll above replaced with this loop
        S38PacketPlayerListItem packet = new S38PacketPlayerListItem(p_72377_1_.listName, true, 1000);

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            EntityPlayerMP entityplayermp1 = (EntityPlayerMP) this.playerEntityList.get(i);

            if (entityplayermp1.getBukkitEntity().canSee(p_72377_1_.getBukkitEntity()))
            {
                entityplayermp1.playerNetServerHandler.sendPacket(packet);
            }
        }

        // CraftBukkit end

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            EntityPlayerMP entityplayermp1 = (EntityPlayerMP) this.playerEntityList.get(i);

            // CraftBukkit start
            if (!p_72377_1_.getBukkitEntity().canSee(entityplayermp1.getBukkitEntity()))
            {
                continue;
            }

            // .name -> .listName
            p_72377_1_.playerNetServerHandler.sendPacket(new S38PacketPlayerListItem(entityplayermp1.listName, true, entityplayermp1.ping));
            // CraftBukkit end
        }
    }

    public void updatePlayerPertinentChunks(EntityPlayerMP p_72358_1_)
    {
        p_72358_1_.getServerForPlayer().getPlayerManager().updatePlayerPertinentChunks(p_72358_1_);
    }

    // Cauldron start - vanilla compatibility
    public void playerLoggedOut(EntityPlayerMP p_72367_1_)
    {
        disconnect(p_72367_1_);
    }
    // Cauldron end

    public String disconnect(EntityPlayerMP entityplayermp)   // CraftBukkit - return string
    {
        entityplayermp.triggerAchievement(StatList.leaveGameStat);
        // Cauldron start - don't show quit messages for players that haven't actually connected
        PlayerQuitEvent playerQuitEvent = null;
        if (entityplayermp.playerNetServerHandler != null)
        {
            // CraftBukkit start - Quitting must be before we do final save of data, in case plugins need to modify it
            org.bukkit.craftbukkit.event.CraftEventFactory.handleInventoryCloseEvent(entityplayermp);
            playerQuitEvent = new PlayerQuitEvent(this.cserver.getPlayer(entityplayermp), "\u00A7e" + entityplayermp.getCommandSenderName() + " left the game.");
            this.cserver.getPluginManager().callEvent(playerQuitEvent);
            entityplayermp.getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());
            // CraftBukkit end
        }
        // Cauldron end
        FMLCommonHandler.instance().firePlayerLoggedOut(entityplayermp);
        this.writePlayerData(entityplayermp);
        WorldServer worldserver = entityplayermp.getServerForPlayer();

        if (entityplayermp.ridingEntity != null && !(entityplayermp.ridingEntity instanceof EntityPlayerMP))   // CraftBukkit - Don't remove players
        {
            worldserver.removePlayerEntityDangerously(entityplayermp.ridingEntity);
            logger.debug("removing player mount");
        }

        worldserver.removeEntity(entityplayermp);
        worldserver.getPlayerManager().removePlayer(entityplayermp);
        this.playerEntityList.remove(entityplayermp);
        this.field_148547_k.remove(entityplayermp.getCommandSenderName());
        ChunkIOExecutor.adjustPoolSize(this.getCurrentPlayerCount()); // CraftBukkit
        // CraftBukkit start - .name -> .listName, replace sendAll with loop
        // this.sendAll(new PacketPlayOutPlayerInfo(entityplayermp.getName(), false, 9999));
        S38PacketPlayerListItem packet = new S38PacketPlayerListItem(entityplayermp.listName, false, 9999);

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            EntityPlayerMP entityplayermp1 = (EntityPlayerMP) this.playerEntityList.get(i);

            if (entityplayermp1.getBukkitEntity().canSee(entityplayermp.getBukkitEntity()))
            {
                entityplayermp1.playerNetServerHandler.sendPacket(packet);
            }
        }

        // This removes the scoreboard (and player reference) for the specific player in the manager
        this.cserver.getScoreboardManager().removePlayer(entityplayermp.getBukkitEntity());
        // Cauldron start
        if (playerQuitEvent != null)
        {
            return playerQuitEvent.getQuitMessage();
        }
        else
        {
            return null;
        }
        // Cauldron end
        // CraftBukkit end
    }

    public String allowUserToConnect(SocketAddress p_148542_1_, GameProfile p_148542_2_)
    {
        String s;

        if (this.bannedPlayers.func_152702_a(p_148542_2_))
        {
            UserListBansEntry userlistbansentry = (UserListBansEntry)this.bannedPlayers.func_152683_b(p_148542_2_);
            s = "You are banned from this server!\nReason: " + userlistbansentry.getBanReason();

            if (userlistbansentry.getBanEndDate() != null)
            {
                s = s + "\nYour ban will be removed on " + dateFormat.format(userlistbansentry.getBanEndDate());
            }

            return s;
        }
        else if (!this.func_152607_e(p_148542_2_))
        {
            return "You are not white-listed on this server!";
        }
        else if (this.bannedIPs.func_152708_a(p_148542_1_))
        {
            IPBanEntry ipbanentry = this.bannedIPs.func_152709_b(p_148542_1_);
            s = "Your IP address is banned from this server!\nReason: " + ipbanentry.getBanReason();

            if (ipbanentry.getBanEndDate() != null)
            {
                s = s + "\nYour ban will be removed on " + dateFormat.format(ipbanentry.getBanEndDate());
            }

            return s;
        }
        else
        {
            return this.playerEntityList.size() >= this.maxPlayers ? "The server is full!" : null;
        }
    }

    // CraftBukkit start - Whole method, SocketAddress to LoginListener, added hostname to signature, return EntityPlayer
    public EntityPlayerMP attemptLogin(NetHandlerLoginServer loginlistener, GameProfile gameprofile, String hostname)
    {
        // Instead of kicking then returning, we need to store the kick reason
        // in the event, check with plugins to see if it's ok, and THEN kick
        // depending on the outcome.
        SocketAddress socketaddress = loginlistener.field_147333_a.getSocketAddress();
        EntityPlayerMP entity = new EntityPlayerMP(this.mcServer, this.mcServer.worldServerForDimension(0), gameprofile, new ItemInWorldManager(this.mcServer.worldServerForDimension(0)));
        Player player = entity.getBukkitEntity();
        PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, ((java.net.InetSocketAddress) socketaddress).getAddress(), ((java.net.InetSocketAddress) loginlistener.field_147333_a.getRawAddress()).getAddress()); // Spigot
        String s;

        if (this.bannedPlayers.func_152702_a(gameprofile) && !this.bannedPlayers.func_152683_b(gameprofile).hasBanExpired())
        {
            UserListBansEntry banentry = (UserListBansEntry) this.bannedPlayers.func_152683_b(gameprofile);
            s = "You are banned from this server!\nReason: " + banentry.getBanReason();

            if (banentry.getBanEndDate() != null)
            {
                s = s + "\nYour ban will be removed on " + dateFormat.format(banentry.getBanEndDate());
            }

            // return s;
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, s);
        }
        else if (!this.func_152607_e(gameprofile))
        {
            // return "You are not white-listed on this server!";
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, org.spigotmc.SpigotConfig.whitelistMessage); // Spigot
        }
        else if (this.bannedIPs.func_152708_a(socketaddress) && !this.bannedPlayers.func_152683_b(gameprofile).hasBanExpired())
        {
            IPBanEntry ipbanentry = this.bannedIPs.func_152709_b(socketaddress);
            s = "Your IP address is banned from this server!\nReason: " + ipbanentry.getBanReason();

            if (ipbanentry.getBanEndDate() != null)
            {
                s = s + "\nYour ban will be removed on " + dateFormat.format(ipbanentry.getBanEndDate());
            }
            // return s;
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, s);
        }
        else
        {
            // return this.players.size() >= this.maxPlayers ? "The server is full!" : null;
            if (this.playerEntityList.size() >= this.maxPlayers)
            {
                event.disallow(PlayerLoginEvent.Result.KICK_FULL, org.spigotmc.SpigotConfig.serverFullMessage); // Spigot
            }
        }

        this.cserver.getPluginManager().callEvent(event);

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED)
        {
            loginlistener.func_147322_a(event.getKickMessage());
            return null;
        }

        return entity;
        // CraftBukkit end
    }

    public EntityPlayerMP createPlayerForUser(GameProfile p_148545_1_)
    {
        UUID uuid = EntityPlayer.func_146094_a(p_148545_1_);
        ArrayList arraylist = Lists.newArrayList();
        EntityPlayerMP entityplayermp;

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            entityplayermp = (EntityPlayerMP)this.playerEntityList.get(i);

            if (entityplayermp.getUniqueID().equals(uuid))
            {
                arraylist.add(entityplayermp);
            }
        }

        Iterator iterator = arraylist.iterator();

        while (iterator.hasNext())
        {
            entityplayermp = (EntityPlayerMP)iterator.next();
            entityplayermp.playerNetServerHandler.kickPlayerFromServer("You logged in from another location");
        }

        Object object;

        if (this.mcServer.isDemo())
        {
            object = new DemoWorldManager(this.mcServer.worldServerForDimension(0));
        }
        else
        {
            object = new ItemInWorldManager(this.mcServer.worldServerForDimension(0));
        }

        return new EntityPlayerMP(this.mcServer, this.mcServer.worldServerForDimension(0), p_148545_1_, (ItemInWorldManager)object);
    }

    public EntityPlayerMP processLogin(GameProfile gameprofile, EntityPlayerMP player)   // CraftBukkit - added EntityPlayer
    {
        ArrayList arraylist = new ArrayList();
        EntityPlayerMP entityplayermp;

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            entityplayermp = (EntityPlayerMP) this.playerEntityList.get(i);

            if (entityplayermp.getCommandSenderName().equalsIgnoreCase(gameprofile.getName()))
            {
                arraylist.add(entityplayermp);
            }
        }

        Iterator iterator = arraylist.iterator();

        while (iterator.hasNext())
        {
            entityplayermp = (EntityPlayerMP) iterator.next();
            entityplayermp.playerNetServerHandler.kickPlayerFromServer("You logged in from another location");
        }

        /* CraftBukkit start
        Object object;

        if (this.mcServer.isDemo())
        {
            object = new DemoWorldManager(this.mcServer.worldServerForDimension(0));
        }
        else
        {
            object = new ItemInWorldManager(this.mcServer.worldServerForDimension(0));
        }

        return new EntityPlayerMP(this.mcServer, this.mcServer.worldServerForDimension(0), p_148545_1_, (ItemInWorldManager)object);
        // */
        return player;
        // CraftBukkit end
    }

    // Cauldron start - refactor entire method for sanity.
    public EntityPlayerMP respawnPlayer(EntityPlayerMP par1EntityPlayerMP, int par2, boolean par3)
    {
        return this.respawnPlayer(par1EntityPlayerMP, par2, par3, null);
    }

    public EntityPlayerMP respawnPlayer(EntityPlayerMP par1EntityPlayerMP, int targetDimension, boolean returnFromEnd, Location location)
    {
        // Phase 1 - check if the player is allowed to respawn in same dimension
        World world = mcServer.worldServerForDimension(targetDimension);

        if (world == null)
        {
            targetDimension = 0;
        }
        else if (location == null && !world.provider.canRespawnHere()) // ignore plugins
        {
            targetDimension = world.provider.getRespawnDimension(par1EntityPlayerMP);
        }

        // Phase 2 - handle return from End
        if (returnFromEnd)
        {
            WorldServer exitWorld = this.mcServer.worldServerForDimension(targetDimension);
            Location enter = par1EntityPlayerMP.getBukkitEntity().getLocation();
            Location exit = null;
            // THE_END -> NORMAL; use bed if available, otherwise default spawn
            exit = ((org.bukkit.craftbukkit.entity.CraftPlayer) par1EntityPlayerMP.getBukkitEntity()).getBedSpawnLocation();

            if (exit == null || ((CraftWorld) exit.getWorld()).getHandle().dimension != 0)
            {
                exit = exitWorld.getWorld().getSpawnLocation();
            }
            PlayerPortalEvent event = new PlayerPortalEvent(par1EntityPlayerMP.getBukkitEntity(), enter, exit, org.bukkit.craftbukkit.CraftTravelAgent.DEFAULT, TeleportCause.END_PORTAL);
            event.useTravelAgent(false);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled() || event.getTo() == null)
            {
                return null;
            }
        }

        // Phase 3 - remove current player from current dimension
        par1EntityPlayerMP.getServerForPlayer().getEntityTracker().removePlayerFromTrackers(par1EntityPlayerMP);
        // par1EntityPlayerMP.getServerForPlayer().getEntityTracker().removeEntityFromAllTrackingPlayers(par1EntityPlayerMP); // CraftBukkit
        par1EntityPlayerMP.getServerForPlayer().getPlayerManager().removePlayer(par1EntityPlayerMP);
        this.playerEntityList.remove(par1EntityPlayerMP);
        this.mcServer.worldServerForDimension(par1EntityPlayerMP.dimension).removePlayerEntityDangerously(par1EntityPlayerMP);

        // Phase 4 - handle bed spawn
        ChunkCoordinates bedSpawnChunkCoords = par1EntityPlayerMP.getBedLocation(targetDimension);
        boolean spawnForced = par1EntityPlayerMP.isSpawnForced(targetDimension);
        par1EntityPlayerMP.dimension = targetDimension;
        // CraftBukkit start
        EntityPlayerMP entityplayermp1 = par1EntityPlayerMP;
        entityplayermp1.setWorld(this.mcServer.worldServerForDimension(par1EntityPlayerMP.dimension)); // make sure to update reference for bed spawn logic
        org.bukkit.World fromWorld = entityplayermp1.getBukkitEntity().getWorld();
        entityplayermp1.playerConqueredTheEnd = false;
        ChunkCoordinates chunkcoordinates1;
        boolean isBedSpawn = false;
        org.bukkit.World toWorld = entityplayermp1.getBukkitEntity().getWorld();

        if (location == null) // use bed logic only if player respawns (player death)
        {
            if (bedSpawnChunkCoords != null) // if player has a bed
            {
                chunkcoordinates1 = EntityPlayer.verifyRespawnCoordinates(this.mcServer.worldServerForDimension(par1EntityPlayerMP.dimension), bedSpawnChunkCoords, spawnForced);
    
                if (chunkcoordinates1 != null)
                {
                    isBedSpawn = true;
                    entityplayermp1.setLocationAndAngles((double)((float)chunkcoordinates1.posX + 0.5F), (double)((float)chunkcoordinates1.posY + 0.1F), (double)((float)chunkcoordinates1.posZ + 0.5F), 0.0F, 0.0F);
                    entityplayermp1.setSpawnChunk(bedSpawnChunkCoords, spawnForced);
                    location = new Location(toWorld, bedSpawnChunkCoords.posX + 0.5, bedSpawnChunkCoords.posY, bedSpawnChunkCoords.posZ + 0.5);
                }
                else // bed was not found (broken)
                {
                    //entityplayermp1.setSpawnChunk(null, true); // CraftBukkit
                    entityplayermp1.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(0, 0));
                    location = new Location(toWorld, toWorld.getSpawnLocation().getX(), toWorld.getSpawnLocation().getY(), toWorld.getSpawnLocation().getZ()); // use the spawnpoint as location
                }
            }
    
            if (location == null)
            {
                location = new Location(toWorld, toWorld.getSpawnLocation().getX(), toWorld.getSpawnLocation().getY(), toWorld.getSpawnLocation().getZ()); // use the world spawnpoint as default location
            }
    
            Player respawnPlayer = this.cserver.getPlayer(entityplayermp1);
            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn);
            this.cserver.getPluginManager().callEvent(respawnEvent);
    
            if (!spawnForced) // mods override plugins
            {
                location = respawnEvent.getRespawnLocation();
            }
    
            par1EntityPlayerMP.reset();
        }
        else // plugin
        {
            location.setWorld(this.mcServer.worldServerForDimension(targetDimension).getWorld());
        }

        WorldServer targetWorld = ((CraftWorld) location.getWorld()).getHandle();
        entityplayermp1.setPositionAndRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        // CraftBukkit end
        targetWorld.theChunkProviderServer.loadChunk((int)entityplayermp1.posX >> 4, (int)entityplayermp1.posZ >> 4);

        while (!targetWorld.getCollidingBoundingBoxes(entityplayermp1, entityplayermp1.boundingBox).isEmpty())
        {
            entityplayermp1.setPosition(entityplayermp1.posX, entityplayermp1.posY + 1.0D, entityplayermp1.posZ);
        }

        // Phase 5 - Respawn player in new world
        int actualDimension = targetWorld.provider.dimensionId;
        // Cauldron - change dim for bukkit added dimensions
        if (DimensionManager.isBukkitDimension(actualDimension))
        {
            FMLEmbeddedChannel serverChannel = ForgeNetworkHandler.getServerChannel();
            serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set(FMLOutboundHandler.OutboundTarget.PLAYER);
            serverChannel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(entityplayermp1);
            serverChannel.writeOutbound(new ForgeMessage.DimensionRegisterMessage(actualDimension, targetWorld.getWorld().getEnvironment().getId()));
        }
        // Cauldron end
        // CraftBukkit start
        entityplayermp1.playerNetServerHandler.sendPacket(new S07PacketRespawn(actualDimension, targetWorld.difficultySetting, targetWorld.getWorldInfo().getTerrainType(), entityplayermp1.theItemInWorldManager.getGameType()));
        entityplayermp1.setWorld(targetWorld); // in case plugin changed it
        entityplayermp1.isDead = false;
        entityplayermp1.playerNetServerHandler.teleport(new Location(targetWorld.getWorld(), entityplayermp1.posX, entityplayermp1.posY, entityplayermp1.posZ, entityplayermp1.rotationYaw, entityplayermp1.rotationPitch));
        entityplayermp1.setSneaking(false);
        chunkcoordinates1 = targetWorld.getSpawnPoint();
        // CraftBukkit end
        entityplayermp1.playerNetServerHandler.sendPacket(new S05PacketSpawnPosition(chunkcoordinates1.posX, chunkcoordinates1.posY, chunkcoordinates1.posZ));
        entityplayermp1.playerNetServerHandler.sendPacket(new S1FPacketSetExperience(entityplayermp1.experience, entityplayermp1.experienceTotal, entityplayermp1.experienceLevel));
        this.updateTimeAndWeatherForPlayer(entityplayermp1, targetWorld);
        targetWorld.getPlayerManager().addPlayer(entityplayermp1);
        targetWorld.spawnEntityInWorld(entityplayermp1);
        this.playerEntityList.add(entityplayermp1);
        entityplayermp1.addSelfToInternalCraftingInventory();
        entityplayermp1.setHealth(entityplayermp1.getHealth());

        // If world changed then fire the appropriate change world event else respawn
        if (fromWorld != location.getWorld())
        {
            FMLCommonHandler.instance().firePlayerChangedDimensionEvent(entityplayermp1, ((CraftWorld)fromWorld).getHandle().provider.dimensionId, ((CraftWorld)location.getWorld()).getHandle().provider.dimensionId); // Cauldron - fire forge changed dimension event
        }
        else FMLCommonHandler.instance().firePlayerRespawnEvent(entityplayermp1);

        return entityplayermp1;
    }

    // Cauldron start - refactor transferPlayerToDimension to be compatible with Bukkit. These methods are to be used when a player comes in contact with a portal
    public void transferPlayerToDimension(EntityPlayerMP p_72356_1_, int p_72356_2_) // wrapper for vanilla compatibility
    {
        transferPlayerToDimension(p_72356_1_, p_72356_2_, mcServer.worldServerForDimension(p_72356_2_).getDefaultTeleporter());
    }

    public void transferPlayerToDimension(EntityPlayerMP p_72356_1_, int p_72356_2_, Teleporter teleporter) // mods such as Twilight Forest call this method directly
    {
        this.transferPlayerToDimension(p_72356_1_, p_72356_2_, teleporter, TeleportCause.MOD); // use our mod cause
    }

    public void transferPlayerToDimension(EntityPlayerMP par1EntityPlayerMP, int par2, TeleportCause cause)
    {
        this.transferPlayerToDimension(par1EntityPlayerMP, par2, mcServer.worldServerForDimension(par2).getDefaultTeleporter(), cause);
    }

    public void transferPlayerToDimension(EntityPlayerMP par1EntityPlayerMP, int targetDimension, Teleporter teleporter, TeleportCause cause) // Cauldron - add TeleportCause
    {
        // Allow Forge hotloading on teleport
        WorldServer fromWorld = this.mcServer.worldServerForDimension(par1EntityPlayerMP.dimension);
        WorldServer exitWorld = this.mcServer.worldServerForDimension(targetDimension);

        // CraftBukkit start - Replaced the standard handling of portals with a more customised method.
        Location enter = par1EntityPlayerMP.getBukkitEntity().getLocation();
        Location exit = null;
        boolean useTravelAgent = false;

        if (exitWorld != null)
        {
            exit = this.calculateTarget(enter, exitWorld);
            if (cause != cause.MOD) // don't use travel agent for custom dimensions
            {
                useTravelAgent = true;
            }
        }

        // allow forge mods to be the teleporter
        TravelAgent agent = null;
        if (exit != null && teleporter == null) 
        {
            teleporter = ((CraftWorld)exit.getWorld()).getHandle().getDefaultTeleporter();
            if (teleporter instanceof TravelAgent) 
            {
                agent = (TravelAgent)teleporter;
            }
        }
        else
        {
            if (teleporter instanceof TravelAgent) 
            {
                agent = (TravelAgent)teleporter;
            }
        }
        if (agent == null) // mod teleporter such as Twilight Forest
        {
            agent = org.bukkit.craftbukkit.CraftTravelAgent.DEFAULT; // return arbitrary TA to compensate for implementation dependent plugins
        }

        PlayerPortalEvent event = new PlayerPortalEvent(par1EntityPlayerMP.getBukkitEntity(), enter, exit, agent, cause);
        event.useTravelAgent(useTravelAgent);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled() || event.getTo() == null)
        {
            return;
        }

        exit = event.useTravelAgent() && cause != cause.MOD ? event.getPortalTravelAgent().findOrCreate(event.getTo()) : event.getTo(); // make sure plugins don't override travelagent for mods

        if (exit == null)
        {
            return;
        }

        exitWorld = ((CraftWorld) exit.getWorld()).getHandle();
        Vector velocity = par1EntityPlayerMP.getBukkitEntity().getVelocity();
        boolean before = exitWorld.theChunkProviderServer.loadChunkOnProvideRequest;
        exitWorld.theChunkProviderServer.loadChunkOnProvideRequest = true;
        exitWorld.getDefaultTeleporter().adjustExit(par1EntityPlayerMP, exit, velocity);
        exitWorld.theChunkProviderServer.loadChunkOnProvideRequest = before;
        // CraftBukkit end

        par1EntityPlayerMP.dimension = targetDimension;
        par1EntityPlayerMP.playerNetServerHandler.sendPacket(new S07PacketRespawn(par1EntityPlayerMP.dimension, par1EntityPlayerMP.worldObj.difficultySetting, par1EntityPlayerMP.worldObj.getWorldInfo().getTerrainType(), par1EntityPlayerMP.theItemInWorldManager.getGameType()));
        fromWorld.removePlayerEntityDangerously(par1EntityPlayerMP);
        par1EntityPlayerMP.isDead = false;
        this.transferEntityToWorld(par1EntityPlayerMP, fromWorld.provider.dimensionId, fromWorld, exitWorld, teleporter);
        this.func_72375_a(par1EntityPlayerMP, fromWorld);
        par1EntityPlayerMP.playerNetServerHandler.setPlayerLocation(par1EntityPlayerMP.posX, par1EntityPlayerMP.posY, par1EntityPlayerMP.posZ, par1EntityPlayerMP.rotationYaw, par1EntityPlayerMP.rotationPitch);
        par1EntityPlayerMP.theItemInWorldManager.setWorld(exitWorld);
        this.updateTimeAndWeatherForPlayer(par1EntityPlayerMP, exitWorld);
        this.syncPlayerInventory(par1EntityPlayerMP);
        Iterator iterator = par1EntityPlayerMP.getActivePotionEffects().iterator();

        while (iterator.hasNext())
        {
            PotionEffect potioneffect = (PotionEffect)iterator.next();
            par1EntityPlayerMP.playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(par1EntityPlayerMP.getEntityId(), potioneffect));
        }

        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(par1EntityPlayerMP, fromWorld.dimension, targetDimension);
    }

    public void transferEntityToWorld(Entity p_82448_1_, int p_82448_2_, WorldServer p_82448_3_, WorldServer p_82448_4_)
    {
        // CraftBukkit start - Split into modular functions
        //transferEntityToWorld(p_82448_1_, p_82448_2_, p_82448_3_, p_82448_4_, p_82448_4_.getDefaultTeleporter());
        Location exit = this.calculateTarget(p_82448_1_.getBukkitEntity().getLocation(), p_82448_4_);
        this.repositionEntity(p_82448_1_, exit, true);
    }

    public void transferEntityToWorld(Entity p_82448_1_, int p_82448_2_, WorldServer p_82448_3_, WorldServer p_82448_4_, Teleporter teleporter)
    {
        WorldProvider pOld = p_82448_3_.provider;
        WorldProvider pNew = p_82448_4_.provider;
        double moveFactor = pOld.getMovementFactor() / pNew.getMovementFactor();
        double d0 = p_82448_1_.posX * moveFactor;
        double d1 = p_82448_1_.posZ * moveFactor;
        double d3 = p_82448_1_.posX;
        double d4 = p_82448_1_.posY;
        double d5 = p_82448_1_.posZ;
        float f = p_82448_1_.rotationYaw;
        p_82448_3_.theProfiler.startSection("moving");

        /*
        if (par1Entity.dimension == -1)
        {
            d0 /= d2;
            d1 /= d2;
            par1Entity.setLocationAndAngles(d0, par1Entity.posY, d1, par1Entity.rotationYaw, par1Entity.rotationPitch);

            if (par1Entity.isEntityAlive())
            {
                par3WorldServer.updateEntityWithOptionalForce(par1Entity, false);
            }
        }
        else if (par1Entity.dimension == 0)
        {
            d0 *= d2;
            d1 *= d2;
            par1Entity.setLocationAndAngles(d0, par1Entity.posY, d1, par1Entity.rotationYaw, par1Entity.rotationPitch);

            if (par1Entity.isEntityAlive())
            {
                par3WorldServer.updateEntityWithOptionalForce(par1Entity, false);
            }
        }
        */
        if (p_82448_1_.dimension == 1)
        {
            ChunkCoordinates chunkcoordinates;

            if (p_82448_2_ == 1)
            {
                chunkcoordinates = p_82448_4_.getSpawnPoint();
            }
            else
            {
                chunkcoordinates = p_82448_4_.getEntrancePortalLocation();
            }

            d0 = (double)chunkcoordinates.posX;
            p_82448_1_.posY = (double)chunkcoordinates.posY;
            d1 = (double)chunkcoordinates.posZ;
            p_82448_1_.setLocationAndAngles(d0, p_82448_1_.posY, d1, 90.0F, 0.0F);

            if (p_82448_1_.isEntityAlive())
            {
                p_82448_3_.updateEntityWithOptionalForce(p_82448_1_, false);
            }
        }

        p_82448_3_.theProfiler.endSection();

        if (p_82448_2_ != 1)
        {
            p_82448_3_.theProfiler.startSection("placing");
            d0 = (double)MathHelper.clamp_int((int)d0, -29999872, 29999872);
            d1 = (double)MathHelper.clamp_int((int)d1, -29999872, 29999872);

            if (p_82448_1_.isEntityAlive())
            {
                p_82448_1_.setLocationAndAngles(d0, p_82448_1_.posY, d1, p_82448_1_.rotationYaw, p_82448_1_.rotationPitch);
                teleporter.placeInPortal(p_82448_1_, d3, d4, d5, f);
                p_82448_4_.spawnEntityInWorld(p_82448_1_);
                p_82448_4_.updateEntityWithOptionalForce(p_82448_1_, false);
            }

            p_82448_3_.theProfiler.endSection();
        }

        p_82448_1_.setWorld(p_82448_4_);
    }

    // Copy of original a(Entity, int, WorldServer, WorldServer) method with only location calculation logic
    public Location calculateTarget(Location enter, World target)
    {
        WorldServer worldserver = ((CraftWorld) enter.getWorld()).getHandle();
        WorldServer worldserver1 = ((CraftWorld) target.getWorld()).getHandle();
        int i = worldserver.dimension;
        double y = enter.getY();
        float yaw = enter.getYaw();
        float pitch = enter.getPitch();
        double d0 = enter.getX();
        double d1 = enter.getZ();
        double d2 = 8.0D;

        /*
        double d3 = entity.locX;
        double d4 = entity.locY;
        double d5 = entity.locZ;
        float f = entity.yaw;

        worldserver.methodProfiler.a("moving");
        */
        if (worldserver1.dimension == -1)
        {
            d0 /= d2;
            d1 /= d2;
            /*
            entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
            if (entity.isAlive()) {
                worldserver.entityJoinedWorld(entity, false);
            }
            */
        }
        else if (worldserver1.dimension == 0)
        {
            d0 *= d2;
            d1 *= d2;
            /*
            entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
            if (entity.isAlive()) {
                worldserver.entityJoinedWorld(entity, false);
            }
            */
        }
        else
        {
            ChunkCoordinates chunkcoordinates;

            if (i == 1)
            {
                // use default NORMAL world spawn instead of target
                worldserver1 = this.mcServer.worlds.get(0);
                chunkcoordinates = worldserver1.getSpawnPoint();
            }
            else
            {
                chunkcoordinates = worldserver1.getEntrancePortalLocation();
            }

            // Cauldron start - validate chunkcoordinates
            if (chunkcoordinates != null)
            {
                d0 = (double) chunkcoordinates.posX;
                y = (double) chunkcoordinates.posY;
                d1 = (double) chunkcoordinates.posZ;
                yaw = 90.0F;
                pitch = 0.0F;
            }
            // Cauldron end
            /*
            entity.setPositionRotation(d0, entity.locY, d1, 90.0F, 0.0F);
            if (entity.isAlive()) {
                worldserver.entityJoinedWorld(entity, false);
            }
            */
        }

        // worldserver.methodProfiler.b();
        if (i != 1)
        {
            // worldserver.methodProfiler.a("placing");
            d0 = (double) MathHelper.clamp_int((int) d0, -29999872, 29999872);
            d1 = (double) MathHelper.clamp_int((int) d1, -29999872, 29999872);
            /*
            if (entity.isAlive()) {
                worldserver1.addEntity(entity);
                entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
                worldserver1.entityJoinedWorld(entity, false);
                worldserver1.t().a(entity, d3, d4, d5, f);
            }

            worldserver.methodProfiler.b();
            */
        }

        // entity.spawnIn(worldserver1);
        return new Location(worldserver1.getWorld(), d0, y, d1, yaw, pitch);
    }

    // copy of original a(Entity, int, WorldServer, WorldServer) method with only entity repositioning logic
    public void repositionEntity(Entity entity, Location exit, boolean portal)
    {
        int i = entity.dimension;
        WorldServer worldserver = (WorldServer) entity.worldObj;
        WorldServer worldserver1 = ((CraftWorld) exit.getWorld()).getHandle();
        /*
        double d0 = entity.locX;
        double d1 = entity.locZ;
        double d2 = 8.0D;
        double d3 = entity.locX;
        double d4 = entity.locY;
        double d5 = entity.locZ;
        float f = entity.yaw;
        */
        worldserver.theProfiler.startSection("moving");
        entity.setLocationAndAngles(exit.getX(), exit.getY(), exit.getZ(), exit.getYaw(), exit.getPitch());

        if (entity.isEntityAlive())
        {
            worldserver.updateEntityWithOptionalForce(entity, false);
        }

        /*
        if (entity.dimension == -1) {
            d0 /= d2;
            d1 /= d2;
            entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
            if (entity.isAlive()) {
                worldserver.entityJoinedWorld(entity, false);
            }
        } else if (entity.dimension == 0) {
            d0 *= d2;
            d1 *= d2;
            entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
            if (entity.isAlive()) {
                worldserver.entityJoinedWorld(entity, false);
            }
        } else {
            ChunkCoordinates chunkcoordinates;

            if (i == 1) {
                chunkcoordinates = worldserver1.getSpawn();
            } else {
                chunkcoordinates = worldserver1.getDimensionSpawn();
            }

            d0 = (double) chunkcoordinates.x;
            entity.locY = (double) chunkcoordinates.y;
            d1 = (double) chunkcoordinates.z;
            entity.setPositionRotation(d0, entity.locY, d1, 90.0F, 0.0F);
            if (entity.isAlive()) {
                worldserver.entityJoinedWorld(entity, false);
            }
        }
        */
        worldserver.theProfiler.endSection();

        if (i != 1)
        {
            worldserver.theProfiler.startSection("placing");

            /*
            d0 = (double) MathHelper.a((int) d0, -29999872, 29999872);
            d1 = (double) MathHelper.a((int) d1, -29999872, 29999872);
            */
            if (entity.isEntityAlive())
            {
                // entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch)
                // worldserver1.s().a(entity, d3, d4, d5, f);
                if (portal)
                {
                    Vector velocity = entity.getBukkitEntity().getVelocity();
                    worldserver1.getDefaultTeleporter().adjustExit(entity, exit, velocity); // Should be getTravelAgent
                    entity.setLocationAndAngles(exit.getX(), exit.getY(), exit.getZ(), exit.getYaw(), exit.getPitch());

                    if (entity.motionX != velocity.getX() || entity.motionY != velocity.getY() || entity.motionZ != velocity.getZ())
                    {
                        entity.getBukkitEntity().setVelocity(velocity);
                    }
                }

                worldserver1.spawnEntityInWorld(entity);
                worldserver1.updateEntityWithOptionalForce(entity, false);
            }

            worldserver.theProfiler.endSection();
        }

        entity.setWorld(worldserver1);
        // CraftBukkit end
    }

    public void sendPlayerInfoToAllPlayers()
    {
        if (++this.playerPingIndex > 600)
        {
            this.playerPingIndex = 0;
        }

        /* CraftBukkit start - Remove updating of lag to players -- it spams way to much on big servers.
        if (this.playerPingIndex < this.playerEntityList.size())
        {
            EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playerEntityList.get(this.playerPingIndex);
            this.sendPacketToAllPlayers(new S38PacketPlayerListItem(entityplayermp.getCommandSenderName(), true, entityplayermp.ping));
        }
        // CraftBukkit end */
    }

    public void sendPacketToAllPlayers(Packet p_148540_1_)
    {
        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            ((EntityPlayerMP)this.playerEntityList.get(i)).playerNetServerHandler.sendPacket(p_148540_1_);
        }
    }

    public void sendPacketToAllPlayersInDimension(Packet p_148537_1_, int p_148537_2_)
    {
        for (int j = 0; j < this.playerEntityList.size(); ++j)
        {
            EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playerEntityList.get(j);

            if (entityplayermp.dimension == p_148537_2_)
            {
                entityplayermp.playerNetServerHandler.sendPacket(p_148537_1_);
            }
        }
    }

    public String func_152609_b(boolean p_152609_1_)
    {
        String s = "";
        ArrayList arraylist = Lists.newArrayList(this.playerEntityList);

        for (int i = 0; i < arraylist.size(); ++i)
        {
            if (i > 0)
            {
                s = s + ", ";
            }

            s = s + ((EntityPlayerMP)arraylist.get(i)).getCommandSenderName();

            if (p_152609_1_)
            {
                s = s + " (" + ((EntityPlayerMP)arraylist.get(i)).getUniqueID().toString() + ")";
            }
        }

        return s;
    }

    public String[] getAllUsernames()
    {
        String[] astring = new String[this.playerEntityList.size()];

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            astring[i] = ((EntityPlayerMP)this.playerEntityList.get(i)).getCommandSenderName();
        }

        return astring;
    }

    public GameProfile[] func_152600_g()
    {
        GameProfile[] agameprofile = new GameProfile[this.playerEntityList.size()];

        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            agameprofile[i] = ((EntityPlayerMP)this.playerEntityList.get(i)).getGameProfile();
        }

        return agameprofile;
    }

    public UserListBans func_152608_h()
    {
        return this.bannedPlayers;
    }

    public BanList getBannedIPs()
    {
        return this.bannedIPs;
    }

    public void func_152605_a(GameProfile p_152605_1_)
    {
        this.ops.func_152687_a(new UserListOpsEntry(p_152605_1_, this.mcServer.getOpPermissionLevel()));
    }

    public void func_152610_b(GameProfile p_152610_1_)
    {
        this.ops.func_152684_c(p_152610_1_);
    }

    public boolean func_152607_e(GameProfile p_152607_1_)
    {
        return !this.whiteListEnforced || this.ops.func_152692_d(p_152607_1_) || this.whiteListedPlayers.func_152692_d(p_152607_1_);
    }

    public boolean func_152596_g(GameProfile p_152596_1_)
    {
        return this.ops.func_152692_d(p_152596_1_) || this.mcServer.isSinglePlayer() && this.mcServer.worldServers[0].getWorldInfo().areCommandsAllowed() && this.mcServer.getServerOwner().equalsIgnoreCase(p_152596_1_.getName()) || this.commandsAllowedForAll;
    }

    public EntityPlayerMP func_152612_a(String p_152612_1_)
    {
        Iterator iterator = this.playerEntityList.iterator();
        EntityPlayerMP entityplayermp;

        do
        {
            if (!iterator.hasNext())
            {
                return null;
            }

            entityplayermp = (EntityPlayerMP)iterator.next();
        }
        while (!entityplayermp.getCommandSenderName().equalsIgnoreCase(p_152612_1_));

        return entityplayermp;
    }

    public List findPlayers(ChunkCoordinates p_82449_1_, int p_82449_2_, int p_82449_3_, int p_82449_4_, int p_82449_5_, int p_82449_6_, int p_82449_7_, Map p_82449_8_, String p_82449_9_, String p_82449_10_, World p_82449_11_)
    {
        if (this.playerEntityList.isEmpty())
        {
            return Collections.emptyList();
        }
        else
        {
            Object object = new ArrayList();
            boolean flag = p_82449_4_ < 0;
            boolean flag1 = p_82449_9_ != null && p_82449_9_.startsWith("!");
            boolean flag2 = p_82449_10_ != null && p_82449_10_.startsWith("!");
            int k1 = p_82449_2_ * p_82449_2_;
            int l1 = p_82449_3_ * p_82449_3_;
            p_82449_4_ = MathHelper.abs_int(p_82449_4_);

            if (flag1)
            {
                p_82449_9_ = p_82449_9_.substring(1);
            }

            if (flag2)
            {
                p_82449_10_ = p_82449_10_.substring(1);
            }

            for (int i2 = 0; i2 < this.playerEntityList.size(); ++i2)
            {
                EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playerEntityList.get(i2);

                if ((p_82449_11_ == null || entityplayermp.worldObj == p_82449_11_) && (p_82449_9_ == null || flag1 != p_82449_9_.equalsIgnoreCase(entityplayermp.getCommandSenderName())))
                {
                    if (p_82449_10_ != null)
                    {
                        Team team = entityplayermp.getTeam();
                        String s2 = team == null ? "" : team.getRegisteredName();

                        if (flag2 == p_82449_10_.equalsIgnoreCase(s2))
                        {
                            continue;
                        }
                    }

                    if (p_82449_1_ != null && (p_82449_2_ > 0 || p_82449_3_ > 0))
                    {
                        float f = p_82449_1_.getDistanceSquaredToChunkCoordinates(entityplayermp.getPlayerCoordinates());

                        if (p_82449_2_ > 0 && f < (float)k1 || p_82449_3_ > 0 && f > (float)l1)
                        {
                            continue;
                        }
                    }

                    if (this.func_96457_a(entityplayermp, p_82449_8_) && (p_82449_5_ == WorldSettings.GameType.NOT_SET.getID() || p_82449_5_ == entityplayermp.theItemInWorldManager.getGameType().getID()) && (p_82449_6_ <= 0 || entityplayermp.experienceLevel >= p_82449_6_) && entityplayermp.experienceLevel <= p_82449_7_)
                    {
                        ((List)object).add(entityplayermp);
                    }
                }
            }

            if (p_82449_1_ != null)
            {
                Collections.sort((List)object, new PlayerPositionComparator(p_82449_1_));
            }

            if (flag)
            {
                Collections.reverse((List)object);
            }

            if (p_82449_4_ > 0)
            {
                object = ((List)object).subList(0, Math.min(p_82449_4_, ((List)object).size()));
            }

            return (List)object;
        }
    }

    private boolean func_96457_a(EntityPlayer p_96457_1_, Map p_96457_2_)
    {
        if (p_96457_2_ != null && p_96457_2_.size() != 0)
        {
            Iterator iterator = p_96457_2_.entrySet().iterator();
            Entry entry;
            boolean flag;
            int i;

            do
            {
                if (!iterator.hasNext())
                {
                    return true;
                }

                entry = (Entry)iterator.next();
                String s = (String)entry.getKey();
                flag = false;

                if (s.endsWith("_min") && s.length() > 4)
                {
                    flag = true;
                    s = s.substring(0, s.length() - 4);
                }

                Scoreboard scoreboard = p_96457_1_.getWorldScoreboard();
                ScoreObjective scoreobjective = scoreboard.getObjective(s);

                if (scoreobjective == null)
                {
                    return false;
                }

                Score score = p_96457_1_.getWorldScoreboard().func_96529_a(p_96457_1_.getCommandSenderName(), scoreobjective);
                i = score.getScorePoints();

                if (i < ((Integer)entry.getValue()).intValue() && flag)
                {
                    return false;
                }
            }
            while (i <= ((Integer)entry.getValue()).intValue() || flag);

            return false;
        }
        else
        {
            return true;
        }
    }

    public void sendToAllNear(double p_148541_1_, double p_148541_3_, double p_148541_5_, double p_148541_7_, int p_148541_9_, Packet p_148541_10_)
    {
        this.sendToAllNearExcept((EntityPlayer)null, p_148541_1_, p_148541_3_, p_148541_5_, p_148541_7_, p_148541_9_, p_148541_10_);
    }

    public void sendToAllNearExcept(EntityPlayer p_148543_1_, double p_148543_2_, double p_148543_4_, double p_148543_6_, double p_148543_8_, int p_148543_10_, Packet p_148543_11_)
    {
        for (int j = 0; j < this.playerEntityList.size(); ++j)
        {
            EntityPlayerMP entityplayermp = (EntityPlayerMP) this.playerEntityList.get(j);

            // CraftBukkit start - Test if player receiving packet can see the source of the packet
            if (p_148543_1_ != null && p_148543_1_ instanceof EntityPlayerMP && !entityplayermp.getBukkitEntity().canSee(((EntityPlayerMP) p_148543_1_).getBukkitEntity()))
            {
                continue;
            }

            // CraftBukkit end

            if (entityplayermp != p_148543_1_ && entityplayermp.dimension == p_148543_10_)
            {
                double d4 = p_148543_2_ - entityplayermp.posX;
                double d5 = p_148543_4_ - entityplayermp.posY;
                double d6 = p_148543_6_ - entityplayermp.posZ;

                // Cauldron start - send packets only to players within configured player tracking range)
                if (p_148543_8_ > org.spigotmc.TrackingRange.getEntityTrackingRange(entityplayermp, 512))
                {
                    p_148543_8_ = org.spigotmc.TrackingRange.getEntityTrackingRange(entityplayermp, 512);
                }
                // Cauldron end

                if (d4 * d4 + d5 * d5 + d6 * d6 < p_148543_8_ * p_148543_8_)
                {
                    entityplayermp.playerNetServerHandler.sendPacket(p_148543_11_);
                }
            }
        }
    }

    public void saveAllPlayerData()
    {
        for (int i = 0; i < this.playerEntityList.size(); ++i)
        {
            this.writePlayerData((EntityPlayerMP)this.playerEntityList.get(i));
        }
    }

    public void func_152601_d(GameProfile p_152601_1_)
    {
        this.whiteListedPlayers.func_152687_a(new UserListWhitelistEntry(p_152601_1_));
    }

    public void func_152597_c(GameProfile p_152597_1_)
    {
        this.whiteListedPlayers.func_152684_c(p_152597_1_);
    }

    public UserListWhitelist func_152599_k()
    {
        return this.whiteListedPlayers;
    }

    public String[] func_152598_l()
    {
        return this.whiteListedPlayers.func_152685_a();
    }

    public UserListOps func_152603_m()
    {
        return this.ops;
    }

    public String[] func_152606_n()
    {
        return this.ops.func_152685_a();
    }

    public void loadWhiteList() {}

    public void updateTimeAndWeatherForPlayer(EntityPlayerMP p_72354_1_, WorldServer p_72354_2_)
    {
        p_72354_1_.playerNetServerHandler.sendPacket(new S03PacketTimeUpdate(p_72354_2_.getTotalWorldTime(), p_72354_2_.getWorldTime(), p_72354_2_.getGameRules().getGameRuleBooleanValue("doDaylightCycle")));

        if (p_72354_2_.isRaining())
        {
            // CraftBukkit start - handle player weather
            p_72354_1_.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(1, 0.0F));
            p_72354_1_.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(7, p_72354_2_.getRainStrength(1.0F)));
            p_72354_1_.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(8, p_72354_2_.getWeightedThunderStrength(1.0F)));
            p_72354_1_.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL, false);
            // CraftBukkit end
        }
    }

    public void syncPlayerInventory(EntityPlayerMP p_72385_1_)
    {
        p_72385_1_.sendContainerToPlayer(p_72385_1_.inventoryContainer);
        p_72385_1_.getBukkitEntity().updateScaledHealth(); // CraftBukkit - Update scaled health on respawn and worldchange
        p_72385_1_.playerNetServerHandler.sendPacket(new S09PacketHeldItemChange(p_72385_1_.inventory.currentItem));
    }

    public int getCurrentPlayerCount()
    {
        return this.playerEntityList.size();
    }

    public int getMaxPlayers()
    {
        return this.maxPlayers;
    }

    public String[] getAvailablePlayerDat()
    {
        // Cauldron start - don't crash if the overworld isn't loaded
        List<WorldServer> worldServers = this.mcServer.worlds;
        return worldServers.isEmpty() ? new String[0] : worldServers.get(0).getSaveHandler().getSaveHandler().getAvailablePlayerDat(); // CraftBukkit
        // Cauldron end
    }

    public void setWhiteListEnabled(boolean p_72371_1_)
    {
        this.whiteListEnforced = p_72371_1_;
    }

    public List getPlayerList(String p_72382_1_)
    {
        ArrayList arraylist = new ArrayList();
        Iterator iterator = this.playerEntityList.iterator();

        while (iterator.hasNext())
        {
            EntityPlayerMP entityplayermp = (EntityPlayerMP)iterator.next();

            if (entityplayermp.getPlayerIP().equals(p_72382_1_))
            {
                arraylist.add(entityplayermp);
            }
        }

        return arraylist;
    }

    public int getViewDistance()
    {
        return this.viewDistance;
    }

    public MinecraftServer getServerInstance()
    {
        return this.mcServer;
    }

    public NBTTagCompound getHostPlayerData()
    {
        return null;
    }

    @SideOnly(Side.CLIENT)
    public void func_152604_a(WorldSettings.GameType p_152604_1_)
    {
        this.gameType = p_152604_1_;
    }

    private void func_72381_a(EntityPlayerMP p_72381_1_, EntityPlayerMP p_72381_2_, World p_72381_3_)
    {
        if (p_72381_2_ != null)
        {
            p_72381_1_.theItemInWorldManager.setGameType(p_72381_2_.theItemInWorldManager.getGameType());
        }
        else if (this.gameType != null)
        {
            p_72381_1_.theItemInWorldManager.setGameType(this.gameType);
        }

        p_72381_1_.theItemInWorldManager.initializeGameType(p_72381_3_.getWorldInfo().getGameType());
    }

    @SideOnly(Side.CLIENT)
    public void setCommandsAllowedForAll(boolean p_72387_1_)
    {
        this.commandsAllowedForAll = p_72387_1_;
    }

    public void removeAllPlayers()
    {
        while (!this.playerEntityList.isEmpty())
        {
            // Spigot start
            EntityPlayerMP p = (EntityPlayerMP) this.playerEntityList.get(0);
            p.playerNetServerHandler.kickPlayerFromServer(this.mcServer.server.getShutdownMessage());

            if ((!this.playerEntityList.isEmpty()) && (this.playerEntityList.get(0) == p))
            {
                this.playerEntityList.remove(0);   // Prevent shutdown hang if already disconnected
            }

            // Spigot end
        }
    }

    // CraftBukkit start - Support multi-line messages
    public void sendMessage(IChatComponent[] ichatbasecomponent)
    {
        for (IChatComponent component : ichatbasecomponent)
        {
            sendChatMsgImpl(component, true);
        }
    }
    // CraftBukkit end

    public void sendChatMsgImpl(IChatComponent p_148544_1_, boolean p_148544_2_)
    {
        this.mcServer.addChatMessage(p_148544_1_);
        this.sendPacketToAllPlayers(new S02PacketChat(p_148544_1_, p_148544_2_));
    }

    public void sendChatMsg(IChatComponent p_148539_1_)
    {
        this.sendChatMsgImpl(p_148539_1_, true);
    }

    public StatisticsFile func_152602_a(EntityPlayer p_152602_1_)
    {
        UUID uuid = p_152602_1_.getUniqueID();
        StatisticsFile statisticsfile = uuid == null ? null : (StatisticsFile)this.field_148547_k.get(uuid);

        if (statisticsfile == null)
        {
            File file1 = new File(this.mcServer.worldServerForDimension(0).getSaveHandler().getWorldDirectory(), "stats");
            File file2 = new File(file1, uuid.toString() + ".json");

            if (!file2.exists())
            {
                File file3 = new File(file1, p_152602_1_.getCommandSenderName() + ".json");

                if (file3.exists() && file3.isFile())
                {
                    file3.renameTo(file2);
                }
            }

            statisticsfile = new StatisticsFile(this.mcServer, file2);
            statisticsfile.func_150882_a();
            this.field_148547_k.put(uuid, statisticsfile);
        }

        return statisticsfile;
    }

    public void func_152611_a(int p_152611_1_)
    {
        this.viewDistance = p_152611_1_;

        if (this.mcServer.worldServers != null)
        {
            WorldServer[] aworldserver = this.mcServer.worldServers;
            int j = aworldserver.length;

            for (int k = 0; k < j; ++k)
            {
                WorldServer worldserver = aworldserver[k];

                if (worldserver != null)
                {
                    worldserver.getPlayerManager().func_152622_a(p_152611_1_);
                }
            }
        }
    }

    @SideOnly(Side.SERVER)
    public boolean isWhiteListEnabled()
    {
        return this.whiteListEnforced;
    }
}