package net.minecraft.entity.player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.item.EntityMinecartHopper;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBeacon;
import net.minecraft.inventory.ContainerBrewingStand;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.ContainerHorseInventory;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryMerchant;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.item.ItemMapBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetServerHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet100OpenWindow;
import net.minecraft.network.packet.Packet101CloseWindow;
import net.minecraft.network.packet.Packet103SetSlot;
import net.minecraft.network.packet.Packet104WindowItems;
import net.minecraft.network.packet.Packet105UpdateProgressbar;
import net.minecraft.network.packet.Packet133TileEditorOpen;
import net.minecraft.network.packet.Packet17Sleep;
import net.minecraft.network.packet.Packet18Animation;
import net.minecraft.network.packet.Packet200Statistic;
import net.minecraft.network.packet.Packet202PlayerAbilities;
import net.minecraft.network.packet.Packet204ClientInfo;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet29DestroyEntity;
import net.minecraft.network.packet.Packet38EntityStatus;
import net.minecraft.network.packet.Packet39AttachEntity;
import net.minecraft.network.packet.Packet3Chat;
import net.minecraft.network.packet.Packet41EntityEffect;
import net.minecraft.network.packet.Packet42RemoveEntityEffect;
import net.minecraft.network.packet.Packet43Experience;
import net.minecraft.network.packet.Packet56MapChunks;
import net.minecraft.network.packet.Packet70GameEvent;
import net.minecraft.network.packet.Packet8UpdateHealth;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScoreObjectiveCriteria;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityDropper;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import net.minecraft.entity.item.EntityItem;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
// CraftBukkit start
import net.minecraft.util.CombatTracker;
import net.minecraft.util.FoodStats;
import org.bukkit.Bukkit;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
// CraftBukkit end

public class EntityPlayerMP extends EntityPlayer implements ICrafting
{
    private String translator = "en_US";

    /**
     * The NetServerHandler assigned to this player by the ServerConfigurationManager.
     */
    public NetServerHandler playerNetServerHandler;

    /** Reference to the MinecraftServer object. */
    public MinecraftServer mcServer;

    /** The ItemInWorldManager belonging to this player */
    public ItemInWorldManager theItemInWorldManager;

    /** player X position as seen by PlayerManager */
    public double managedPosX;

    /** player Z position as seen by PlayerManager */
    public double managedPosZ;

    /** LinkedList that holds the loaded chunks. */
    public final List loadedChunks = new LinkedList();

    /** entities added to this list will  be packet29'd to the player */
    public final List destroyedItemsNetCache = new LinkedList();
    private float field_130068_bO = Float.MIN_VALUE;

    /** set to getHealth */
    private float lastHealth = -1.0E8F;

    /** set to foodStats.GetFoodLevel */
    private int lastFoodLevel = -99999999;

    /** set to foodStats.getSaturationLevel() == 0.0F each tick */
    private boolean wasHungry = true;

    /** Amount of experience the client was last set to */
    public int lastExperience = -99999999; // CraftBukkit - private -> public

    /** de-increments onUpdate, attackEntityFrom is ignored if this >0 */
    public int initialInvulnerability = 60; // CraftBukkit - private -> public

    /** must be between 3>x>15 (strictly between) */
    private int renderDistance;
    private int chatVisibility;
    private boolean chatColours = true;
    private long field_143005_bX = 0L;

    /**
     * The currently in use window ID. Incremented every time a window is opened.
     */
    public int currentWindowId;

    /**
     * poor mans concurency flag, lets hope the jvm doesn't re-order the setting of this flag wrt the inventory change
     * on the next line
     */
    public boolean playerInventoryBeingManipulated;
    public int ping;

    /**
     * Set when a player beats the ender dragon, used to respawn the player at the spawn point while retaining inventory
     * and XP
     */
    public boolean playerConqueredTheEnd;
    // CraftBukkit start
    public String cb_displayName;
    public String listName;
    public org.bukkit.Location compassTarget;
    public int newExp = 0;
    public int newLevel = 0;
    public int newTotalExp = 0;
    public boolean keepLevel = false;
    public double maxHealthCache;
    // CraftBukkit end
    // Spigot start
    public boolean collidesWithEntities = true;
    @Override
    public boolean canBeCollidedWith()
    {
        return this.collidesWithEntities && super.canBeCollidedWith();
    }
    // Spigot end

    public EntityPlayerMP(MinecraftServer par1MinecraftServer, World par2World, String par3Str, ItemInWorldManager par4ItemInWorldManager)
    {
        super(par2World, par3Str);
        par4ItemInWorldManager.thisPlayerMP = this;
        this.theItemInWorldManager = par4ItemInWorldManager;
        this.renderDistance = par1MinecraftServer.getConfigurationManager().getViewDistance();
        ChunkCoordinates chunkcoordinates = par2World.provider.getRandomizedSpawnPoint();
        if (par1MinecraftServer == null) //ToDo: Remove this in 1.7, Fake players shouldn't be used purely client side.
            this.renderDistance = 0;
        else
            this.renderDistance = par1MinecraftServer.getConfigurationManager().getViewDistance();
        int i = chunkcoordinates.posX;
        int j = chunkcoordinates.posZ;
        int k = chunkcoordinates.posY;

        if (!par2World.provider.hasNoSky && par2World.getWorldInfo().getGameType() != EnumGameType.ADVENTURE)
        {
            int var9 = Math.max(5, par1MinecraftServer.getSpawnProtectionSize() - 6);
            i += this.rand.nextInt(var9 * 2) - var9;
            j += this.rand.nextInt(var9 * 2) - var9;
            k = par2World.getTopSolidOrLiquidBlock(i, j);
        }

        this.mcServer = par1MinecraftServer;
        this.stepHeight = 0.0F;
        this.yOffset = 0.0F;
        this.setLocationAndAngles((double)i + 0.5D, (double)k, (double)j + 0.5D, 0.0F, 0.0F);

        while (!par2World.getCollidingBoundingBoxes(this, this.boundingBox).isEmpty())
        {
            this.setPosition(this.posX, this.posY + 1.0D, this.posZ);
        }
        // CraftBukkit start
        this.cb_displayName = this.getDisplayName();
        this.listName = this.username;
        // this.canPickUpLoot = true; TODO
        this.maxHealthCache = this.getMaxHealth();
        // CraftBukkit end
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readEntityFromNBT(par1NBTTagCompound);

        if (par1NBTTagCompound.hasKey("playerGameType"))
        {
            if (MinecraftServer.getServer().getForceGamemode())
            {
                this.theItemInWorldManager.setGameType(MinecraftServer.getServer().getGameType());
            }
            else
            {
                this.theItemInWorldManager.setGameType(EnumGameType.getByID(par1NBTTagCompound.getInteger("playerGameType")));
            }
        }
        this.getBukkitEntity().readExtraData(par1NBTTagCompound); // CraftBukkit
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeEntityToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setInteger("playerGameType", this.theItemInWorldManager.getGameType().getID());
        this.getBukkitEntity().setExtraData(par1NBTTagCompound); // CraftBukkit
    }

    // CraftBukkit start - World fallback code, either respawn location or global spawn

    /**
     * Sets the reference to the World object.
     */
    public void setWorld(World world)
    {
        super.setWorld(world);

        if (world == null)
        {
            this.isDead = false;
            ChunkCoordinates position = null;

            if (this.spawnWorld != null && !this.spawnWorld.equals(""))
            {
                CraftWorld cworld = (CraftWorld) Bukkit.getServer().getWorld(this.spawnWorld);

                if (cworld != null && this.getBedLocation() != null)
                {
                    world = cworld.getHandle();
                    position = EntityPlayer.verifyRespawnCoordinates(cworld.getHandle(), this.getBedLocation(), false);
                }
            }

            if (world == null || position == null)
            {
                world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
                position = world.getSpawnPoint();
            }

            this.worldObj = world;
            this.setPosition(position.posX + 0.5, position.posY, position.posZ + 0.5);
        }

        this.dimension = ((WorldServer) this.worldObj).provider.dimensionId;
        this.theItemInWorldManager.setWorld((WorldServer) world);
    }
    // CraftBukkit end

    /**
     * Add experience levels to this player.
     */
    public void addExperienceLevel(int par1)
    {
        super.addExperienceLevel(par1);
        this.lastExperience = -1;
    }

    public void addSelfToInternalCraftingInventory()
    {
        this.openContainer.addCraftingToCrafters(this);
    }

    /**
     * sets the players height back to normal after doing things like sleeping and dieing
     */
    protected void resetHeight()
    {
        this.yOffset = 0.0F;
    }

    /**
     * Called to update the entity's position/logic.
     */
    public void onUpdate()
    {
        this.theItemInWorldManager.updateBlockRemoving();
        --this.initialInvulnerability;
        this.openContainer.detectAndSendChanges();

        if (!this.worldObj.isRemote && !ForgeHooks.canInteractWith(this, this.openContainer))
        {
            this.closeScreen();
            this.openContainer = this.inventoryContainer;
        }

        // CraftBukkit start
        if (this.hurtResistantTime > 0)
        {
            --this.hurtResistantTime;
        }

        // CraftBukkit end

        while (!this.destroyedItemsNetCache.isEmpty())
        {
            int i = Math.min(this.destroyedItemsNetCache.size(), 127);
            int[] aint = new int[i];
            Iterator iterator = this.destroyedItemsNetCache.iterator();
            int j = 0;

            while (iterator.hasNext() && j < i)
            {
                aint[j++] = ((Integer)iterator.next()).intValue();
                iterator.remove();
            }

            this.playerNetServerHandler.sendPacketToPlayer(new Packet29DestroyEntity(aint));
        }

        if (!this.loadedChunks.isEmpty())
        {
            ArrayList arraylist = new ArrayList();
            Iterator iterator1 = this.loadedChunks.iterator();
            ArrayList arraylist1 = new ArrayList();

            while (iterator1.hasNext() && arraylist.size() < 5)
            {
                ChunkCoordIntPair chunkcoordintpair = (ChunkCoordIntPair)iterator1.next();
                iterator1.remove();

                if (chunkcoordintpair != null && this.worldObj.blockExists(chunkcoordintpair.chunkXPos << 4, 0, chunkcoordintpair.chunkZPos << 4))
                {
                    // CraftBukkit start - Get tile entities directly from the chunk instead of the world
                    Chunk chunk = this.worldObj.getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);
                    arraylist.add(chunk);
                    arraylist1.addAll(chunk.chunkTileEntityMap.values());
                    // CraftBukkit end
                }
            }

            if (!arraylist.isEmpty())
            {
                this.playerNetServerHandler.sendPacketToPlayer(new Packet56MapChunks(arraylist));
                Iterator iterator2 = arraylist1.iterator();

                while (iterator2.hasNext())
                {
                    TileEntity tileentity = (TileEntity)iterator2.next();
                    this.sendTileEntityToPlayer(tileentity);
                }

                iterator2 = arraylist.iterator();

                while (iterator2.hasNext())
                {
                    Chunk chunk = (Chunk)iterator2.next();
                    this.getServerForPlayer().getEntityTracker().func_85172_a(this, chunk);
                    MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(chunk.getChunkCoordIntPair(), this));
                }
            }
        }

        if (this.field_143005_bX > 0L && this.mcServer.func_143007_ar() > 0 && MinecraftServer.getSystemTimeMillis() - this.field_143005_bX > (long)(this.mcServer.func_143007_ar() * 1000 * 60))
        {
            this.playerNetServerHandler.kickPlayerFromServer("You have been idle for too long!");
        }
    }

    public void onUpdateEntity()
    {
        try
        {
            super.onUpdate();

            for (int i = 0; i < this.inventory.getSizeInventory(); ++i)
            {
                ItemStack itemstack = this.inventory.getStackInSlot(i);

                if (itemstack != null && Item.itemsList[itemstack.itemID].isMap() && this.playerNetServerHandler.packetSize() <= 5)
                {
                    Packet packet = ((ItemMapBase)Item.itemsList[itemstack.itemID]).createMapDataPacket(itemstack, this.worldObj, this);

                    if (packet != null)
                    {
                        this.playerNetServerHandler.sendPacketToPlayer(packet);
                    }
                }
            }

            if (this.getHealth() != this.lastHealth || this.lastFoodLevel != this.foodStats.getFoodLevel() || this.foodStats.getSaturationLevel() == 0.0F != this.wasHungry)
            {
                // CraftBukkit - Optionally scale health
                this.playerNetServerHandler.sendPacketToPlayer(new Packet8UpdateHealth(this.getBukkitEntity().getScaledHealth(), this.foodStats.getFoodLevel(), this.foodStats.getSaturationLevel()));
                this.lastHealth = this.getHealth();
                this.lastFoodLevel = this.foodStats.getFoodLevel();
                this.wasHungry = this.foodStats.getSaturationLevel() == 0.0F;
            }

            if (this.getHealth() + this.getAbsorptionAmount() != this.field_130068_bO)
            {
                this.field_130068_bO = this.getHealth() + this.getAbsorptionAmount();
                // CraftBukkit - Update ALL the scores!
                this.worldObj.getServer().getScoreboardManager().updateAllScoresForList(ScoreObjectiveCriteria.health, this.getEntityName(), com.google.common.collect.ImmutableList.of(this));
            }

            // CraftBukkit start - Force max health updates
            if (this.maxHealthCache != this.getMaxHealth())
            {
                this.getBukkitEntity().updateScaledHealth();
            }
            // CraftBukkit end

            if (this.experienceTotal != this.lastExperience)
            {
                this.lastExperience = this.experienceTotal;
                this.playerNetServerHandler.sendPacketToPlayer(new Packet43Experience(this.experience, this.experienceTotal, this.experienceLevel));
            }

            // CraftBukkit start
            if (this.oldLevel == -1)
            {
                this.oldLevel = this.experienceLevel;
            }

            if (this.oldLevel != this.experienceLevel)
            {
                CraftEventFactory.callPlayerLevelChangeEvent(this.worldObj.getServer().getPlayer((EntityPlayerMP) this), this.oldLevel, this.experienceLevel);
                this.oldLevel = this.experienceLevel;
            }

            // CraftBukkit end
        }
        catch (Throwable throwable)
        {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Ticking player");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Player being ticked");
            this.addEntityCrashInfo(crashreportcategory);
            throw new ReportedException(crashreport);
        }
    }

    /**
     * Called when the mob's health reaches 0.
     */
    public void onDeath(DamageSource par1DamageSource)
    {
        if (ForgeHooks.onLivingDeath(this, par1DamageSource)) return;
        // CraftBukkit start
        java.util.List<org.bukkit.inventory.ItemStack> loot = new java.util.ArrayList<org.bukkit.inventory.ItemStack>();
        boolean keepInventory = this.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory");

        if (!keepInventory)
        {
            for (int i = 0; i < this.inventory.mainInventory.length; ++i)
            {
                if (this.inventory.mainInventory[i] != null)
                {
                    loot.add(CraftItemStack.asCraftMirror(this.inventory.mainInventory[i]));
                }
            }

            for (int i = 0; i < this.inventory.armorInventory.length; ++i)
            {
                if (this.inventory.armorInventory[i] != null)
                {
                    loot.add(CraftItemStack.asCraftMirror(this.inventory.armorInventory[i]));
                }
            }
            captureDrops = true;
            capturedDrops.clear();
        }

        ChatMessageComponent chatmessagecomponent = this.func_110142_aN().func_94546_b();
        String deathmessage = chatmessagecomponent.toString();
        org.bukkit.event.entity.PlayerDeathEvent event = CraftEventFactory.callPlayerDeathEvent(this, loot, deathmessage);
        String deathMessage = event.getDeathMessage();

        if (deathMessage != null && deathMessage.length() > 0)
        {
            if (deathMessage.equals(chatmessagecomponent.toString()))
            {
                this.mcServer.getConfigurationManager().sendChatMsg(chatmessagecomponent);
            }
            else
            {
                this.mcServer.getConfigurationManager().sendChatMsg(ChatMessageComponent.createFromText(event.getDeathMessage()));
            }
        }

        // CraftBukkit - we clean the player's inventory after the EntityDeathEvent is called so plugins can get the exact state of the inventory.
        if (!keepInventory)
        {
            for (int i = 0; i < this.inventory.mainInventory.length; ++i)
            {
                this.inventory.mainInventory[i] = null;
            }

            for (int i = 0; i < this.inventory.armorInventory.length; ++i)
            {
                this.inventory.armorInventory[i] = null;
            }
            // Cauldron start
            captureDrops = false;
            PlayerDropsEvent forgeEvent = new PlayerDropsEvent(this, par1DamageSource, capturedDrops, recentlyHit > 0);

            if (!MinecraftForge.EVENT_BUS.post(forgeEvent))
            {
                for (EntityItem item : capturedDrops)
                {
                    joinEntityItemWithWorld(item);
                }
            }
            // Cauldron end
        }

        this.closeScreen();
        // CraftBukkit end
        // CraftBukkit - Get our scores instead
        Collection<Score> collection = this.worldObj.getServer().getScoreboardManager().getScoreboardScores(ScoreObjectiveCriteria.deathCount, this.getEntityName(), new java.util.ArrayList<Score>());
        Iterator iterator = collection.iterator();

        while (iterator.hasNext())
        {
            Score score = (Score) iterator.next(); // CraftBukkit - Use our scores instead
            score.func_96648_a();
        }

        EntityLivingBase entitylivingbase = this.func_94060_bK();

        if (entitylivingbase != null)
        {
            entitylivingbase.addToPlayerScore(this, this.scoreValue);
        }

        this.addStat(StatList.deathsStat, 1);
    }

    /**
     * Called when the entity is attacked.
     */
    public boolean attackEntityFrom(DamageSource par1DamageSource, float par2)
    {
        if (this.isEntityInvulnerable())
        {
            return false;
        }
        else
        {
            // CraftBukkit - this.server.getPvP() -> this.world.pvpMode
            boolean flag = this.mcServer.isDedicatedServer() && this.worldObj.pvpMode && "fall".equals(par1DamageSource.damageType);

            if (!flag && this.initialInvulnerability > 0 && par1DamageSource != DamageSource.outOfWorld)
            {
                return false;
            }
            else
            {
                if (par1DamageSource instanceof EntityDamageSource)
                {
                    Entity entity = par1DamageSource.getEntity();

                    if (entity instanceof EntityPlayer && !this.canAttackPlayer((EntityPlayer)entity))
                    {
                        return false;
                    }

                    if (entity instanceof EntityArrow)
                    {
                        EntityArrow entityarrow = (EntityArrow)entity;

                        if (entityarrow.shootingEntity instanceof EntityPlayer && !this.canAttackPlayer((EntityPlayer)entityarrow.shootingEntity))
                        {
                            return false;
                        }
                    }
                }

                return super.attackEntityFrom(par1DamageSource, par2);
            }
        }
    }

    public boolean canAttackPlayer(EntityPlayer par1EntityPlayer)
    {
        // CraftBukkit - this.server.getPvP() -> this.world.pvpMode
        return !this.worldObj.pvpMode ? false : super.canAttackPlayer(par1EntityPlayer);
    }

    /**
     * Teleports the entity to another dimension. Params: Dimension number to teleport to
     */
    public void travelToDimension(int par1)
    {
        if (this.dimension == 1 && par1 == 1)
        {
            this.triggerAchievement(AchievementList.theEnd2);
            this.worldObj.removeEntity(this);
            this.playerConqueredTheEnd = true;
            this.playerNetServerHandler.sendPacketToPlayer(new Packet70GameEvent(4, 0));
        }
        else
        {
            if (this.dimension == 0 && par1 == 1)
            {
                this.triggerAchievement(AchievementList.theEnd);
                // CraftBukkit start - Rely on custom portal management
                /*
                ChunkCoordinates chunkcoordinates = this.server.getWorldServer(i).getDimensionSpawn();

                if (chunkcoordinates != null) {
                    this.playerConnection.a((double) chunkcoordinates.x, (double) chunkcoordinates.y, (double) chunkcoordinates.z, 0.0F, 0.0F);
                }

                i = 1;
                */
                // CraftBukkit end
            }
            else
            {
                this.triggerAchievement(AchievementList.portal);
            }

            // CraftBukkit start
            TeleportCause cause = (this.dimension == 1 || par1 == 1) ? TeleportCause.END_PORTAL : TeleportCause.NETHER_PORTAL;
            this.mcServer.getConfigurationManager().changeDimension(this, par1, cause);
            // CraftBukkit end
            this.lastExperience = -1;
            this.lastHealth = -1.0F;
            this.lastFoodLevel = -1;
        }
    }

    /**
     * called from onUpdate for all tileEntity in specific chunks
     */
    private void sendTileEntityToPlayer(TileEntity par1TileEntity)
    {
        if (par1TileEntity != null)
        {
            Packet packet = par1TileEntity.getDescriptionPacket();

            if (packet != null)
            {
                this.playerNetServerHandler.sendPacketToPlayer(packet);
            }
        }
    }

    /**
     * Called whenever an item is picked up from walking over it. Args: pickedUpEntity, stackSize
     */
    public void onItemPickup(Entity par1Entity, int par2)
    {
        super.onItemPickup(par1Entity, par2);
        this.openContainer.detectAndSendChanges();
    }

    /**
     * Attempts to have the player sleep in a bed at the specified location.
     */
    public EnumStatus sleepInBedAt(int par1, int par2, int par3)
    {
        EnumStatus enumstatus = super.sleepInBedAt(par1, par2, par3);

        if (enumstatus == EnumStatus.OK)
        {
            Packet17Sleep packet17sleep = new Packet17Sleep(this, 0, par1, par2, par3);
            this.getServerForPlayer().getEntityTracker().sendPacketToAllPlayersTrackingEntity(this, packet17sleep);
            this.playerNetServerHandler.setPlayerLocation(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
            this.playerNetServerHandler.sendPacketToPlayer(packet17sleep);
        }

        return enumstatus;
    }

    /**
     * Wake up the player if they're sleeping.
     */
    public void wakeUpPlayer(boolean par1, boolean par2, boolean par3)
    {
        if (this.fauxSleeping && !this.sleeping)
        {
            return;    // CraftBukkit - Can't leave bed if not in one!
        }

        if (this.isPlayerSleeping())
        {
            this.getServerForPlayer().getEntityTracker().sendPacketToAllAssociatedPlayers(this, new Packet18Animation(this, 3));
        }

        super.wakeUpPlayer(par1, par2, par3);

        if (this.playerNetServerHandler != null)
        {
            this.playerNetServerHandler.setPlayerLocation(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
        }
    }

    /**
     * Called when a player mounts an entity. e.g. mounts a pig, mounts a boat.
     */
    public void mountEntity(Entity par1Entity)
    {
        // CraftBukkit start
        this.setPassengerOf(par1Entity);
    }

    public void setPassengerOf(Entity entity)
    {
        // mount(null) doesn't really fly for overloaded methods,
        // so this method is needed
        Entity currentVehicle = this.ridingEntity;
        super.setPassengerOf(entity);

        // Check if the vehicle actually changed.
        if (currentVehicle != this.ridingEntity)
        {
            this.playerNetServerHandler.sendPacketToPlayer(new Packet39AttachEntity(0, this, this.ridingEntity));
            this.playerNetServerHandler.setPlayerLocation(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
        }
        // CraftBukkit end
    }

    /**
     * Takes in the distance the entity has fallen this tick and whether its on the ground to update the fall distance
     * and deal fall damage if landing on the ground.  Args: distanceFallenThisTick, onGround
     */
    protected void updateFallState(double par1, boolean par3) {}

    /**
     * likeUpdateFallState, but called from updateFlyingState, rather than moveEntity
     */
    public void updateFlyingState(double par1, boolean par3)
    {
        super.updateFallState(par1, par3);
    }

    /**
     * Displays the GUI for editing a sign. Args: tileEntitySign
     */
    public void displayGUIEditSign(TileEntity par1TileEntity)
    {
        if (par1TileEntity instanceof TileEntitySign)
        {
            ((TileEntitySign)par1TileEntity).func_142010_a(this);
            this.playerNetServerHandler.sendPacketToPlayer(new Packet133TileEditorOpen(0, par1TileEntity.xCoord, par1TileEntity.yCoord, par1TileEntity.zCoord));
        }
    }

    // Cauldron add vanilla method back with correct signature. Fixes issue #3
    public void incrementWindowID()
    {
        this.currentWindowId = this.currentWindowId % 100 + 1;
    }

    // Cauldron CB-only method, used in CraftHumanEntity
    public int nextContainerCounter()   // CraftBukkit - private void -> public int
    {
        this.currentWindowId = this.currentWindowId % 100 + 1;
        return this.currentWindowId; // CraftBukkit
    }

    /**
     * Displays the crafting GUI for a workbench.
     */
    public void displayGUIWorkbench(int par1, int par2, int par3)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerWorkbench(this.inventory, this.worldObj, par1, par2, par3));

        if (container == null)
        {
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 1, "Crafting", 9, true));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    public void displayGUIEnchantment(int par1, int par2, int par3, String par4Str)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerEnchantment(this.inventory, this.worldObj, par1, par2, par3));

        if (container == null)
        {
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 4, par4Str == null ? "" : par4Str, 9, par4Str != null));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    /**
     * Displays the GUI for interacting with an anvil.
     */
    public void displayGUIAnvil(int par1, int par2, int par3)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerRepair(this.inventory, this.worldObj, par1, par2, par3, this));

        if (container == null)
        {
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 8, "Repairing", 9, true));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    /**
     * Displays the GUI for interacting with a chest inventory. Args: chestInventory
     */
    public void displayGUIChest(IInventory par1IInventory)
    {
        if (this.openContainer != this.inventoryContainer)
        {
            this.closeScreen();
        }

        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerChest(this.inventory, par1IInventory));

        if (container == null)
        {
            par1IInventory.closeChest(); // Cauldron - prevent chest from being stuck in open state on clients
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 0, par1IInventory.getInvName(), par1IInventory.getSizeInventory(), par1IInventory.isInvNameLocalized()));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    public void displayGUIHopper(TileEntityHopper par1TileEntityHopper)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerHopper(this.inventory, par1TileEntityHopper));

        if (container == null)
        {
            par1TileEntityHopper.closeChest(); // Cauldron - prevent chest from being stuck in open state on clients
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 9, par1TileEntityHopper.getInvName(), par1TileEntityHopper.getSizeInventory(), par1TileEntityHopper.isInvNameLocalized()));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    public void displayGUIHopperMinecart(EntityMinecartHopper par1EntityMinecartHopper)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerHopper(this.inventory, par1EntityMinecartHopper));

        if (container == null)
        {
            par1EntityMinecartHopper.closeChest(); // Cauldron - prevent chest from being stuck in open state on clients
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 9, par1EntityMinecartHopper.getInvName(), par1EntityMinecartHopper.getSizeInventory(), par1EntityMinecartHopper.isInvNameLocalized()));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    /**
     * Displays the furnace GUI for the passed in furnace entity. Args: tileEntityFurnace
     */
    public void displayGUIFurnace(TileEntityFurnace par1TileEntityFurnace)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerFurnace(this.inventory, par1TileEntityFurnace));

        if (container == null)
        {
            par1TileEntityFurnace.closeChest(); // Cauldron - prevent chests from being stuck in open state on clients
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 2, par1TileEntityFurnace.getInvName(), par1TileEntityFurnace.getSizeInventory(), par1TileEntityFurnace.isInvNameLocalized()));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    /**
     * Displays the dipsenser GUI for the passed in dispenser entity. Args: TileEntityDispenser
     */
    public void displayGUIDispenser(TileEntityDispenser par1TileEntityDispenser)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerDispenser(this.inventory, par1TileEntityDispenser));

        if (container == null)
        {
            par1TileEntityDispenser.closeChest(); // Cauldron - prevent chests from being stuck in open state on clients
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, par1TileEntityDispenser instanceof TileEntityDropper ? 10 : 3, par1TileEntityDispenser.getInvName(), par1TileEntityDispenser.getSizeInventory(), par1TileEntityDispenser.isInvNameLocalized()));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    /**
     * Displays the GUI for interacting with a brewing stand.
     */
    public void displayGUIBrewingStand(TileEntityBrewingStand par1TileEntityBrewingStand)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerBrewingStand(this.inventory, par1TileEntityBrewingStand));

        if (container == null)
        {
            par1TileEntityBrewingStand.closeChest(); // Cauldron - prevent chests from being stuck in open state on clients
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 5, par1TileEntityBrewingStand.getInvName(), par1TileEntityBrewingStand.getSizeInventory(), par1TileEntityBrewingStand.isInvNameLocalized()));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    /**
     * Displays the GUI for interacting with a beacon.
     */
    public void displayGUIBeacon(TileEntityBeacon par1TileEntityBeacon)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerBeacon(this.inventory, par1TileEntityBeacon));

        if (container == null)
        {
            par1TileEntityBeacon.closeChest(); // Cauldron - prevent chests from being stuck in open state on clients
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 7, par1TileEntityBeacon.getInvName(), par1TileEntityBeacon.getSizeInventory(), par1TileEntityBeacon.isInvNameLocalized()));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    public void displayGUIMerchant(IMerchant par1IMerchant, String par2Str)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerMerchant(this.inventory, par1IMerchant, this.worldObj));

        if (container == null)
        {
            return;
        }

        // CraftBukkit end
        this.nextContainerCounter();
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
        InventoryMerchant inventorymerchant = ((ContainerMerchant)this.openContainer).getMerchantInventory();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 6, par2Str == null ? "" : par2Str, inventorymerchant.getSizeInventory(), par2Str != null));
        MerchantRecipeList merchantrecipelist = par1IMerchant.getRecipes(this);

        if (merchantrecipelist != null)
        {
            try
            {
                ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
                DataOutputStream dataoutputstream = new DataOutputStream(bytearrayoutputstream);
                dataoutputstream.writeInt(this.currentWindowId);
                merchantrecipelist.writeRecipiesToStream(dataoutputstream);
                this.playerNetServerHandler.sendPacketToPlayer(new Packet250CustomPayload("MC|TrList", bytearrayoutputstream.toByteArray()));
            }
            catch (IOException ioexception)
            {
                ioexception.printStackTrace();
            }
        }
    }

    public void displayGUIHorse(EntityHorse par1EntityHorse, IInventory par2IInventory)
    {
        // CraftBukkit start - Inventory open hook
        Container container = CraftEventFactory.callInventoryOpenEvent(this, new ContainerHorseInventory(this.inventory, par2IInventory, par1EntityHorse));

        if (container == null)
        {
            par2IInventory.closeChest(); // Cauldron - prevent chests from being stuck in open state on clients
            return;
        }

        // CraftBukkit end

        if (this.openContainer != this.inventoryContainer)
        {
            this.closeScreen();
        }

        this.incrementWindowID();
        this.playerNetServerHandler.sendPacketToPlayer(new Packet100OpenWindow(this.currentWindowId, 11, par2IInventory.getInvName(), par2IInventory.getSizeInventory(), par2IInventory.isInvNameLocalized(), par1EntityHorse.entityId));
        this.openContainer = container; // CraftBukkit - Use container we passed to event
        this.openContainer.windowId = this.currentWindowId;
        this.openContainer.addCraftingToCrafters(this);
    }

    /**
     * Sends the contents of an inventory slot to the client-side Container. This doesn't have to match the actual
     * contents of that slot. Args: Container, slot number, slot contents
     */
    public void sendSlotContents(Container par1Container, int par2, ItemStack par3ItemStack)
    {
        if (!(par1Container.getSlot(par2) instanceof SlotCrafting))
        {
            if (!this.playerInventoryBeingManipulated)
            {
                this.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(par1Container.windowId, par2, par3ItemStack));
            }
        }
    }

    public void sendContainerToPlayer(Container par1Container)
    {
        this.sendContainerAndContentsToPlayer(par1Container, par1Container.getInventory());
    }

    public void sendContainerAndContentsToPlayer(Container par1Container, List par2List)
    {
        this.playerNetServerHandler.sendPacketToPlayer(new Packet104WindowItems(par1Container.windowId, par2List));
        this.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(-1, -1, this.inventory.getItemStack()));

        if (par1Container.getBukkitView() == null) return;         // Cauldron - allow vanilla mods to bypass
        // CraftBukkit start - Send a Set Slot to update the crafting result slot
        if (java.util.EnumSet.of(InventoryType.CRAFTING, InventoryType.WORKBENCH).contains(par1Container.getBukkitView().getType()))
        {
            this.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(par1Container.windowId, 0, par1Container.getSlot(0).getStack()));
        }

        // CraftBukkit end
    }

    /**
     * Sends two ints to the client-side Container. Used for furnace burning time, smelting progress, brewing progress,
     * and enchanting level. Normally the first int identifies which variable to update, and the second contains the new
     * value. Both are truncated to shorts in non-local SMP.
     */
    public void sendProgressBarUpdate(Container par1Container, int par2, int par3)
    {
        this.playerNetServerHandler.sendPacketToPlayer(new Packet105UpdateProgressbar(par1Container.windowId, par2, par3));
    }

    /**
     * sets current screen to null (used on escape buttons of GUIs)
     */
    public void closeScreen()
    {
        CraftEventFactory.handleInventoryCloseEvent(this); // CraftBukkit
        this.playerNetServerHandler.sendPacketToPlayer(new Packet101CloseWindow(this.openContainer.windowId));
        this.closeContainer();
    }

    /**
     * updates item held by mouse
     */
    public void updateHeldItem()
    {
        if (!this.playerInventoryBeingManipulated)
        {
            this.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(-1, -1, this.inventory.getItemStack()));
        }
    }

    /**
     * Closes the container the player currently has open.
     */
    public void closeContainer()
    {
        this.openContainer.onContainerClosed(this);
        this.openContainer = this.inventoryContainer;
    }

    public void setEntityActionState(float par1, float par2, boolean par3, boolean par4)
    {
        if (this.ridingEntity != null)
        {
            if (par1 >= -1.0F && par1 <= 1.0F)
            {
                this.moveStrafing = par1;
            }

            if (par2 >= -1.0F && par2 <= 1.0F)
            {
                this.moveForward = par2;
            }

            this.isJumping = par3;
            this.setSneaking(par4);
        }
    }

    /**
     * Adds a value to a statistic field.
     */
    public void addStat(StatBase par1StatBase, int par2)
    {
        if (par1StatBase != null)
        {
            if (!par1StatBase.isIndependent)
            {
                this.playerNetServerHandler.sendPacketToPlayer(new Packet200Statistic(par1StatBase.statId, par2));
            }
        }
    }

    public void mountEntityAndWakeUp()
    {
        if (this.riddenByEntity != null)
        {
            this.riddenByEntity.mountEntity(this);
        }

        if (this.sleeping)
        {
            this.wakeUpPlayer(true, false, false);
        }
    }

    /**
     * this function is called when a players inventory is sent to him, lastHealth is updated on any dimension
     * transitions, then reset.
     */
    public void setPlayerHealthUpdated()
    {
        this.lastHealth = -1.0E8F;
        this.lastExperience = -1; // CraftBukkit - Added to reset
    }

    /**
     * Add a chat message to the player
     */
    public void addChatMessage(String par1Str)
    {
        this.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat(ChatMessageComponent.createFromTranslationKey(par1Str)));
    }

    /**
     * Used for when item use count runs out, ie: eating completed
     */
    protected void onItemUseFinish()
    {
        this.playerNetServerHandler.sendPacketToPlayer(new Packet38EntityStatus(this.entityId, (byte)9));
        super.onItemUseFinish();
    }

    /**
     * sets the itemInUse when the use item button is clicked. Args: itemstack, int maxItemUseDuration
     */
    public void setItemInUse(ItemStack par1ItemStack, int par2)
    {
        super.setItemInUse(par1ItemStack, par2);

        if (par1ItemStack != null && par1ItemStack.getItem() != null && par1ItemStack.getItem().getItemUseAction(par1ItemStack) == EnumAction.eat)
        {
            this.getServerForPlayer().getEntityTracker().sendPacketToAllAssociatedPlayers(this, new Packet18Animation(this, 5));
        }
    }

    /**
     * Copies the values from the given player into this player if boolean par2 is true. Always clones Ender Chest
     * Inventory.
     */
    public void clonePlayer(EntityPlayer par1EntityPlayer, boolean par2)
    {
        super.clonePlayer(par1EntityPlayer, par2);
        this.lastExperience = -1;
        this.lastHealth = -1.0F;
        this.lastFoodLevel = -1;
        this.destroyedItemsNetCache.addAll(((EntityPlayerMP)par1EntityPlayer).destroyedItemsNetCache);
    }

    protected void onNewPotionEffect(PotionEffect par1PotionEffect)
    {
        super.onNewPotionEffect(par1PotionEffect);
        this.playerNetServerHandler.sendPacketToPlayer(new Packet41EntityEffect(this.entityId, par1PotionEffect));
    }

    protected void onChangedPotionEffect(PotionEffect par1PotionEffect, boolean par2)
    {
        super.onChangedPotionEffect(par1PotionEffect, par2);
        this.playerNetServerHandler.sendPacketToPlayer(new Packet41EntityEffect(this.entityId, par1PotionEffect));
    }

    protected void onFinishedPotionEffect(PotionEffect par1PotionEffect)
    {
        super.onFinishedPotionEffect(par1PotionEffect);
        this.playerNetServerHandler.sendPacketToPlayer(new Packet42RemoveEntityEffect(this.entityId, par1PotionEffect));
    }

    /**
     * Move the entity to the coordinates informed, but keep yaw/pitch values.
     */
    public void setPositionAndUpdate(double par1, double par3, double par5)
    {
        this.playerNetServerHandler.setPlayerLocation(par1, par3, par5, this.rotationYaw, this.rotationPitch);
    }

    /**
     * Called when the player performs a critical hit on the Entity. Args: entity that was hit critically
     */
    public void onCriticalHit(Entity par1Entity)
    {
        this.getServerForPlayer().getEntityTracker().sendPacketToAllAssociatedPlayers(this, new Packet18Animation(par1Entity, 6));
    }

    public void onEnchantmentCritical(Entity par1Entity)
    {
        this.getServerForPlayer().getEntityTracker().sendPacketToAllAssociatedPlayers(this, new Packet18Animation(par1Entity, 7));
    }

    /**
     * Sends the player's abilities to the server (if there is one).
     */
    public void sendPlayerAbilities()
    {
        if (this.playerNetServerHandler != null)
        {
            this.playerNetServerHandler.sendPacketToPlayer(new Packet202PlayerAbilities(this.capabilities));
        }
    }

    public WorldServer getServerForPlayer()
    {
        return (WorldServer)this.worldObj;
    }

    /**
     * Sets the player's game mode and sends it to them.
     */
    public void setGameType(EnumGameType par1EnumGameType)
    {
        this.theItemInWorldManager.setGameType(par1EnumGameType);
        this.playerNetServerHandler.sendPacketToPlayer(new Packet70GameEvent(3, par1EnumGameType.getID()));
    }

    public void sendChatToPlayer(ChatMessageComponent par1ChatMessageComponent)
    {
        this.playerNetServerHandler.sendPacketToPlayer(new Packet3Chat(par1ChatMessageComponent));
    }

    /**
     * Returns true if the command sender is allowed to use the given command.
     */
    public boolean canCommandSenderUseCommand(int par1, String par2Str)
    {
        return "seed".equals(par2Str) && !this.mcServer.isDedicatedServer() ? true : (!"tell".equals(par2Str) && !"help".equals(par2Str) && !"me".equals(par2Str) ? (this.mcServer.getConfigurationManager().isPlayerOpped(this.username) ? this.mcServer.func_110455_j() >= par1 : false) : true);
    }

    /**
     * Gets the player's IP address. Used in /banip.
     */
    public String getPlayerIP()
    {
        String s = this.playerNetServerHandler.netManager.getSocketAddress().toString();
        s = s.substring(s.indexOf("/") + 1);
        s = s.substring(0, s.indexOf(":"));
        return s;
    }

    public void updateClientInfo(Packet204ClientInfo par1Packet204ClientInfo)
    {
        this.translator = par1Packet204ClientInfo.getLanguage();
        int i = 256 >> par1Packet204ClientInfo.getRenderDistance();

        if (i > 3 && i < 15)
        {
            this.renderDistance = i;
        }

        this.chatVisibility = par1Packet204ClientInfo.getChatVisibility();
        this.chatColours = par1Packet204ClientInfo.getChatColours();

        if (this.mcServer.isSinglePlayer() && this.mcServer.getServerOwner().equals(this.username))
        {
            this.mcServer.setDifficultyForAllWorlds(par1Packet204ClientInfo.getDifficulty());
        }

        this.setHideCape(1, !par1Packet204ClientInfo.getShowCape());
    }

    public int getChatVisibility()
    {
        return this.chatVisibility;
    }

    /**
     * on recieving this message the client (if permission is given) will download the requested textures
     */
    public void requestTexturePackLoad(String par1Str, int par2)
    {
        String s1 = par1Str + "\0" + par2; // CraftBukkit - fix decompile error
        this.playerNetServerHandler.sendPacketToPlayer(new Packet250CustomPayload("MC|TPack", s1.getBytes()));
    }

    /**
     * Return the position for this command sender.
     */
    public ChunkCoordinates getPlayerCoordinates()
    {
        return new ChunkCoordinates(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posY + 0.5D), MathHelper.floor_double(this.posZ));
    }

    public void func_143004_u()
    {
        this.field_143005_bX = MinecraftServer.getSystemTimeMillis();
    }
    
    // CraftBukkit start
    public long timeOffset = 0;
    public boolean relativeTime = true;

    public long getPlayerTime()
    {
        if (this.relativeTime)
        {
            // Adds timeOffset to the current server time.
            return this.worldObj.getWorldTime() + this.timeOffset;
        }
        else
        {
            // Adds timeOffset to the beginning of this day.
            return this.worldObj.getWorldTime() - (this.worldObj.getWorldTime() % 24000) + this.timeOffset;
        }
    }

    public WeatherType weather = null;

    public WeatherType getPlayerWeather()
    {
        return this.weather;
    }

    public void setPlayerWeather(WeatherType type, boolean plugin)
    {
        if (!plugin && this.weather != null)
        {
            return;
        }

        if (plugin)
        {
            this.weather = type;
        }

        this.playerNetServerHandler.sendPacketToPlayer(new Packet70GameEvent(type == WeatherType.DOWNFALL ? 1 : 2, 0));
    }

    public void resetPlayerWeather()
    {
        this.weather = null;
        this.setPlayerWeather(this.worldObj.getWorldInfo().isRaining() ? WeatherType.DOWNFALL : WeatherType.CLEAR, false);
    }

    @Override
    public String toString()
    {
        return super.toString() + "(" + this.username + " at " + this.posX + "," + this.posY + "," + this.posZ + ")";
    }

    public void reset()
    {
        float exp = 0;
        boolean keepInventory = this.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory");

        if (this.keepLevel || keepInventory)
        {
            exp = this.experience;
            this.newTotalExp = this.experienceTotal;
            this.newLevel = this.experienceLevel;
        }

        this.setHealth(this.getMaxHealth());
        this.fire = 0;
        this.fallDistance = 0;
        this.foodStats = new FoodStats();
        this.experienceLevel = this.newLevel;
        this.experienceTotal = this.newTotalExp;
        this.experience = 0;
        this.deathTime = 0;
        this.clearActivePotions(); // Should be removeAllEffects.
        super.potionsNeedUpdate = true; // Cauldron - change to super to temporarily workaround remapping bug with SpecialSource
        this.openContainer = this.inventoryContainer;
        this.attackingPlayer = null;
        this.entityLivingToAttack = null;
        this._combatTracker = new CombatTracker(this);
        this.lastExperience = -1;

        if (this.keepLevel || keepInventory)
        {
            this.experience = exp;
        }
        else
        {
            this.addExperience(this.newExp);
        }

        this.keepLevel = false;
    }

    @Override
    public CraftPlayer getBukkitEntity()
    {
        return (CraftPlayer) super.getBukkitEntity();
    }
    // CraftBukkit end

    /* ===================================== FORGE START =====================================*/
    
    /**
     * Returns the default eye height of the player
     * @return player default eye height
     */
    @Override
    public float getDefaultEyeHeight()
    {
        return 1.62F;
    }
}