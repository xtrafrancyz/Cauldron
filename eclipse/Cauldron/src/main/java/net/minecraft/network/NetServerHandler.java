package net.minecraft.network;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import cpw.mods.fml.common.network.FMLNetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBeacon;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWritableBook;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet0KeepAlive;
import net.minecraft.network.packet.Packet101CloseWindow;
import net.minecraft.network.packet.Packet102WindowClick;
import net.minecraft.network.packet.Packet103SetSlot;
import net.minecraft.network.packet.Packet106Transaction;
import net.minecraft.network.packet.Packet107CreativeSetSlot;
import net.minecraft.network.packet.Packet108EnchantItem;
import net.minecraft.network.packet.Packet10Flying;
import net.minecraft.network.packet.Packet130UpdateSign;
import net.minecraft.network.packet.Packet131MapData;
import net.minecraft.network.packet.Packet13PlayerLookMove;
import net.minecraft.network.packet.Packet14BlockDig;
import net.minecraft.network.packet.Packet15Place;
import net.minecraft.network.packet.Packet16BlockItemSwitch;
import net.minecraft.network.packet.Packet18Animation;
import net.minecraft.network.packet.Packet19EntityAction;
import net.minecraft.network.packet.Packet202PlayerAbilities;
import net.minecraft.network.packet.Packet203AutoComplete;
import net.minecraft.network.packet.Packet204ClientInfo;
import net.minecraft.network.packet.Packet205ClientCommand;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet255KickDisconnect;
import net.minecraft.network.packet.Packet27PlayerInput;
import net.minecraft.network.packet.Packet39AttachEntity;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.network.packet.Packet40EntityMetadata;
import net.minecraft.network.packet.Packet53BlockChange;
import net.minecraft.network.packet.Packet6SpawnPosition;
import net.minecraft.network.packet.Packet7UseEntity;
import net.minecraft.network.packet.Packet9Respawn;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.BanEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraft.command.ICommandSender;

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
// CraftBukkit start
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.LazyPlayerSet;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
//import org.bukkit.event.Event; // Cauldron - use fully-qualified name to avoid clash with Forge
//import org.bukkit.event.block.Action; // Cauldron - use fully-qualified name to avoid clash with Forge
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.InventoryView;
// CraftBukkit end
// Cauldron start
import net.minecraftforge.cauldron.CauldronUtils;
import org.bukkit.event.inventory.InventoryType;
// Cauldron end

public class NetServerHandler extends NetHandler
{
    /** The underlying network manager for this server handler. */
    public final INetworkManager netManager;

    /** Reference to the MinecraftServer object. */
    private final MinecraftServer mcServer;

    /** This is set to true whenever a player disconnects from the server. */
    public boolean connectionClosed;

    /** Reference to the EntityPlayerMP object. */
    public EntityPlayerMP playerEntity;

    /** incremented each tick */
    private int currentTicks;

    /**
     * player is kicked if they float for over 80 ticks without flying enabled
     */
    public int ticksForFloatKick;
    private boolean field_72584_h;
    private int keepAliveRandomID;
    private long keepAliveTimeSent;
    private static Random randomGenerator = new Random();
    private long ticksOfLastKeepAlive;
    private volatile int chatSpamThresholdCount;
    private static final AtomicIntegerFieldUpdater chatSpamField = AtomicIntegerFieldUpdater.newUpdater(NetServerHandler.class, CauldronUtils.deobfuscatedEnvironment() ? "chatSpamThresholdCount" : "fiel" + "d_72581_m"); // CraftBukkit - multithreaded field
    private int creativeItemCreationSpamThresholdTally = 0;

    /** The last known x position for this connection. */
    private double lastPosX;

    /** The last known y position for this connection. */
    private double lastPosY;

    /** The last known z position for this connection. */
    private double lastPosZ;

    /** is true when the player has moved since his last movement packet */
    public boolean hasMoved = true; // CraftBukkit - private -> public
    private IntHashMap field_72586_s = new IntHashMap();

    public NetServerHandler(MinecraftServer par1MinecraftServer, INetworkManager par2INetworkManager, EntityPlayerMP par3EntityPlayerMP)
    {
        this.mcServer = par1MinecraftServer;
        this.netManager = par2INetworkManager;
        par2INetworkManager.setNetHandler(this);
        this.playerEntity = par3EntityPlayerMP;
        par3EntityPlayerMP.playerNetServerHandler = this;
        // CraftBukkit start
        this.server = par1MinecraftServer.server;
    }

    private final org.bukkit.craftbukkit.CraftServer server;
    private int lastTick = MinecraftServer.currentTick;
    private int lastDropTick = MinecraftServer.currentTick;
    private int dropCount = 0;
    private static final int PLACE_DISTANCE_SQUARED = 6 * 6;

    // Get position of last block hit for BlockDamageLevel.STOPPED
    private double lastPosX__ForEvent_CB = Double.MAX_VALUE;
    private double lastPosY__ForEvent_CB = Double.MAX_VALUE;
    private double lastPosZ__ForEvent_CB = Double.MAX_VALUE;
    private float lastPitch = Float.MAX_VALUE;
    private float lastYaw = Float.MAX_VALUE;
    private boolean justTeleported = false;
    private boolean spigotHasMoved; // Spigot

    // For the packet15 hack :(
    Long lastPacket;

    // Store the last block right clicked and what type it was
    private int lastMaterial;

    // Cauldron - rename getPlayer -> getPlayerB() to disambiguate with FML's getPlayer() method of the same name (below)
    // Plugins calling this method will be remapped appropriately, but CraftBukkit code should be updated
    public CraftPlayer getPlayerB()
    {
        return (this.playerEntity == null) ? null : (CraftPlayer) this.playerEntity.getBukkitEntity();
    }
    private final static HashSet<Integer> invalidItems = new HashSet<Integer>(java.util.Arrays.asList(8, 9, 10, 11, 26, 34, 36, 43, 51, 52, 55, 59, 60, 62, 63, 64, 68, 71, 74, 75, 83, 90, 92, 93, 94, 95, 104, 105, 115, 117, 118, 119, 125, 127, 132, 137, 140, 141, 142, 144)); // TODO: Check after every update.
    // CraftBukkit end

    /**
     * run once each game tick
     */
    public void networkTick()
    {
        this.field_72584_h = false;
        ++this.currentTicks;
        this.mcServer.theProfiler.startSection("packetflow");
        this.netManager.processReadPackets();
        this.mcServer.theProfiler.endStartSection("keepAlive");

        if ((long)this.currentTicks - this.ticksOfLastKeepAlive > 20L)
        {
            this.ticksOfLastKeepAlive = (long)this.currentTicks;
            this.keepAliveTimeSent = System.nanoTime() / 1000000L;
            this.keepAliveRandomID = randomGenerator.nextInt();
            this.sendPacketToPlayer(new Packet0KeepAlive(this.keepAliveRandomID));
        }

        // CraftBukkit start
        for (int spam; (spam = this.chatSpamThresholdCount) > 0 && !chatSpamField.compareAndSet(this, spam, spam - 1);) ;

        /* Use thread-safe field access instead
        if (this.m > 0) {
            --this.m;
        }
        */
        // CraftBukkit end

        if (this.creativeItemCreationSpamThresholdTally > 0)
        {
            --this.creativeItemCreationSpamThresholdTally;
        }

        this.mcServer.theProfiler.endStartSection("playerTick");
        this.mcServer.theProfiler.endSection();
    }

    public void kickPlayerFromServer(String par1Str)
    {
        if (!this.connectionClosed)
        {
            // CraftBukkit start
            String leaveMessage = EnumChatFormatting.YELLOW + this.playerEntity.getCommandSenderName() + " left the game.";
            PlayerKickEvent event = new PlayerKickEvent(this.server.getPlayer(this.playerEntity), par1Str, leaveMessage);

            if (this.server.getServer().isServerRunning())
            {
                this.server.getPluginManager().callEvent(event);
            }

            if (event.isCancelled())
            {
                // Do not kick the player
                return;
            }

            // Send the possibly modified leave message
            par1Str = event.getReason();
            // CraftBukkit end
            this.playerEntity.mountEntityAndWakeUp();
            this.sendPacketToPlayer(new Packet255KickDisconnect(par1Str));
            this.netManager.serverShutdown();
            // CraftBukkit start
            leaveMessage = event.getLeaveMessage();

            if (leaveMessage != null && leaveMessage.length() > 0)
            {
                this.mcServer.getConfigurationManager().sendChatMsg(ChatMessageComponent.createFromText(leaveMessage));
            }

            // CraftBukkit end
            this.mcServer.getConfigurationManager().disconnect(this.playerEntity);
            this.connectionClosed = true;
        }
    }

    public void func_110774_a(Packet27PlayerInput par1Packet27PlayerInput)
    {
        this.playerEntity.setEntityActionState(par1Packet27PlayerInput.func_111010_d(), par1Packet27PlayerInput.func_111012_f(), par1Packet27PlayerInput.func_111013_g(), par1Packet27PlayerInput.func_111011_h());
    }

    public void handleFlying(Packet10Flying par1Packet10Flying)
    {
        WorldServer worldserver = this.mcServer.worldServerForDimension(this.playerEntity.dimension);
        this.field_72584_h = true;

        if (!this.playerEntity.playerConqueredTheEnd)
        {
            double d0;

            if (!this.hasMoved)
            {
                d0 = par1Packet10Flying.yPosition - this.lastPosY;

                if (par1Packet10Flying.xPosition == this.lastPosX && d0 * d0 < 0.01D && par1Packet10Flying.zPosition == this.lastPosZ)
                {
                    this.hasMoved = true;
                }
            }

            // CraftBukkit start
            Player player = this.getPlayerB();
            Location from = new Location(player.getWorld(), lastPosX__ForEvent_CB, lastPosY__ForEvent_CB, lastPosZ__ForEvent_CB, lastYaw, lastPitch); // Get the Players previous Event location.
            Location to = player.getLocation().clone(); // Start off the To location as the Players current location.

            // If the packet contains movement information then we update the To location with the correct XYZ.
            if (par1Packet10Flying.moving && !(par1Packet10Flying.moving && par1Packet10Flying.yPosition == -999.0D && par1Packet10Flying.stance == -999.0D))
            {
                to.setX(par1Packet10Flying.xPosition);
                to.setY(par1Packet10Flying.yPosition);
                to.setZ(par1Packet10Flying.zPosition);
            }

            // If the packet contains look information then we update the To location with the correct Yaw & Pitch.
            if (par1Packet10Flying.rotating)
            {
                to.setYaw(par1Packet10Flying.yaw);
                to.setPitch(par1Packet10Flying.pitch);
            }

            // Prevent 40 event-calls for less than a single pixel of movement >.>
            double delta = Math.pow(this.lastPosX__ForEvent_CB - to.getX(), 2) + Math.pow(this.lastPosY__ForEvent_CB - to.getY(), 2) + Math.pow(this.lastPosZ__ForEvent_CB - to.getZ(), 2);
            float deltaAngle = Math.abs(this.lastYaw - to.getYaw()) + Math.abs(this.lastPitch - to.getPitch());

            if ((delta > 1f / 256 || deltaAngle > 10f) && (this.hasMoved && !this.playerEntity.isDead))
            {
                this.lastPosX__ForEvent_CB = to.getX();
                this.lastPosY__ForEvent_CB = to.getY();
                this.lastPosZ__ForEvent_CB = to.getZ();
                this.lastYaw = to.getYaw();
                this.lastPitch = to.getPitch();

                // Skip the first time we do this
                if (spigotHasMoved)   // Spigot - Better Check!
                {
                    PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);
                    this.server.getPluginManager().callEvent(event);

                    // If the event is cancelled we move the player back to their old location.
                    if (event.isCancelled())
                    {
                        this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet13PlayerLookMove(from.getX(), from.getY() + 1.6200000047683716D, from.getY(), from.getZ(), from.getYaw(), from.getPitch(), false));
                        return;
                    }

                    /* If a Plugin has changed the To destination then we teleport the Player
                    there to avoid any 'Moved wrongly' or 'Moved too quickly' errors.
                    We only do this if the Event was not cancelled. */
                    if (!to.equals(event.getTo()) && !event.isCancelled())
                    {
                        this.playerEntity.getBukkitEntity().teleport(event.getTo(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                        return;
                    }

                    /* Check to see if the Players Location has some how changed during the call of the event.
                    This can happen due to a plugin teleporting the player instead of using .setTo() */
                    if (!from.equals(this.getPlayerB().getLocation()) && this.justTeleported)
                    {
                        this.justTeleported = false;
                        return;
                    }
                }
                else
                {
                    spigotHasMoved = true;    // Spigot - Better Check!
                }
            }

            if (Double.isNaN(par1Packet10Flying.xPosition) || Double.isNaN(par1Packet10Flying.yPosition) || Double.isNaN(par1Packet10Flying.zPosition) || Double.isNaN(par1Packet10Flying.stance))
            {
                player.teleport(player.getWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.UNKNOWN);
                System.err.println(player.getName() + " was caught trying to crash the server with an invalid position.");
                player.kickPlayer("Nope!");
                return;
            }

            if (this.hasMoved && !this.playerEntity.isDead)
            {
                // CraftBukkit end
                double d1;
                double d2;
                double d3;

                if (this.playerEntity.ridingEntity != null)
                {
                    float f = this.playerEntity.rotationYaw;
                    float f1 = this.playerEntity.rotationPitch;
                    this.playerEntity.ridingEntity.updateRiderPosition();
                    d1 = this.playerEntity.posX;
                    d2 = this.playerEntity.posY;
                    d3 = this.playerEntity.posZ;

                    if (par1Packet10Flying.rotating)
                    {
                        f = par1Packet10Flying.yaw;
                        f1 = par1Packet10Flying.pitch;
                    }

                    this.playerEntity.onGround = par1Packet10Flying.onGround;
                    this.playerEntity.onUpdateEntity();
                    this.playerEntity.ySize = 0.0F;
                    this.playerEntity.setPositionAndRotation(d1, d2, d3, f, f1);

                    if (this.playerEntity.ridingEntity != null)
                    {
                        worldserver.uncheckedUpdateEntity(this.playerEntity.ridingEntity, true);  // Cauldron - required for vehicle AI
                    }

                    if (this.playerEntity.ridingEntity != null)
                    {
                        this.playerEntity.ridingEntity.updateRiderPosition();
                    }

                    if (!this.hasMoved) //Fixes teleportation kick while riding entities
                    {
                        return;
                    }

                    this.mcServer.getConfigurationManager().serverUpdateMountedMovingPlayer(this.playerEntity);

                    if (this.hasMoved)
                    {
                        this.lastPosX = this.playerEntity.posX;
                        this.lastPosY = this.playerEntity.posY;
                        this.lastPosZ = this.playerEntity.posZ;
                    }

                    worldserver.updateEntity(this.playerEntity);
                    return;
                }

                if (this.playerEntity.isPlayerSleeping())
                {
                    this.playerEntity.onUpdateEntity();
                    this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                    worldserver.updateEntity(this.playerEntity);
                    return;
                }

                d0 = this.playerEntity.posY;
                this.lastPosX = this.playerEntity.posX;
                this.lastPosY = this.playerEntity.posY;
                this.lastPosZ = this.playerEntity.posZ;
                d1 = this.playerEntity.posX;
                d2 = this.playerEntity.posY;
                d3 = this.playerEntity.posZ;
                float f2 = this.playerEntity.rotationYaw;
                float f3 = this.playerEntity.rotationPitch;

                if (par1Packet10Flying.moving && par1Packet10Flying.yPosition == -999.0D && par1Packet10Flying.stance == -999.0D)
                {
                    par1Packet10Flying.moving = false;
                }

                double d4;

                if (par1Packet10Flying.moving)
                {
                    d1 = par1Packet10Flying.xPosition;
                    d2 = par1Packet10Flying.yPosition;
                    d3 = par1Packet10Flying.zPosition;
                    d4 = par1Packet10Flying.stance - par1Packet10Flying.yPosition;

                    if (!this.playerEntity.isPlayerSleeping() && (d4 > 1.65D || d4 < 0.1D))
                    {
                        this.kickPlayerFromServer("Illegal stance");
                        this.mcServer.getLogAgent().logWarning(this.playerEntity.getCommandSenderName() + " had an illegal stance: " + d4);
                        return;
                    }

                    if (Math.abs(par1Packet10Flying.xPosition) > 3.2E7D || Math.abs(par1Packet10Flying.zPosition) > 3.2E7D)
                    {
                        // CraftBukkit - teleport to previous position instead of kicking, players get stuck
                        this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                        return;
                    }
                }

                if (par1Packet10Flying.rotating)
                {
                    f2 = par1Packet10Flying.yaw;
                    f3 = par1Packet10Flying.pitch;
                }

                this.playerEntity.onUpdateEntity();
                this.playerEntity.ySize = 0.0F;
                this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, f2, f3);

                if (!this.hasMoved)
                {
                    return;
                }

                d4 = d1 - this.playerEntity.posX;
                double d5 = d2 - this.playerEntity.posY;
                double d6 = d3 - this.playerEntity.posZ;
                //BUGFIX: min -> max, grabs the highest distance
                double d7 = Math.max(Math.abs(d4), Math.abs(this.playerEntity.motionX));
                double d8 = Math.max(Math.abs(d5), Math.abs(this.playerEntity.motionY));
                double d9 = Math.max(Math.abs(d6), Math.abs(this.playerEntity.motionZ));
                double d10 = d7 * d7 + d8 * d8 + d9 * d9;

                if (d10 > 100.0D && this.hasMoved && (!this.mcServer.isSinglePlayer() || !this.mcServer.getServerOwner().equals(this.playerEntity.getCommandSenderName())))   // CraftBukkit - Added this.checkMovement condition to solve this check being triggered by teleports
                {
                    this.mcServer.getLogAgent().logWarning(this.playerEntity.getCommandSenderName() + " moved too quickly! " + d4 + "," + d5 + "," + d6 + " (" + d7 + ", " + d8 + ", " + d9 + ")");
                    this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                    return;
                }

                float f4 = 0.0625F;
                boolean flag = worldserver.getCollidingBoundingBoxes(this.playerEntity, this.playerEntity.boundingBox.copy().contract((double)f4, (double)f4, (double)f4)).isEmpty();

                if (this.playerEntity.onGround && !par1Packet10Flying.onGround && d5 > 0.0D)
                {
                    this.playerEntity.addExhaustion(0.2F);
                }

                if (!this.hasMoved) //Fixes "Moved Too Fast" kick when being teleported while moving
                {
                    return;
                }

                this.playerEntity.moveEntity(d4, d5, d6);
                this.playerEntity.onGround = par1Packet10Flying.onGround;
                this.playerEntity.addMovementStat(d4, d5, d6);
                double d11 = d5;
                d4 = d1 - this.playerEntity.posX;
                d5 = d2 - this.playerEntity.posY;

                if (d5 > -0.5D || d5 < 0.5D)
                {
                    d5 = 0.0D;
                }

                d6 = d3 - this.playerEntity.posZ;
                d10 = d4 * d4 + d5 * d5 + d6 * d6;
                boolean flag1 = false;

                if (d10 > 0.0625D && !this.playerEntity.isPlayerSleeping() && !this.playerEntity.theItemInWorldManager.isCreative())
                {
                    flag1 = true;
                    this.mcServer.getLogAgent().logWarning(this.playerEntity.getCommandSenderName() + " moved wrongly!");
                }

                if (!this.hasMoved) //Fixes "Moved Too Fast" kick when being teleported while moving
                {
                    return;
                }

                this.playerEntity.setPositionAndRotation(d1, d2, d3, f2, f3);
                boolean flag2 = worldserver.getCollidingBoundingBoxes(this.playerEntity, this.playerEntity.boundingBox.copy().contract((double)f4, (double)f4, (double)f4)).isEmpty();

                if (flag && (flag1 || !flag2) && !this.playerEntity.isPlayerSleeping() && !this.playerEntity.noClip)
                {
                    this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, f2, f3);
                    return;
                }

                AxisAlignedBB axisalignedbb = this.playerEntity.boundingBox.copy().expand((double)f4, (double)f4, (double)f4).addCoord(0.0D, -0.55D, 0.0D);

                if (!this.mcServer.isFlightAllowed() && !this.playerEntity.capabilities.allowFlying && !worldserver.checkBlockCollision(axisalignedbb))   // CraftBukkit - check abilities instead of creative mode
                {
                    if (d11 >= -0.03125D)
                    {
                        ++this.ticksForFloatKick;

                        if (this.ticksForFloatKick > 80)
                        {
                            this.mcServer.getLogAgent().logWarning(this.playerEntity.getCommandSenderName() + " was kicked for floating too long!");
                            this.kickPlayerFromServer("Flying is not enabled on this server");
                            return;
                        }
                    }
                }
                else
                {
                    this.ticksForFloatKick = 0;
                }

                if (!this.hasMoved) //Fixes "Moved Too Fast" kick when being teleported while moving
                {
                    return;
                }

                this.playerEntity.onGround = par1Packet10Flying.onGround;
                this.mcServer.getConfigurationManager().serverUpdateMountedMovingPlayer(this.playerEntity);

                if (this.playerEntity.theItemInWorldManager.isCreative())
                {
                    return;    // CraftBukkit - fixed fall distance accumulating while being in Creative mode.
                }

                this.playerEntity.updateFlyingState(this.playerEntity.posY - d0, par1Packet10Flying.onGround);
            }
            else if (this.currentTicks % 20 == 0)
            {
                this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
            }
        }
    }

    /**
     * Moves the player to the specified destination and rotation
     */
    public void setPlayerLocation(double par1, double par3, double par5, float par7, float par8)
    {
        // CraftBukkit start - Delegate to teleport(Location)
        Player player = this.getPlayerB();
        Location from = player.getLocation();
        Location to = new Location(this.getPlayerB().getWorld(), par1, par3, par5, par7, par8);
        PlayerTeleportEvent event = new PlayerTeleportEvent(player, from, to, PlayerTeleportEvent.TeleportCause.UNKNOWN);
        this.server.getPluginManager().callEvent(event);
        from = event.getFrom();
        to = event.isCancelled() ? from : event.getTo();
        this.teleport(to);
    }

    public void teleport(Location dest)
    {
        double d0, d1, d2;
        float f, f1;
        d0 = dest.getX();
        d1 = dest.getY();
        d2 = dest.getZ();
        f = dest.getYaw();
        f1 = dest.getPitch();

        // TODO: make sure this is the best way to address this.
        if (Float.isNaN(f))
        {
            f = 0;
        }

        if (Float.isNaN(f1))
        {
            f1 = 0;
        }

        this.lastPosX__ForEvent_CB = d0;
        this.lastPosY__ForEvent_CB = d1;
        this.lastPosZ__ForEvent_CB = d2;
        this.lastYaw = f;
        this.lastPitch = f1;
        this.justTeleported = true;
        // CraftBukkit end
        this.hasMoved = false;
        this.lastPosX = d0;
        this.lastPosY = d1;
        this.lastPosZ = d2;
        this.playerEntity.setPositionAndRotation(d0, d1, d2, f, f1);
        this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet13PlayerLookMove(d0, d1 + 1.6200000047683716D, d1, d2, f, f1, false));
    }

    public void handleBlockDig(Packet14BlockDig par1Packet14BlockDig)
    {
        if (this.playerEntity.isDead)
        {
            return;    // CraftBukkit
        }

        WorldServer worldserver = this.mcServer.worldServerForDimension(this.playerEntity.dimension);
        this.playerEntity.func_143004_u();

        if (par1Packet14BlockDig.status == 4)
        {
            // CraftBukkit start
            // If the ticks aren't the same then the count starts from 0 and we update the lastDropTick.
            if (this.lastDropTick != MinecraftServer.currentTick)
            {
                this.dropCount = 0;
                this.lastDropTick = MinecraftServer.currentTick;
            }
            else
            {
                // Else we increment the drop count and check the amount.
                this.dropCount++;

                if (this.dropCount >= 20)
                {
                    this.mcServer.getLogAgent().logWarning(this.playerEntity.getCommandSenderName() + " dropped their items too quickly!");
                    this.kickPlayerFromServer("You dropped your items too quickly (Hacking?)");
                    return;
                }
            }

            // CraftBukkit end
            this.playerEntity.dropOneItem(false);
        }
        else if (par1Packet14BlockDig.status == 3)
        {
            this.playerEntity.dropOneItem(true);
        }
        else if (par1Packet14BlockDig.status == 5)
        {
            this.playerEntity.stopUsingItem();
        }
        else
        {
            boolean flag = false;

            if (par1Packet14BlockDig.status == 0)
            {
                flag = true;
            }

            if (par1Packet14BlockDig.status == 1)
            {
                flag = true;
            }

            if (par1Packet14BlockDig.status == 2)
            {
                flag = true;
            }

            int i = par1Packet14BlockDig.xPosition;
            int j = par1Packet14BlockDig.yPosition;
            int k = par1Packet14BlockDig.zPosition;

            if (flag)
            {
                double d0 = this.playerEntity.posX - ((double)i + 0.5D);
                double d1 = this.playerEntity.posY - ((double)j + 0.5D) + 1.5D;
                double d2 = this.playerEntity.posZ - ((double)k + 0.5D);
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;

                double dist = playerEntity.theItemInWorldManager.getBlockReachDistance() + 1;
                dist *= dist;

                if (d3 > dist)
                {
                    return;
                }

                if (j >= this.mcServer.getBuildLimit())
                {
                    return;
                }
            }

            if (par1Packet14BlockDig.status == 0)
            {
                // CraftBukkit start
                if (!this.mcServer.isBlockProtected(worldserver, i, j, k, this.playerEntity))
                {
                    this.playerEntity.theItemInWorldManager.onBlockClicked(i, j, k, par1Packet14BlockDig.face);
                }
                else
                {
                    CraftEventFactory.callPlayerInteractEvent(this.playerEntity, org.bukkit.event.block.Action.LEFT_CLICK_BLOCK, i, j, k, par1Packet14BlockDig.face, this.playerEntity.inventory.getCurrentItem());
                    this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(i, j, k, worldserver));
                    // Update any tile entity data for this block
                    TileEntity tileentity = worldserver.getBlockTileEntity(i, j, k);

                    if (tileentity != null)
                    {
                        this.playerEntity.playerNetServerHandler.sendPacketToPlayer(tileentity.getDescriptionPacket());
                    }

                    // CraftBukkit end
                }
            }
            else if (par1Packet14BlockDig.status == 2)
            {
                this.playerEntity.theItemInWorldManager.uncheckedTryHarvestBlock(i, j, k);

                if (worldserver.getBlockId(i, j, k) != 0)
                {
                    this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(i, j, k, worldserver));
                }
            }
            else if (par1Packet14BlockDig.status == 1)
            {
                this.playerEntity.theItemInWorldManager.cancelDestroyingBlock(i, j, k);

                if (worldserver.getBlockId(i, j, k) != 0)
                {
                    this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(i, j, k, worldserver));
                }
            }
        }
    }

    public void handlePlace(Packet15Place par1Packet15Place)
    {
        WorldServer worldserver = this.mcServer.worldServerForDimension(this.playerEntity.dimension);

        // CraftBukkit start
        if (this.playerEntity.isDead)
        {
            return;
        }

        // This is a horrible hack needed because the client sends 2 packets on 'right mouse click'
        // aimed at a block. We shouldn't need to get the second packet if the data is handled
        // but we cannot know what the client will do, so we might still get it
        //
        // If the time between packets is small enough, and the 'signature' similar, we discard the
        // second one. This sadly has to remain until Mojang makes their packets saner. :(
        //  -- Grum
        this.playerEntity.func_143004_u();

        if (par1Packet15Place.getDirection() == 255)
        {
            if (par1Packet15Place.getItemStack() != null && par1Packet15Place.getItemStack().itemID == this.lastMaterial && this.lastPacket != null && par1Packet15Place.creationTimeMillis - this.lastPacket < 100)
            {
                this.lastPacket = null;
                return;
            }
        }
        else
        {
            this.lastMaterial = par1Packet15Place.getItemStack() == null ? -1 : par1Packet15Place.getItemStack().itemID;
            this.lastPacket = par1Packet15Place.creationTimeMillis;
        }

        // CraftBukkit - if rightclick decremented the item, always send the update packet.
        // this is not here for CraftBukkit's own functionality; rather it is to fix
        // a notch bug where the item doesn't update correctly.
        boolean always = false;
        // CraftBukkit end
        ItemStack itemstack = this.playerEntity.inventory.getCurrentItem();
        boolean flag = false;
        int i = par1Packet15Place.getXPosition();
        int j = par1Packet15Place.getYPosition();
        int k = par1Packet15Place.getZPosition();
        int l = par1Packet15Place.getDirection();
        this.playerEntity.func_143004_u();

        if (par1Packet15Place.getDirection() == 255)
        {
            if (itemstack == null)
            {
                return;
            }

            // CraftBukkit start
            int itemstackAmount = itemstack.stackSize;
            org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(this.playerEntity, org.bukkit.event.block.Action.RIGHT_CLICK_AIR, itemstack);
            // Cauldron start - merge with Forge event
            PlayerInteractEvent forgeEvent = ForgeEventFactory.onPlayerInteract(this.playerEntity, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, 0, 0, 0, -1);

            if (event.useItemInHand() != org.bukkit.event.Event.Result.DENY && forgeEvent.useItem != net.minecraftforge.event.Event.Result.DENY)
            {
                this.playerEntity.theItemInWorldManager.tryUseItem(this.playerEntity, this.playerEntity.worldObj, itemstack);
            }
            // Cauldron end

            // CraftBukkit - notch decrements the counter by 1 in the above method with food,
            // snowballs and so forth, but he does it in a place that doesn't cause the
            // inventory update packet to get sent
            always = (itemstack.stackSize != itemstackAmount);
            // CraftBukkit end
        }
        else if (par1Packet15Place.getYPosition() >= this.mcServer.getBuildLimit() - 1 && (par1Packet15Place.getDirection() == 1 || par1Packet15Place.getYPosition() >= this.mcServer.getBuildLimit()))
        {
            this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat(ChatMessageComponent.createFromTranslationWithSubstitutions("build.tooHigh", new Object[] {Integer.valueOf(this.mcServer.getBuildLimit())}).setColor(EnumChatFormatting.RED)));
            flag = true;
        }
        else
        {
            // CraftBukkit start - Check if we can actually do something over this large a distance
            Location eyeLoc = this.getPlayerB().getEyeLocation();

            if (Math.pow(eyeLoc.getX() - i, 2) + Math.pow(eyeLoc.getY() - j, 2) + Math.pow(eyeLoc.getZ() - k, 2) > PLACE_DISTANCE_SQUARED)
            {
                return;
            }

            // Cauldron start - record place result so we can update client inventory slot if place event is cancelled. Fixes stacksize client-side bug
            if (!this.playerEntity.theItemInWorldManager.activateBlockOrUseItem(this.playerEntity, worldserver, itemstack, i, j, k, l, par1Packet15Place.getXOffset(), par1Packet15Place.getYOffset(), par1Packet15Place.getZOffset()))
            {
                always = true; // force Packet103SetSlot to be sent to client
            }
            // Cauldron end
            // CraftBukkit end
            flag = true;
        }

        if (flag)
        {
            this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(i, j, k, worldserver));

            if (l == 0)
            {
                --j;
            }

            if (l == 1)
            {
                ++j;
            }

            if (l == 2)
            {
                --k;
            }

            if (l == 3)
            {
                ++k;
            }

            if (l == 4)
            {
                --i;
            }

            if (l == 5)
            {
                ++i;
            }

            this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(i, j, k, worldserver));
        }

        itemstack = this.playerEntity.inventory.getCurrentItem();

        if (itemstack != null && itemstack.stackSize == 0)
        {
            this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem] = null;
            itemstack = null;
        }

        if (itemstack == null || itemstack.getMaxItemUseDuration() == 0)
        {
            this.playerEntity.playerInventoryBeingManipulated = true;
            this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem] = ItemStack.copyItemStack(this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem]);
            Slot slot = this.playerEntity.openContainer.getSlotFromInventory((IInventory) this.playerEntity.inventory, this.playerEntity.inventory.currentItem);
            // Cauldron start - abort if no slot, fixes RP2 timer crash block place - see #181   
            if (slot == null) 
            {
                this.playerEntity.playerInventoryBeingManipulated = false; // set flag to false or it will cause inventory to glitch on death
                return;
            }
            // Cauldron end
            this.playerEntity.openContainer.detectAndSendChanges();
            this.playerEntity.playerInventoryBeingManipulated = false;

            // CraftBukkit - TODO CHECK IF NEEDED -- new if structure might not need 'always'. Kept it in for now, but may be able to remove in future
            if (!ItemStack.areItemStacksEqual(this.playerEntity.inventory.getCurrentItem(), par1Packet15Place.getItemStack()) || always)
            {
                this.sendPacketToPlayer(new Packet103SetSlot(this.playerEntity.openContainer.windowId, slot.slotNumber, this.playerEntity.inventory.getCurrentItem()));
            }
        }
    }

    public void handleErrorMessage(String par1Str, Object[] par2ArrayOfObj)
    {
        if (this.connectionClosed)
        {
            return;    // CraftBukkit - Rarely it would send a disconnect line twice
        }

        this.mcServer.getLogAgent().logInfo(this.playerEntity.getCommandSenderName() + " lost connection: " + par1Str);
        // CraftBukkit start - We need to handle custom quit messages
        String quitMessage = this.mcServer.getConfigurationManager().disconnect(this.playerEntity);

        if ((quitMessage != null) && (quitMessage.length() > 0))
        {
            this.mcServer.getConfigurationManager().sendChatMsg(ChatMessageComponent.createFromText(quitMessage));
        }

        // CraftBukkit end
        this.connectionClosed = true;

        if (this.mcServer.isSinglePlayer() && this.playerEntity.getCommandSenderName().equals(this.mcServer.getServerOwner()))
        {
            this.mcServer.getLogAgent().logInfo("Stopping singleplayer server as player logged out");
            this.mcServer.initiateShutdown();
        }
    }

    /**
     * Default handler called for packets that don't have their own handlers in NetClientHandler; currentlly does
     * nothing.
     */
    public void unexpectedPacket(Packet par1Packet)
    {
        if (this.connectionClosed)
        {
            return;    // CraftBukkit
        }

        this.mcServer.getLogAgent().logWarning(this.getClass() + " wasn\'t prepared to deal with a " + par1Packet.getClass());
        this.kickPlayerFromServer("Protocol error, unexpected packet");
    }

    /**
     * addToSendQueue. if it is a chat packet, check before sending it
     */
    public void sendPacketToPlayer(Packet par1Packet)
    {
        if (par1Packet instanceof Packet3Chat)
        {
            Packet3Chat packet3chat = (Packet3Chat)par1Packet;
            int i = this.playerEntity.getChatVisibility();

            if (i == 2)
            {
                return;
            }

            if (i == 1 && !packet3chat.getIsServer())
            {
                return;
            }
        }

        // CraftBukkit start
        if (par1Packet == null)
        {
            return;
        }
        else if (par1Packet instanceof Packet6SpawnPosition)
        {
            Packet6SpawnPosition packet6 = (Packet6SpawnPosition) par1Packet;
            this.playerEntity.compassTarget = new Location(this.getPlayerB().getWorld(), packet6.xPosition, packet6.yPosition, packet6.zPosition);
        }

        // CraftBukkit end

        try
        {
            this.netManager.addToSendQueue(par1Packet);
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Sending packet");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Packet being sent");
            crashreportcategory.addCrashSectionCallable("Packet ID", new CallablePacketID(this, par1Packet));
            crashreportcategory.addCrashSectionCallable("Packet class", new CallablePacketClass(this, par1Packet));
            throw new ReportedException(crashreport);
        }
    }

    public void handleBlockItemSwitch(Packet16BlockItemSwitch par1Packet16BlockItemSwitch)
    {
        // CraftBukkit start
        if (this.playerEntity.isDead)
        {
            return;
        }

        if (par1Packet16BlockItemSwitch.id >= 0 && par1Packet16BlockItemSwitch.id < InventoryPlayer.getHotbarSize())
        {
            PlayerItemHeldEvent event = new PlayerItemHeldEvent(this.getPlayerB(), this.playerEntity.inventory.currentItem, par1Packet16BlockItemSwitch.id);
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled())
            {
                this.sendPacketToPlayer(new Packet16BlockItemSwitch(this.playerEntity.inventory.currentItem));
                this.playerEntity.func_143004_u();
                return;
            }

            // CraftBukkit end
            this.playerEntity.inventory.currentItem = par1Packet16BlockItemSwitch.id;
            this.playerEntity.func_143004_u();
        }
        else
        {
            this.mcServer.getLogAgent().logWarning(this.playerEntity.getCommandSenderName() + " tried to set an invalid carried item");
            this.kickPlayerFromServer("Nope!"); // CraftBukkit
        }
    }

    public void handleChat(Packet3Chat par1Packet3Chat)
    {
        par1Packet3Chat = FMLNetworkHandler.handleChatMessage(this, par1Packet3Chat);

        if (par1Packet3Chat == null || par1Packet3Chat.message == null)
        {
            return;
        }
        if (this.playerEntity.getChatVisibility() == 2)
        {
            this.sendPacketToPlayer(new Packet3Chat(ChatMessageComponent.createFromTranslationKey("chat.cannotSend").setColor(EnumChatFormatting.RED)));
        }
        else
        {
            this.playerEntity.func_143004_u();
            String s = par1Packet3Chat.message;

            if (s.length() > 100)
            {
                // CraftBukkit start
                if (par1Packet3Chat.canProcessAsync())
                {
                    Waitable waitable = new Waitable()
                    {
                        @Override
                        protected Object evaluate()
                        {
                            NetServerHandler.this.kickPlayerFromServer("Chat message too long");
                            return null;
                        }
                    };
                    this.mcServer.processQueue.add(waitable);

                    try
                    {
                        waitable.get();
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                    catch (ExecutionException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                else
                {
                    this.kickPlayerFromServer("Chat message too long");
                }

                // CraftBukkit end
            }
            else
            {
                for (int i = 0; i < s.length(); ++i)
                {
                    if (!ChatAllowedCharacters.isAllowedCharacter(s.charAt(i)))
                    {
                        // CraftBukkit start
                        if (par1Packet3Chat.canProcessAsync())
                        {
                            Waitable waitable = new Waitable()
                            {
                                @Override
                                protected Object evaluate()
                                {
                                    NetServerHandler.this.kickPlayerFromServer("Illegal characters in chat");
                                    return null;
                                }
                            };
                            this.mcServer.processQueue.add(waitable);

                            try
                            {
                                waitable.get();
                            }
                            catch (InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                            }
                            catch (ExecutionException e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                        else
                        {
                            this.kickPlayerFromServer("Illegal characters in chat");
                        }

                        // CraftBukkit end
                        return;
                    }
                }

                // CraftBukkit start
                if (this.playerEntity.getChatVisibility() == 1 && !s.startsWith("/"))
                {
                    this.sendPacketToPlayer(new Packet3Chat(ChatMessageComponent.createFromTranslationKey("chat.cannotSend").setColor(EnumChatFormatting.RED)));
                    return;
                }

                if (!par1Packet3Chat.canProcessAsync())
                {
                    try
                    {
                        this.mcServer.server.playerCommandState = true;
                        this.chat(s, par1Packet3Chat.canProcessAsync());
                    }
                    finally
                    {
                        this.mcServer.server.playerCommandState = false;
                    }
                }
                else
                {
                    this.chat(s, par1Packet3Chat.canProcessAsync());
                }

                // This section stays because it is only applicable to packets
                // Spigot - spam exclusions
                boolean counted = true;
                for ( String exclude : org.spigotmc.SpigotConfig.spamExclusions )
                {
                    if ( exclude != null && s.startsWith( exclude ) )
                    {
                        counted = false;
                        break;
                    }
                }
                if (counted && chatSpamField.addAndGet(this, 20) > 200 && !this.mcServer.getConfigurationManager().isPlayerOpped(this.playerEntity.getCommandSenderName())) { // CraftBukkit use thread-safe spam
                    if (par1Packet3Chat.canProcessAsync()) {
                        Waitable waitable = new Waitable() {
                            @Override
                            protected Object evaluate() {
                                NetServerHandler.this.kickPlayerFromServer("disconnect.spam");
                                return null;
                            }
                        };

                        this.mcServer.processQueue.add(waitable);

                        try {
                            waitable.get();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        this.kickPlayerFromServer("disconnect.spam");
                    }
                }
            }
        }
    }

    public void chat(String s, boolean async)
    {
        if (!this.playerEntity.isDead)
        {
            if (s.length() == 0)
            {
                this.mcServer.getLogAgent().logWarning(this.playerEntity.getCommandSenderName() + " tried to send an empty message");
                return;
            }

            if (getPlayerB().isConversing())
            {
                getPlayerB().acceptConversationInput(s);
                return;
            }

            if (s.startsWith("/"))
            {
                this.handleSlashCommand(s);
                return;
            }
            else
            {
                // Cauldron start - call Forge event
                ChatMessageComponent chatmessagecomponent = ChatMessageComponent.createFromTranslationWithSubstitutions("chat.type.text", new Object[] {this.playerEntity.getTranslatedEntityName(), s});
                chatmessagecomponent = ForgeHooks.onServerChatEvent(this, s, chatmessagecomponent);
                // Cauldron end

                Player player = this.getPlayerB();
                AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(async, player, s, new LazyPlayerSet());
                event.setCancelled(chatmessagecomponent == null); // Cauldron pre-cancel event if forge event was cancelled
                this.server.getPluginManager().callEvent(event);

                if (PlayerChatEvent.getHandlerList().getRegisteredListeners().length != 0)
                {
                    // Evil plugins still listening to deprecated event
                    final PlayerChatEvent queueEvent = new PlayerChatEvent(player, event.getMessage(), event.getFormat(), event.getRecipients());
                    queueEvent.setCancelled(event.isCancelled());
                    Waitable waitable = new Waitable()
                    {
                        @Override
                        protected Object evaluate()
                        {
                            org.bukkit.Bukkit.getPluginManager().callEvent(queueEvent);

                            if (queueEvent.isCancelled())
                            {
                                return null;
                            }

                            String message = String.format(queueEvent.getFormat(), queueEvent.getPlayer().getDisplayName(), queueEvent.getMessage());
                            NetServerHandler.this.mcServer.console.sendMessage(message);

                            if (((LazyPlayerSet) queueEvent.getRecipients()).isLazy())
                            {
                                for (Object player : NetServerHandler.this.mcServer.getConfigurationManager().playerEntityList)
                                {
                                    ((EntityPlayerMP) player).sendChatToPlayer(ChatMessageComponent.createFromText(message));
                                }
                            }
                            else
                            {
                                for (Player player : queueEvent.getRecipients())
                                {
                                    player.sendMessage(message);
                                }
                            }

                            return null;
                        }
                    };

                    if (async)
                    {
                        mcServer.processQueue.add(waitable);
                    }
                    else
                    {
                        waitable.run();
                    }

                    try
                    {
                        waitable.get();
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt(); // This is proper habit for java. If we aren't handling it, pass it on!
                    }
                    catch (ExecutionException e)
                    {
                        throw new RuntimeException("Exception processing chat event", e.getCause());
                    }
                }
                else
                {
                    if (event.isCancelled() || chatmessagecomponent == null)
                    {
                        return;
                    }

                    s = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
                    mcServer.console.sendMessage(s);

                    if (((LazyPlayerSet) event.getRecipients()).isLazy())
                    {
                        for (Object recipient : mcServer.getConfigurationManager().playerEntityList)
                        {
                            ((EntityPlayerMP) recipient).sendChatToPlayer(ChatMessageComponent.createFromText(s));
                        }
                    }
                    else
                    {
                        for (Player recipient : event.getRecipients())
                        {
                            recipient.sendMessage(s);
                        }
                    }
                }
            }
        }

        return;
    }
    // CraftBukkit end

    /**
     * Processes a / command
     */
    private void handleSlashCommand(String par1Str)
    {
        org.bukkit.craftbukkit.SpigotTimings.playerCommandTimer.startTiming(); // Spigot
        // CraftBukkit start
        CraftPlayer player = this.getPlayerB();
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, par1Str, new LazyPlayerSet());
        this.server.getPluginManager().callEvent(event);

        if (event.isCancelled())
        {
            org.bukkit.craftbukkit.SpigotTimings.playerCommandTimer.stopTiming(); // Spigot
            return;
        }

        try
        {
            if (org.spigotmc.SpigotConfig.logCommands)
            {
                this.mcServer.getLogAgent().logInfo(event.getPlayer().getName() + " issued server command: " + event.getMessage()); // Spigot
            }
            // Cauldron start - handle bukkit/vanilla commands
            int space = event.getMessage().indexOf(" ");
            // if bukkit command exists then execute it over vanilla
            if (this.server.getCommandMap().getCommand(event.getMessage().substring(1, space != -1 ? space : event.getMessage().length())) != null)
            {
                this.server.dispatchCommand(event.getPlayer(), event.getMessage().substring(1));
                org.bukkit.craftbukkit.SpigotTimings.playerCommandTimer.stopTiming(); // Spigot
                return;
            }
            else // process vanilla command
            {
                this.server.dispatchVanillaCommand(event.getPlayer(), event.getMessage().substring(1));
                org.bukkit.craftbukkit.SpigotTimings.playerCommandTimer.stopTiming(); // Spigot
                return;
            }
        }
        catch (org.bukkit.command.CommandException ex)
        {
            player.sendMessage(org.bukkit.ChatColor.RED + "An internal error occurred while attempting to perform this command");
            java.util.logging.Logger.getLogger(NetServerHandler.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            org.bukkit.craftbukkit.SpigotTimings.playerCommandTimer.stopTiming(); // Spigot
            return;
        }

        // CraftBukkit end
        /* CraftBukkit start - No longer needed as we have already handled it in server.dispatchServerCommand above.
        this.minecraftServer.getCommandHandler().a(this.player, s);
        // CraftBukkit end */
    }

    public void handleAnimation(Packet18Animation par1Packet18Animation)
    {
        if (this.playerEntity.isDead)
        {
            return;    // CraftBukkit
        }

        this.playerEntity.func_143004_u();

        if (par1Packet18Animation.animate == 1)
        {
            // CraftBukkit start - Raytrace to look for 'rogue armswings'
            float f = 1.0F;
            float f1 = this.playerEntity.prevRotationPitch + (this.playerEntity.rotationPitch - this.playerEntity.prevRotationPitch) * f;
            float f2 = this.playerEntity.prevRotationYaw + (this.playerEntity.rotationYaw - this.playerEntity.prevRotationYaw) * f;
            double d0 = this.playerEntity.prevPosX + (this.playerEntity.posX - this.playerEntity.prevPosX) * (double) f;
            double d1 = this.playerEntity.prevPosY + (this.playerEntity.posY - this.playerEntity.prevPosY) * (double) f + 1.62D - (double) this.playerEntity.yOffset;
            double d2 = this.playerEntity.prevPosZ + (this.playerEntity.posZ - this.playerEntity.prevPosZ) * (double) f;
            Vec3 vec3 = this.playerEntity.worldObj.getWorldVec3Pool().getVecFromPool(d0, d1, d2);
            float f3 = MathHelper.cos(-f2 * 0.017453292F - (float)Math.PI);
            float f4 = MathHelper.sin(-f2 * 0.017453292F - (float)Math.PI);
            float f5 = -MathHelper.cos(-f1 * 0.017453292F);
            float f6 = MathHelper.sin(-f1 * 0.017453292F);
            float f7 = f4 * f5;
            float f8 = f3 * f5;
            double d3 = 5.0D;
            Vec3 vec31 = vec3.addVector((double) f7 * d3, (double) f6 * d3, (double) f8 * d3);
            MovingObjectPosition movingobjectposition = this.playerEntity.worldObj.clip(vec3, vec31, true);

            if (movingobjectposition == null || movingobjectposition.typeOfHit != EnumMovingObjectType.TILE)
            {
                CraftEventFactory.callPlayerInteractEvent(this.playerEntity, org.bukkit.event.block.Action.LEFT_CLICK_AIR, this.playerEntity.inventory.getCurrentItem());
            }

            // Arm swing animation
            PlayerAnimationEvent event = new PlayerAnimationEvent(this.getPlayerB());
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled())
            {
                return;
            }

            // CraftBukkit end
            this.playerEntity.swingItem();
        }
    }

    /**
     * runs registerPacket on the given Packet19EntityAction
     */
    public void handleEntityAction(Packet19EntityAction par1Packet19EntityAction)
    {
        // CraftBukkit start
        if (this.playerEntity.isDead)
        {
            return;
        }

        this.playerEntity.func_143004_u();

        if (par1Packet19EntityAction.action == 1 || par1Packet19EntityAction.action == 2)
        {
            PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getPlayerB(), par1Packet19EntityAction.action == 1);
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled())
            {
                return;
            }
        }

        if (par1Packet19EntityAction.action == 4 || par1Packet19EntityAction.action == 5)
        {
            PlayerToggleSprintEvent event = new PlayerToggleSprintEvent(this.getPlayerB(), par1Packet19EntityAction.action == 4);
            this.server.getPluginManager().callEvent(event);

            if (event.isCancelled())
            {
                return;
            }
        }

        // CraftBukkit end

        if (par1Packet19EntityAction.action == 1)
        {
            this.playerEntity.setSneaking(true);
        }
        else if (par1Packet19EntityAction.action == 2)
        {
            this.playerEntity.setSneaking(false);
        }
        else if (par1Packet19EntityAction.action == 4)
        {
            this.playerEntity.setSprinting(true);
        }
        else if (par1Packet19EntityAction.action == 5)
        {
            this.playerEntity.setSprinting(false);
        }
        else if (par1Packet19EntityAction.action == 3)
        {
            this.playerEntity.wakeUpPlayer(false, true, true);
            this.hasMoved = false;
        }
        else if (par1Packet19EntityAction.action == 6)
        {
            if (this.playerEntity.ridingEntity != null && this.playerEntity.ridingEntity instanceof EntityHorse)
            {
                ((EntityHorse)this.playerEntity.ridingEntity).setJumpPower(par1Packet19EntityAction.auxData);
            }
        }
        else if (par1Packet19EntityAction.action == 7 && this.playerEntity.ridingEntity != null && this.playerEntity.ridingEntity instanceof EntityHorse)
        {
            ((EntityHorse)this.playerEntity.ridingEntity).openGUI(this.playerEntity);
        }
    }

    public void handleKickDisconnect(Packet255KickDisconnect par1Packet255KickDisconnect)
    {
        this.netManager.networkShutdown("disconnect.quitting", new Object[0]);
    }

    /**
     * returns 0 for memoryMapped connections
     */
    public int packetSize()
    {
        return this.netManager.packetSize();
    }

    public void handleUseEntity(Packet7UseEntity par1Packet7UseEntity)
    {
        if (this.playerEntity.isDead)
        {
            return;    // CraftBukkit
        }

        WorldServer worldserver = this.mcServer.worldServerForDimension(this.playerEntity.dimension);
        Entity entity = worldserver.getEntityByID(par1Packet7UseEntity.targetEntity);
        this.playerEntity.func_143004_u();

        if (entity == this.playerEntity)
        {
            this.kickPlayerFromServer("Cannot interact with self!");
        }
        else
        {
            if (entity != null)
            {
                boolean flag = this.playerEntity.canEntityBeSeen(entity);
                double d0 = 36.0D;

                if (!flag)
                {
                    d0 = 9.0D;
                }

                if (this.playerEntity.getDistanceSqToEntity(entity) < d0)
                {
                    ItemStack itemInHand = this.playerEntity.inventory.getCurrentItem(); // CraftBukkit

                    if (par1Packet7UseEntity.isLeftClick == 0)
                    {
                        // CraftBukkit start
                        boolean triggerTagUpdate = itemInHand != null && itemInHand.itemID == Item.nameTag.itemID && entity instanceof EntityLiving;
                        boolean triggerChestUpdate = itemInHand != null && itemInHand.itemID == Block.chest.blockID && entity instanceof EntityHorse;
                        boolean triggerLeashUpdate = itemInHand != null && itemInHand.itemID == Item.leash.itemID && entity instanceof EntityLiving;
                        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent((Player) this.getPlayerB(), entity.getBukkitEntity());
                        this.server.getPluginManager().callEvent(event);

                        if (triggerLeashUpdate && (event.isCancelled() || this.playerEntity.inventory.getCurrentItem() == null || this.playerEntity.inventory.getCurrentItem().itemID != Item.leash.itemID))
                        {
                            // Refresh the current leash state
                            this.sendPacketToPlayer(new Packet39AttachEntity(1, entity, ((EntityLiving) entity).getLeashedToEntity()));
                        }

                        if (triggerTagUpdate && (event.isCancelled() || this.playerEntity.inventory.getCurrentItem() == null || this.playerEntity.inventory.getCurrentItem().itemID != Item.nameTag.itemID))
                        {
                            // Refresh the current entity metadata
                            this.sendPacketToPlayer(new Packet40EntityMetadata(entity.entityId, entity.dataWatcher, true));
                        }

                        if (triggerChestUpdate && (event.isCancelled() || this.playerEntity.inventory.getCurrentItem() == null || this.playerEntity.inventory.getCurrentItem().itemID != Block.chest.blockID))
                        {
                            this.sendPacketToPlayer(new Packet40EntityMetadata(entity.entityId, entity.dataWatcher, true));
                        }

                        if (event.isCancelled())
                        {
                            return;
                        }

                        // CraftBukkit end
                        this.playerEntity.interactWith(entity);

                        // CraftBukkit start - Update the client if the item is an infinite one
                        if (itemInHand != null && itemInHand.stackSize <= -1)
                        {
                            this.playerEntity.sendContainerToPlayer(this.playerEntity.openContainer);
                        }
                    }
                    else if (par1Packet7UseEntity.isLeftClick == 1)
                    {
                        // CraftBukkit - Check for player
                       if ((entity instanceof EntityItem) || (entity instanceof EntityXPOrb) || (entity instanceof EntityArrow) || (entity == this.playerEntity))
                        {
                            String type = entity.getClass().getSimpleName();
                            kickPlayerFromServer("Attacking an " + type + " is not permitted");
                            System.out.println("Player " + playerEntity.getCommandSenderName() + " tried to attack an " + type + ", so I have disconnected them for exploiting.");
                            return;
                        }

                        this.playerEntity.attackTargetEntityWithCurrentItem(entity);

                        if (itemInHand != null && itemInHand.stackSize <= -1)
                        {
                            this.playerEntity.sendContainerToPlayer(this.playerEntity.openContainer);
                        }
                }
                // CraftBukkit end
                }
            }
        }
    }

    public void handleClientCommand(Packet205ClientCommand par1Packet205ClientCommand)
    {
        this.playerEntity.func_143004_u();

        if (par1Packet205ClientCommand.forceRespawn == 1)
        {
            if (this.playerEntity.playerConqueredTheEnd)
            {
                this.mcServer.getConfigurationManager().changeDimension(this.playerEntity, 0, PlayerTeleportEvent.TeleportCause.END_PORTAL); // CraftBukkit - reroute logic through custom portal management
            }
            else if (this.playerEntity.getServerForPlayer().getWorldInfo().isHardcoreModeEnabled())
            {
                if (this.mcServer.isSinglePlayer() && this.playerEntity.getCommandSenderName().equals(this.mcServer.getServerOwner()))
                {
                    this.playerEntity.playerNetServerHandler.kickPlayerFromServer("You have died. Game over, man, it\'s game over!");
                    this.mcServer.deleteWorldAndStopServer();
                }
                else
                {
                    BanEntry banentry = new BanEntry(this.playerEntity.getCommandSenderName());
                    banentry.setBanReason("Death in Hardcore");
                    this.mcServer.getConfigurationManager().getBannedPlayers().put(banentry);
                    this.playerEntity.playerNetServerHandler.kickPlayerFromServer("You have died. Game over, man, it\'s game over!");
                }
            }
            else
            {
                if (this.playerEntity.getHealth() > 0.0F)
                {
                    return;
                }

                this.playerEntity = this.mcServer.getConfigurationManager().respawnPlayer(this.playerEntity, playerEntity.dimension, false);
            }
        }
    }

    /**
     * If this returns false, all packets will be queued for the main thread to handle, even if they would otherwise be
     * processed asynchronously. Used to avoid processing packets on the client before the world has been downloaded
     * (which happens on the main thread)
     */
    public boolean canProcessPacketsAsync()
    {
        return true;
    }

    /**
     * respawns the player
     */
    public void handleRespawn(Packet9Respawn par1Packet9Respawn) {}

    public void handleCloseWindow(Packet101CloseWindow par1Packet101CloseWindow)
    {
        if (this.playerEntity.isDead)
        {
            return;    // CraftBukkit
        }

        // Cauldron start - vanilla compatibility
        try 
        {
            if (this.playerEntity.openContainer.getBukkitView() != null)
            {
                CraftEventFactory.handleInventoryCloseEvent(this.playerEntity); // CraftBukkit
            }
        }
        catch (AbstractMethodError e)
        {
            // do nothing
        }
        // Cauldron end
        this.playerEntity.closeContainer();
    }

    public void handleWindowClick(Packet102WindowClick par1Packet102WindowClick)
    {
        if (this.playerEntity.isDead)
        {
            return; // CraftBukkit
        }

        this.playerEntity.func_143004_u();

        if (this.playerEntity.openContainer.windowId == par1Packet102WindowClick.window_Id && this.playerEntity.openContainer.isPlayerNotUsingContainer(this.playerEntity))
        {
            // CraftBukkit start - Call InventoryClickEvent
            if (par1Packet102WindowClick.inventorySlot < -1 && par1Packet102WindowClick.inventorySlot != -999)
            {
                return;
            }

            InventoryView inventory = this.playerEntity.openContainer.getBukkitView();
            SlotType type = CraftInventoryView.getSlotType(inventory, par1Packet102WindowClick.inventorySlot);
            InventoryClickEvent event = null;
            ClickType click = ClickType.UNKNOWN;
            InventoryAction action = InventoryAction.UNKNOWN;
            ItemStack itemstack = null;

            // Cauldron start - some containers such as NEI's Creative Container does not have a view at this point so we need to create one
            if (inventory == null)
            {
                inventory = new CraftInventoryView(this.playerEntity.getBukkitEntity(), MinecraftServer.getServer().server.createInventory(this.playerEntity.getBukkitEntity(), InventoryType.CHEST), this.playerEntity.openContainer);
                this.playerEntity.openContainer.bukkitView = inventory;
            }
            // Cauldron end

            if (par1Packet102WindowClick.inventorySlot == -1)
            {
                type = SlotType.OUTSIDE; // override
                click = par1Packet102WindowClick.mouseClick == 0 ? ClickType.WINDOW_BORDER_LEFT : ClickType.WINDOW_BORDER_RIGHT;
                action = InventoryAction.NOTHING;
            }
            else if (par1Packet102WindowClick.holdingShift == 0)
            {
                if (par1Packet102WindowClick.mouseClick == 0)
                {
                    click = ClickType.LEFT;
                }
                else if (par1Packet102WindowClick.mouseClick == 1)
                {
                    click = ClickType.RIGHT;
                }

                if (par1Packet102WindowClick.mouseClick == 0 || par1Packet102WindowClick.mouseClick == 1)
                {
                    action = InventoryAction.NOTHING; // Don't want to repeat ourselves

                    if (par1Packet102WindowClick.inventorySlot == -999)
                    {
                        if (playerEntity.inventory.getItemStack() != null)
                        {
                            action = par1Packet102WindowClick.mouseClick == 0 ? InventoryAction.DROP_ALL_CURSOR : InventoryAction.DROP_ONE_CURSOR;
                        }
                    }
                    else
                    {
                        Slot slot = this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot);

                        if (slot != null)
                        {
                            ItemStack clickedItem = slot.getStack();
                            ItemStack cursor = playerEntity.inventory.getItemStack();

                            if (clickedItem == null)
                            {
                                if (cursor != null)
                                {
                                    action = par1Packet102WindowClick.mouseClick == 0 ? InventoryAction.PLACE_ALL : InventoryAction.PLACE_ONE;
                                }
                            }
                            else if (slot.canTakeStack(playerEntity))     // Should be Slot.isPlayerAllowed
                            {
                                if (cursor == null)
                                {
                                    action = par1Packet102WindowClick.mouseClick == 0 ? InventoryAction.PICKUP_ALL : InventoryAction.PICKUP_HALF;
                                }
                                else if (slot.isItemValid(cursor))     // Should be Slot.isItemAllowed
                                {
                                    if (clickedItem.isItemEqual(cursor) && ItemStack.areItemStackTagsEqual(clickedItem, cursor))
                                    {
                                        int toPlace = par1Packet102WindowClick.mouseClick == 0 ? cursor.stackSize : 1;
                                        toPlace = Math.min(toPlace, clickedItem.getMaxStackSize() - clickedItem.stackSize);
                                        toPlace = Math.min(toPlace, slot.inventory.getInventoryStackLimit() - clickedItem.stackSize);

                                        if (toPlace == 1)
                                        {
                                            action = InventoryAction.PLACE_ONE;
                                        }
                                        else if (toPlace == cursor.stackSize)
                                        {
                                            action = InventoryAction.PLACE_ALL;
                                        }
                                        else if (toPlace < 0)
                                        {
                                            action = toPlace != -1 ? InventoryAction.PICKUP_SOME : InventoryAction.PICKUP_ONE; // this happens with oversized stacks
                                        }
                                        else if (toPlace != 0)
                                        {
                                            action = InventoryAction.PLACE_SOME;
                                        }
                                    }
                                    else if (cursor.stackSize <= slot.getSlotStackLimit())     // Should be Slot.getMaxStackSize()
                                    {
                                        action = InventoryAction.SWAP_WITH_CURSOR;
                                    }
                                }
                                else if (cursor.itemID == clickedItem.itemID && (!cursor.getHasSubtypes() || cursor.getItemDamage() == clickedItem.getItemDamage()) && ItemStack.areItemStackTagsEqual(cursor, clickedItem))
                                {
                                    if (clickedItem.stackSize >= 0)
                                    {
                                        if (clickedItem.stackSize + cursor.stackSize <= cursor.getMaxStackSize())
                                        {
                                            // As of 1.5, this is result slots only
                                            action = InventoryAction.PICKUP_ALL;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (par1Packet102WindowClick.holdingShift == 1)
            {
                if (par1Packet102WindowClick.mouseClick == 0)
                {
                    click = ClickType.SHIFT_LEFT;
                }
                else if (par1Packet102WindowClick.mouseClick == 1)
                {
                    click = ClickType.SHIFT_RIGHT;
                }

                if (par1Packet102WindowClick.mouseClick == 0 || par1Packet102WindowClick.mouseClick == 1)
                {
                    if (par1Packet102WindowClick.inventorySlot < 0)
                    {
                        action = InventoryAction.NOTHING;
                    }
                    else
                    {
                        Slot slot = this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot);

                        if (slot != null && slot.canTakeStack(this.playerEntity) && slot.getHasStack())   // Should be Slot.hasItem()
                        {
                            action = InventoryAction.MOVE_TO_OTHER_INVENTORY;
                        }
                        else
                        {
                            action = InventoryAction.NOTHING;
                        }
                    }
                }
            }
            else if (par1Packet102WindowClick.holdingShift == 2)
            {
                if (par1Packet102WindowClick.mouseClick >= 0 && par1Packet102WindowClick.mouseClick < 9)
                {
                    click = ClickType.NUMBER_KEY;
                    Slot clickedSlot = this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot);

                    if (clickedSlot.canTakeStack(playerEntity))
                    {
                        ItemStack hotbar = this.playerEntity.inventory.getStackInSlot(par1Packet102WindowClick.mouseClick);
                        boolean canCleanSwap = hotbar == null || (clickedSlot.inventory == playerEntity.inventory && clickedSlot.isItemValid(hotbar)); // the slot will accept the hotbar item

                        if (clickedSlot.getHasStack())
                        {
                            if (canCleanSwap)
                            {
                                action = InventoryAction.HOTBAR_SWAP;
                            }
                            else
                            {
                                int firstEmptySlot = playerEntity.inventory.getFirstEmptyStack(); // Should be Inventory.firstEmpty()

                                if (firstEmptySlot > -1)
                                {
                                    action = InventoryAction.HOTBAR_MOVE_AND_READD;
                                }
                                else
                                {
                                    action = InventoryAction.NOTHING; // This is not sane! Mojang: You should test for other slots of same type
                                }
                            }
                        }
                        else if (!clickedSlot.getHasStack() && hotbar != null && clickedSlot.isItemValid(hotbar))
                        {
                            action = InventoryAction.HOTBAR_SWAP;
                        }
                        else
                        {
                            action = InventoryAction.NOTHING;
                        }
                    }
                    else
                    {
                        action = InventoryAction.NOTHING;
                    }

                    // Special constructor for number key
                    event = new InventoryClickEvent(inventory, type, par1Packet102WindowClick.inventorySlot, click, action, par1Packet102WindowClick.mouseClick);
                }
            }
            else if (par1Packet102WindowClick.holdingShift == 3)
            {
                if (par1Packet102WindowClick.mouseClick == 2)
                {
                    click = ClickType.MIDDLE;

                    if (par1Packet102WindowClick.inventorySlot == -999)
                    {
                        action = InventoryAction.NOTHING;
                    }
                    else
                    {
                        Slot slot = this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot);

                        if (slot != null && slot.getHasStack() && playerEntity.capabilities.isCreativeMode && playerEntity.inventory.getItemStack() == null)
                        {
                            action = InventoryAction.CLONE_STACK;
                        }
                        else
                        {
                            action = InventoryAction.NOTHING;
                        }
                    }
                }
                else
                {
                    click = ClickType.UNKNOWN;
                    action = InventoryAction.UNKNOWN;
                }
            }
            else if (par1Packet102WindowClick.holdingShift == 4)
            {
                if (par1Packet102WindowClick.inventorySlot >= 0)
                {
                    if (par1Packet102WindowClick.mouseClick == 0)
                    {
                        click = ClickType.DROP;
                        Slot slot = this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot);

                        if (slot != null && slot.getHasStack() && slot.canTakeStack(playerEntity) && slot.getStack() != null && slot.getStack().itemID != 0)
                        {
                            action = InventoryAction.DROP_ONE_SLOT;
                        }
                        else
                        {
                            action = InventoryAction.NOTHING;
                        }
                    }
                    else if (par1Packet102WindowClick.mouseClick == 1)
                    {
                        click = ClickType.CONTROL_DROP;
                        Slot slot = this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot);

                        if (slot != null && slot.getHasStack() && slot.canTakeStack(playerEntity) && slot.getStack() != null && slot.getStack().itemID != 0)
                        {
                            action = InventoryAction.DROP_ALL_SLOT;
                        }
                        else
                        {
                            action = InventoryAction.NOTHING;
                        }
                    }
                }
                else
                {
                    // Sane default (because this happens when they are holding nothing. Don't ask why.)
                    click = ClickType.LEFT;

                    if (par1Packet102WindowClick.mouseClick == 1)
                    {
                        click = ClickType.RIGHT;
                    }

                    action = InventoryAction.NOTHING;
                }
            }
            else if (par1Packet102WindowClick.holdingShift == 5)
            {
                itemstack = this.playerEntity.openContainer.slotClick(par1Packet102WindowClick.inventorySlot, par1Packet102WindowClick.mouseClick, 5, this.playerEntity);
            }
            else if (par1Packet102WindowClick.holdingShift == 6)
            {
                click = ClickType.DOUBLE_CLICK;
                action = InventoryAction.NOTHING;

                if (par1Packet102WindowClick.inventorySlot >= 0 && this.playerEntity.inventory.getItemStack() != null)
                {
                    ItemStack cursor = this.playerEntity.inventory.getItemStack();
                    action = InventoryAction.NOTHING;

                    // Quick check for if we have any of the item
                    // Cauldron start - can't call getContents() on modded IInventory; CB-added method
                    try {
                    if (inventory.getTopInventory().contains(cursor.itemID) || inventory.getBottomInventory().contains(cursor.itemID))
                    {
                        action = InventoryAction.COLLECT_TO_CURSOR;
                    }
                    } catch (AbstractMethodError ex) {
                        // nothing we can do
                    }
                    // Cauldron end
                }
            }

            // TODO check on updates

            if (par1Packet102WindowClick.holdingShift != 5)
            {
                if (click == ClickType.NUMBER_KEY)
                {
                    event = new InventoryClickEvent(inventory, type, par1Packet102WindowClick.inventorySlot, click, action, par1Packet102WindowClick.mouseClick);
                }
                else
                {
                    event = new InventoryClickEvent(inventory, type, par1Packet102WindowClick.inventorySlot, click, action);
                }

                org.bukkit.inventory.Inventory top = inventory.getTopInventory();

                if (par1Packet102WindowClick.inventorySlot == 0 && top instanceof CraftingInventory)
                {
                    // Cauldron start - vanilla compatibility (mod recipes)
                    org.bukkit.inventory.Recipe recipe = null;
                    try
                    {
                        recipe = ((CraftingInventory) top).getRecipe();
                    }
                    catch (AbstractMethodError e)
                    {
                        // do nothing
                    }
                    // Cauldron end

                    if (recipe != null)
                    {
                        if (click == ClickType.NUMBER_KEY)
                        {
                            event = new CraftItemEvent(recipe, inventory, type, par1Packet102WindowClick.inventorySlot, click, action, par1Packet102WindowClick.mouseClick);
                        }
                        else
                        {
                            event = new CraftItemEvent(recipe, inventory, type, par1Packet102WindowClick.inventorySlot, click, action);
                        }
                    }
                }

                server.getPluginManager().callEvent(event);

                switch (event.getResult())
                {
                    case ALLOW:
                    case DEFAULT:
                        itemstack = this.playerEntity.openContainer.slotClick(par1Packet102WindowClick.inventorySlot, par1Packet102WindowClick.mouseClick, par1Packet102WindowClick.holdingShift, this.playerEntity);
                        break;
                    case DENY:
                        /* Needs enum constructor in InventoryAction
                        if (action.modifiesOtherSlots()) {
                        } else {
                            if (action.modifiesCursor()) {
                                this.player.playerConnection.sendPacket(new Packet103SetSlot(-1, -1, this.player.inventory.getCarried()));
                            }
                            if (action.modifiesClicked()) {
                                this.player.playerConnection.sendPacket(new Packet103SetSlot(this.player.activeContainer.windowId, packet102windowclick.slot, this.player.activeContainer.getSlot(packet102windowclick.slot).getItem()));
                            }
                        }*/
                        switch (action)
                        {
                                // Modified other slots
                            case PICKUP_ALL:
                            case MOVE_TO_OTHER_INVENTORY:
                            case HOTBAR_MOVE_AND_READD:
                            case HOTBAR_SWAP:
                            case COLLECT_TO_CURSOR:
                            case UNKNOWN:
                                this.playerEntity.sendContainerToPlayer(this.playerEntity.openContainer);
                                break;

                                // Modified cursor and clicked
                            case PICKUP_SOME:
                            case PICKUP_HALF:
                            case PICKUP_ONE:
                            case PLACE_ALL:
                            case PLACE_SOME:
                            case PLACE_ONE:
                            case SWAP_WITH_CURSOR:
                                this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(-1, -1, this.playerEntity.inventory.getItemStack()));
                                this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(this.playerEntity.openContainer.windowId, par1Packet102WindowClick.inventorySlot, this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot).getStack()));
                                break;

                                // Modified clicked only
                            case DROP_ALL_SLOT:
                            case DROP_ONE_SLOT:
                                this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(this.playerEntity.openContainer.windowId, par1Packet102WindowClick.inventorySlot, this.playerEntity.openContainer.getSlot(par1Packet102WindowClick.inventorySlot).getStack()));
                                break;

                                // Modified cursor only
                            case DROP_ALL_CURSOR:
                            case DROP_ONE_CURSOR:
                            case CLONE_STACK:
                                this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(-1, -1, this.playerEntity.inventory.getItemStack()));
                                break;

                                // Nothing
                            case NOTHING:
                                break;
                        }

                        return;
                }
            }

            // CraftBukkit end

            if (ItemStack.areItemStacksEqual(par1Packet102WindowClick.itemStack, itemstack))
            {
                this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet106Transaction(par1Packet102WindowClick.window_Id, par1Packet102WindowClick.action, true));
                this.playerEntity.playerInventoryBeingManipulated = true;
                this.playerEntity.openContainer.detectAndSendChanges();
                this.playerEntity.updateHeldItem();
                this.playerEntity.playerInventoryBeingManipulated = false;
            }
            else
            {
                this.field_72586_s.addKey(this.playerEntity.openContainer.windowId, Short.valueOf(par1Packet102WindowClick.action));
                this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet106Transaction(par1Packet102WindowClick.window_Id, par1Packet102WindowClick.action, false));
                this.playerEntity.openContainer.setPlayerIsPresent(this.playerEntity, false);
                ArrayList arraylist = new ArrayList();

                for (int i = 0; i < this.playerEntity.openContainer.inventorySlots.size(); ++i)
                {
                    arraylist.add(((Slot)this.playerEntity.openContainer.inventorySlots.get(i)).getStack());
                }

                this.playerEntity.sendContainerAndContentsToPlayer(this.playerEntity.openContainer, arraylist);

                // CraftBukkit start - Send a Set Slot to update the crafting result slot
                if (type == SlotType.RESULT && itemstack != null)
                {
                    this.playerEntity.playerNetServerHandler.sendPacketToPlayer((Packet)(new Packet103SetSlot(this.playerEntity.openContainer.windowId, 0, itemstack)));
                }

                // CraftBukkit end
            }
        }
    }

    public void handleEnchantItem(Packet108EnchantItem par1Packet108EnchantItem)
    {
        this.playerEntity.func_143004_u();

        if (this.playerEntity.openContainer.windowId == par1Packet108EnchantItem.windowId && this.playerEntity.openContainer.isPlayerNotUsingContainer(this.playerEntity))
        {
            this.playerEntity.openContainer.enchantItem(this.playerEntity, par1Packet108EnchantItem.enchantment);
            this.playerEntity.openContainer.detectAndSendChanges();
        }
    }

    /**
     * Handle a creative slot packet.
     */
    public void handleCreativeSetSlot(Packet107CreativeSetSlot par1Packet107CreativeSetSlot)
    {
        if (this.playerEntity.theItemInWorldManager.isCreative())
        {
            boolean flag = par1Packet107CreativeSetSlot.slot < 0;
            ItemStack itemstack = par1Packet107CreativeSetSlot.itemStack;
            boolean flag1 = par1Packet107CreativeSetSlot.slot >= 1 && par1Packet107CreativeSetSlot.slot < 36 + InventoryPlayer.getHotbarSize();
            // CraftBukkit
            boolean flag2 = itemstack == null || itemstack.itemID < Item.itemsList.length && itemstack.itemID >= 0 && Item.itemsList[itemstack.itemID] != null && !invalidItems.contains(itemstack.itemID);
            boolean flag3 = itemstack == null || itemstack.getItemDamage() >= 0 && itemstack.getItemDamage() >= 0 && itemstack.stackSize <= 64 && itemstack.stackSize > 0;

            // CraftBukkit start - Call click event
            if (flag || (flag1 && !ItemStack.areItemStacksEqual(this.playerEntity.inventoryContainer.getSlot(par1Packet107CreativeSetSlot.slot).getStack(), par1Packet107CreativeSetSlot.itemStack)))   // Insist on valid slot
            {
                org.bukkit.entity.HumanEntity player = this.playerEntity.getBukkitEntity();
                InventoryView inventory = new CraftInventoryView(player, player.getInventory(), this.playerEntity.inventoryContainer);
                org.bukkit.inventory.ItemStack item = CraftItemStack.asBukkitCopy(par1Packet107CreativeSetSlot.itemStack); // Should be packet107setcreativeslot.newitem
                SlotType type = SlotType.QUICKBAR;

                if (flag)
                {
                    type = SlotType.OUTSIDE;
                }
                else if (par1Packet107CreativeSetSlot.slot < 36)
                {
                    if (par1Packet107CreativeSetSlot.slot >= 5 && par1Packet107CreativeSetSlot.slot < 9)
                    {
                        type = SlotType.ARMOR;
                    }
                    else
                    {
                        type = SlotType.CONTAINER;
                    }
                }

                InventoryCreativeEvent event = new InventoryCreativeEvent(inventory, type, flag ? -999 : par1Packet107CreativeSetSlot.slot, item);
                server.getPluginManager().callEvent(event);
                itemstack = CraftItemStack.asNMSCopy(event.getCursor());

                switch (event.getResult())
                {
                    case ALLOW:
                        // Plugin cleared the id / stacksize checks
                        flag2 = flag3 = true;
                        break;
                    case DEFAULT:
                        break;
                    case DENY:
                        // Reset the slot
                        if (par1Packet107CreativeSetSlot.slot >= 0)
                        {
                            this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(this.playerEntity.inventoryContainer.windowId, par1Packet107CreativeSetSlot.slot, this.playerEntity.inventoryContainer.getSlot(par1Packet107CreativeSetSlot.slot).getStack()));
                            this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(-1, -1, null));
                        }

                        return;
                }
            }

            // CraftBukkit end

            if (flag1 && flag2 && flag3)
            {
                if (itemstack == null)
                {
                    this.playerEntity.inventoryContainer.putStackInSlot(par1Packet107CreativeSetSlot.slot, (ItemStack)null);
                }
                else
                {
                    this.playerEntity.inventoryContainer.putStackInSlot(par1Packet107CreativeSetSlot.slot, itemstack);
                }

                this.playerEntity.inventoryContainer.setPlayerIsPresent(this.playerEntity, true);
            }
            else if (flag && flag2 && flag3 && this.creativeItemCreationSpamThresholdTally < 200)
            {
                this.creativeItemCreationSpamThresholdTally += 20;
                EntityItem entityitem = this.playerEntity.dropPlayerItem(itemstack);

                if (entityitem != null)
                {
                    entityitem.setAgeToCreativeDespawnTime();
                }
            }
        }
    }

    public void handleTransaction(Packet106Transaction par1Packet106Transaction)
    {
        if (this.playerEntity.isDead)
        {
            return;    // CraftBukkit
        }

        Short oshort = (Short)this.field_72586_s.lookup(this.playerEntity.openContainer.windowId);

        if (oshort != null && par1Packet106Transaction.shortWindowId == oshort.shortValue() && this.playerEntity.openContainer.windowId == par1Packet106Transaction.windowId && !this.playerEntity.openContainer.isPlayerNotUsingContainer(this.playerEntity))
        {
            this.playerEntity.openContainer.setPlayerIsPresent(this.playerEntity, true);
        }
    }

    /**
     * Updates Client side signs
     */
    public void handleUpdateSign(Packet130UpdateSign par1Packet130UpdateSign)
    {
        if (this.playerEntity.isDead)
        {
            return;    // CraftBukkit
        }

        this.playerEntity.func_143004_u();
        WorldServer worldserver = this.mcServer.worldServerForDimension(this.playerEntity.dimension);

        if (worldserver.blockExists(par1Packet130UpdateSign.xPosition, par1Packet130UpdateSign.yPosition, par1Packet130UpdateSign.zPosition))
        {
            TileEntity tileentity = worldserver.getBlockTileEntity(par1Packet130UpdateSign.xPosition, par1Packet130UpdateSign.yPosition, par1Packet130UpdateSign.zPosition);

            if (tileentity instanceof TileEntitySign)
            {
                TileEntitySign tileentitysign = (TileEntitySign)tileentity;

                if (!tileentitysign.isEditable() || tileentitysign.func_142009_b() != this.playerEntity)
                {
                    this.mcServer.logWarning("Player " + this.playerEntity.getCommandSenderName() + " just tried to change non-editable sign");
                    this.sendPacketToPlayer(new Packet130UpdateSign(par1Packet130UpdateSign.xPosition, par1Packet130UpdateSign.yPosition, par1Packet130UpdateSign.zPosition, tileentitysign.signText)); // CraftBukkit
                    return;
                }
            }

            int i;
            int j;

            for (j = 0; j < 4; ++j)
            {
                boolean flag = true;

                if (par1Packet130UpdateSign.signLines[j].length() > 15)
                {
                    flag = false;
                }
                else
                {
                    for (i = 0; i < par1Packet130UpdateSign.signLines[j].length(); ++i)
                    {
                        if (!ChatAllowedCharacters.isAllowedCharacter(par1Packet130UpdateSign.signLines[j].charAt(i)))   // Spigot
                        {
                            flag = false;
                        }
                    }
                }

                if (!flag)
                {
                    par1Packet130UpdateSign.signLines[j] = "!?";
                }
            }

            if (tileentity instanceof TileEntitySign)
            {
                j = par1Packet130UpdateSign.xPosition;
                int k = par1Packet130UpdateSign.yPosition;
                i = par1Packet130UpdateSign.zPosition;
                TileEntitySign tileentitysign1 = (TileEntitySign)tileentity;
                // CraftBukkit start
                Player player = this.server.getPlayer(this.playerEntity);
                SignChangeEvent event = new SignChangeEvent((org.bukkit.craftbukkit.block.CraftBlock) player.getWorld().getBlockAt(j, k, i), this.server.getPlayer(this.playerEntity), par1Packet130UpdateSign.signLines);
                this.server.getPluginManager().callEvent(event);

                if (!event.isCancelled())
                {
                    for (int l = 0; l < 4; ++l)
                    {
                        tileentitysign1.signText[l] = event.getLine(l);

                        if (tileentitysign1.signText[l] == null)
                        {
                            tileentitysign1.signText[l] = "";
                        }
                    }

                    tileentitysign1.isEditable = false;
                }

                // CraftBukkit end
                tileentitysign1.onInventoryChanged();
                worldserver.markBlockForUpdate(j, k, i);
            }
        }
    }

    /**
     * Handle a keep alive packet.
     */
    public void handleKeepAlive(Packet0KeepAlive par1Packet0KeepAlive)
    {
        if (par1Packet0KeepAlive.randomId == this.keepAliveRandomID)
        {
            int i = (int)(System.nanoTime() / 1000000L - this.keepAliveTimeSent);
            this.playerEntity.ping = (this.playerEntity.ping * 3 + i) / 4;
        }
    }

    /**
     * determine if it is a server handler
     */
    public boolean isServerHandler()
    {
        return true;
    }

    /**
     * Handle a player abilities packet.
     */
    public void handlePlayerAbilities(Packet202PlayerAbilities par1Packet202PlayerAbilities)
    {
        // CraftBukkit start
        if (this.playerEntity.capabilities.allowFlying && this.playerEntity.capabilities.isFlying != par1Packet202PlayerAbilities.getFlying())
        {
            PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(this.server.getPlayer(this.playerEntity), par1Packet202PlayerAbilities.getFlying());
            this.server.getPluginManager().callEvent(event);

            if (!event.isCancelled())
            {
                this.playerEntity.capabilities.isFlying = par1Packet202PlayerAbilities.getFlying(); // Actually set the player's flying status
            }
            else
            {
                this.playerEntity.sendPlayerAbilities(); // Tell the player their ability was reverted
            }
        }

        // CraftBukkit end
    }

    public void handleAutoComplete(Packet203AutoComplete par1Packet203AutoComplete)
    {
        StringBuilder stringbuilder = new StringBuilder();
        String s;

        for (Iterator iterator = this.mcServer.getPossibleCompletions(this.playerEntity, par1Packet203AutoComplete.getText()).iterator(); iterator.hasNext(); stringbuilder.append(s))
        {
            s = (String)iterator.next();

            if (stringbuilder.length() > 0)
            {
                stringbuilder.append('\0'); // CraftBukkit - fix decompile issue
            }
        }

        this.playerEntity.playerNetServerHandler.sendPacketToPlayer(new Packet203AutoComplete(stringbuilder.toString()));
    }

    public void handleClientInfo(Packet204ClientInfo par1Packet204ClientInfo)
    {
        this.playerEntity.updateClientInfo(par1Packet204ClientInfo);
    }

    public void handleCustomPayload(Packet250CustomPayload par1Packet250CustomPayload)
    {
        FMLNetworkHandler.handlePacket250Packet(par1Packet250CustomPayload, netManager, this);
    }

    public void handleVanilla250Packet(Packet250CustomPayload par1Packet250CustomPayload)
    {
        DataInputStream datainputstream;
        ItemStack itemstack;
        ItemStack itemstack1;

        // CraftBukkit start - Ignore empty payloads
        if (par1Packet250CustomPayload.length <= 0)
        {
            return;
        }

        // CraftBukkit end

        if ("MC|BEdit".equals(par1Packet250CustomPayload.channel))
        {
            try
            {
                datainputstream = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                itemstack = Packet.readItemStack(datainputstream);

                if (!ItemWritableBook.validBookTagPages(itemstack.getTagCompound()))
                {
                    throw new IOException("Invalid book tag!");
                }

                itemstack1 = this.playerEntity.inventory.getCurrentItem();

                if (itemstack != null && itemstack.itemID == Item.writableBook.itemID && itemstack.itemID == itemstack1.itemID)
                {
                    CraftEventFactory.handleEditBookEvent(playerEntity, itemstack); // CraftBukkit
                }

                // CraftBukkit start
            }
            catch (Throwable exception)
            {
                this.mcServer.getLogAgent().logWarningException(this.playerEntity.getCommandSenderName() + " sent invalid MC|BEdit data", exception);
                this.kickPlayerFromServer("Invalid book data!");
                // CraftBukkit end
            }
        }
        else if ("MC|BSign".equals(par1Packet250CustomPayload.channel))
        {
            try
            {
                datainputstream = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                itemstack = Packet.readItemStack(datainputstream);

                if (!ItemEditableBook.validBookTagContents(itemstack.getTagCompound()))
                {
                    throw new IOException("Invalid book tag!");
                }

                itemstack1 = this.playerEntity.inventory.getCurrentItem();

                if (itemstack != null && itemstack.itemID == Item.writtenBook.itemID && itemstack1.itemID == Item.writableBook.itemID)
                {
                    CraftEventFactory.handleEditBookEvent(playerEntity, itemstack); // CraftBukkit
                }

                // CraftBukkit start
            }
            catch (Throwable exception1)
            {
                this.mcServer.getLogAgent().logWarningException(this.playerEntity.getCommandSenderName() + " sent invalid MC|BSign data", exception1);
                this.kickPlayerFromServer("Invalid book data!");
                // CraftBukkit end
            }
        }
        else
        {
            int i;

            if ("MC|TrSel".equals(par1Packet250CustomPayload.channel))
            {
                try
                {
                    datainputstream = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                    i = datainputstream.readInt();
                    Container container = this.playerEntity.openContainer;

                    if (container instanceof ContainerMerchant)
                    {
                        ((ContainerMerchant)container).setCurrentRecipeIndex(i);
                    }
                }
                catch (Exception exception2)
                {
                    // CraftBukkit start
                    this.mcServer.getLogAgent().logWarningException(this.playerEntity.getCommandSenderName() + " sent invalid MC|TrSel data", exception2);
                    this.kickPlayerFromServer("Invalid trade data!");
                    // CraftBukkit end
                }
            }
            else
            {
                int j;

                if ("MC|AdvCdm".equals(par1Packet250CustomPayload.channel))
                {
                    if (!this.mcServer.isCommandBlockEnabled())
                    {
                        this.playerEntity.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("advMode.notEnabled"));
                    }
                    else if (this.playerEntity.canCommandSenderUseCommand(2, "") && this.playerEntity.capabilities.isCreativeMode)
                    {
                        try
                        {
                            datainputstream = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                            i = datainputstream.readInt();
                            j = datainputstream.readInt();
                            int k = datainputstream.readInt();
                            String s = Packet.readString(datainputstream, 256);
                            TileEntity tileentity = this.playerEntity.worldObj.getBlockTileEntity(i, j, k);

                            if (tileentity != null && tileentity instanceof TileEntityCommandBlock)
                            {
                                ((TileEntityCommandBlock)tileentity).setCommand(s);
                                this.playerEntity.worldObj.markBlockForUpdate(i, j, k);
                                this.playerEntity.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions("advMode.setCommand.success", new Object[] {s}));
                            }
                        }
                        catch (Exception exception3)
                        {
                            // CraftBukkit start
                            this.mcServer.getLogAgent().logWarningException(this.playerEntity.getCommandSenderName() + " sent invalid MC|AdvCdm data", exception3);
                            this.kickPlayerFromServer("Invalid CommandBlock data!");
                            // CraftBukkit end
                        }
                    }
                    else
                    {
                        this.playerEntity.sendChatToPlayer(ChatMessageComponent.createFromTranslationKey("advMode.notAllowed"));
                    }
                }
                else if ("MC|Beacon".equals(par1Packet250CustomPayload.channel))
                {
                    if (this.playerEntity.openContainer instanceof ContainerBeacon)
                    {
                        try
                        {
                            datainputstream = new DataInputStream(new ByteArrayInputStream(par1Packet250CustomPayload.data));
                            i = datainputstream.readInt();
                            j = datainputstream.readInt();
                            ContainerBeacon containerbeacon = (ContainerBeacon)this.playerEntity.openContainer;
                            Slot slot = containerbeacon.getSlot(0);

                            if (slot.getHasStack())
                            {
                                slot.decrStackSize(1);
                                TileEntityBeacon tileentitybeacon = containerbeacon.getBeacon();
                                tileentitybeacon.setPrimaryEffect(i);
                                tileentitybeacon.setSecondaryEffect(j);
                                tileentitybeacon.onInventoryChanged();
                            }
                        }
                        catch (Exception exception4)
                        {
                            // CraftBukkit start
                            this.mcServer.getLogAgent().logWarningException(this.playerEntity.getCommandSenderName() + " sent invalid MC|Beacon data", exception4);
                            this.kickPlayerFromServer("Invalid beacon data!");
                            // CraftBukkit end
                        }
                    }
                }
                else if ("MC|ItemName".equals(par1Packet250CustomPayload.channel) && this.playerEntity.openContainer instanceof ContainerRepair)
                {
                    ContainerRepair containerrepair = (ContainerRepair)this.playerEntity.openContainer;

                    if (par1Packet250CustomPayload.data != null && par1Packet250CustomPayload.data.length >= 1)
                    {
                        String s1 = ChatAllowedCharacters.filerAllowedCharacters(new String(par1Packet250CustomPayload.data));

                        if (s1.length() <= 30)
                        {
                            containerrepair.updateItemName(s1);
                        }
                    }
                    else
                    {
                        containerrepair.updateItemName("");
                    }
                }
                // CraftBukkit start // Cauldron - move REGISTER/UNREGISTER handling to FML
                else
                {
                    server.getMessenger().dispatchIncomingMessage(playerEntity.getBukkitEntity(), par1Packet250CustomPayload.channel, par1Packet250CustomPayload.data);
                }

                // CraftBukkit end
            }
        }
    }

    public boolean isConnectionClosed()
    {
        return this.connectionClosed;
    }


    @Override

    /**
     * Contains logic for handling packets containing arbitrary unique item data. Currently this is only for maps.
     */
    public void handleMapData(Packet131MapData par1Packet131MapData)
    {
        FMLNetworkHandler.handlePacket131Packet(this, par1Packet131MapData);
    }

    // modloader compat -- yuk!
    @Override
    public EntityPlayerMP getPlayer()
    {
        return playerEntity;
    }
}