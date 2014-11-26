package net.minecraft.item;

// CraftBukkit start
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet53BlockChange;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumGameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
// CraftBukkit end

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.BlockEvent;
// Cauldron start
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.event.inventory.InventoryType;
// Cauldron end

public class ItemInWorldManager
{
    /** Forge reach distance */
    private double blockReachDistance = 5.0d;

    /** The world object that this object is connected to. */
    public World theWorld;

    /** The EntityPlayerMP object that this object is connected to. */
    public EntityPlayerMP thisPlayerMP;
    private EnumGameType gameType;

    /** True if the player is destroying a block */
    private boolean isDestroyingBlock;
    private int initialDamage;
    private int partiallyDestroyedBlockX;
    private int partiallyDestroyedBlockY;
    private int partiallyDestroyedBlockZ;
    private int curblockDamage;

    /**
     * Set to true when the "finished destroying block" packet is received but the block wasn't fully damaged yet. The
     * block will not be destroyed while this is false.
     */
    private boolean receivedFinishDiggingPacket;
    private int posX;
    private int posY;
    private int posZ;
    private int field_73093_n;
    private int durabilityRemainingOnBlock;

    public ItemInWorldManager(World par1World)
    {
        this.gameType = EnumGameType.NOT_SET;
        this.durabilityRemainingOnBlock = -1;
        this.theWorld = par1World;
    }

    public void setGameType(EnumGameType par1EnumGameType)
    {
        this.gameType = par1EnumGameType;
        par1EnumGameType.configurePlayerCapabilities(this.thisPlayerMP.capabilities);
        this.thisPlayerMP.sendPlayerAbilities();
    }

    public EnumGameType getGameType()
    {
        return this.gameType;
    }

    /**
     * Get if we are in creative game mode.
     */
    public boolean isCreative()
    {
        return this.gameType.isCreative();
    }

    /**
     * if the gameType is currently NOT_SET then change it to par1
     */
    public void initializeGameType(EnumGameType par1EnumGameType)
    {
        if (this.gameType == EnumGameType.NOT_SET)
        {
            this.gameType = par1EnumGameType;
        }

        this.setGameType(this.gameType);
    }

    public void updateBlockRemoving()
    {
        this.curblockDamage = MinecraftServer.currentTick; // CraftBukkit
        int i;
        float f;
        int j;

        if (this.receivedFinishDiggingPacket)
        {
            i = this.curblockDamage - this.field_73093_n;
            int k = this.theWorld.getBlockId(this.posX, this.posY, this.posZ);

            if (k == 0)
            {
                this.receivedFinishDiggingPacket = false;
            }
            else
            {
                Block block = Block.blocksList[k];
                f = block.getPlayerRelativeBlockHardness(this.thisPlayerMP, this.thisPlayerMP.worldObj, this.posX, this.posY, this.posZ) * (float)(i + 1);
                j = (int)(f * 10.0F);

                if (j != this.durabilityRemainingOnBlock)
                {
                    this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, this.posX, this.posY, this.posZ, j);
                    this.durabilityRemainingOnBlock = j;
                }

                if (f >= 1.0F)
                {
                    this.receivedFinishDiggingPacket = false;
                    this.tryHarvestBlock(this.posX, this.posY, this.posZ);
                }
            }
        }
        else if (this.isDestroyingBlock)
        {
            i = this.theWorld.getBlockId(this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ);
            Block block1 = Block.blocksList[i];

            if (block1 == null)
            {
                this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ, -1);
                this.durabilityRemainingOnBlock = -1;
                this.isDestroyingBlock = false;
            }
            else
            {
                int l = this.curblockDamage - this.initialDamage;
                f = block1.getPlayerRelativeBlockHardness(this.thisPlayerMP, this.thisPlayerMP.worldObj, this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ) * (float)(l + 1);
                j = (int)(f * 10.0F);

                if (j != this.durabilityRemainingOnBlock)
                {
                    this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ, j);
                    this.durabilityRemainingOnBlock = j;
                }
            }
        }
    }

    /**
     * if not creative, it calls destroyBlockInWorldPartially untill the block is broken first. par4 is the specific
     * side. tryHarvestBlock can also be the result of this call
     */
    public void onBlockClicked(int par1, int par2, int par3, int par4) // Cauldron - merge this whole method by hand
    {
        // this.world.douseFire((EntityHuman) null, i, j, k, l); // CraftBukkit - moved down
        // CraftBukkit
        org.bukkit.event.player.PlayerInteractEvent playerinteractevent = CraftEventFactory.callPlayerInteractEvent(this.thisPlayerMP, org.bukkit.event.block.Action.LEFT_CLICK_BLOCK, par1, par2, par3, par4, this.thisPlayerMP.inventory.getCurrentItem());

        if (!this.gameType.isAdventure() || this.thisPlayerMP.isCurrentToolAdventureModeExempt(par1, par2, par3))
        {
            net.minecraftforge.event.entity.player.PlayerInteractEvent playerinteractevent1 = ForgeEventFactory.onPlayerInteract(this.thisPlayerMP, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.LEFT_CLICK_BLOCK, par1, par2, par3, par4); // Forge

            // CraftBukkit start
            if (playerinteractevent.isCancelled() || playerinteractevent1.isCanceled())
            {
                // Let the client know the block still exists
                ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                // Update any tile entity data for this block
                TileEntity tileentity = this.theWorld.getBlockTileEntity(par1, par2, par3);

                if (tileentity != null)
                {
                    this.thisPlayerMP.playerNetServerHandler.sendPacketToPlayer(tileentity.getDescriptionPacket());
                }

                return;
            }

            // CraftBukkit end
            if (this.isCreative())
            {
                if (!this.theWorld.extinguishFire((EntityPlayer)null, par1, par2, par3, par4))
                {
                    this.tryHarvestBlock(par1, par2, par3);
                }
            }
            else
            {
                //this.world.douseFire(this.player, i, j, k, l);  // Forge
                this.initialDamage = this.curblockDamage;
                float f = 1.0F;
                int i1 = this.theWorld.getBlockId(par1, par2, par3);
                // CraftBukkit start - Swings at air do *NOT* exist.
                Block block = Block.blocksList[i1]; // Forge

                if (block != null)
                {
                    if (playerinteractevent.useInteractedBlock() == org.bukkit.event.Event.Result.DENY || playerinteractevent1.useBlock == net.minecraftforge.event.Event.Result.DENY)   // Cauldron
                    {
                        // If we denied a door from opening, we need to send a correcting update to the client, as it already opened the door.
                        if (i1 == Block.doorWood.blockID)
                        {
                            // For some reason *BOTH* the bottom/top part have to be marked updated.
                            boolean bottom = (this.theWorld.getBlockMetadata(par1, par2, par3) & 8) == 0;
                            ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                            ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2 + (bottom ? 1 : -1), par3, this.theWorld));
                        }
                        else if (i1 == Block.trapdoor.blockID)
                        {
                            ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                        }
                    }
                    else
                    {
                        // Forge start
                        block.onBlockClicked(theWorld, par1, par2, par3, this.thisPlayerMP);
                        theWorld.extinguishFire(this.thisPlayerMP, par1, par2, par3, par4);
                        f = block.getPlayerRelativeBlockHardness(this.thisPlayerMP, this.theWorld, par1, par2, par3);
                        // Forge end
                    }
                }

                if (playerinteractevent.useItemInHand() == org.bukkit.event.Event.Result.DENY || playerinteractevent1.useItem == net.minecraftforge.event.Event.Result.DENY)   // Forge
                {
                    // If we 'insta destroyed' then the client needs to be informed.
                    if (f > 1.0f)
                    {
                        ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                    }

                    return;
                }

                org.bukkit.event.block.BlockDamageEvent blockEvent = CraftEventFactory.callBlockDamageEvent(this.thisPlayerMP, par1, par2, par3, this.thisPlayerMP.inventory.getCurrentItem(), f >= 1.0f);

                if (blockEvent.isCancelled())
                {
                    // Let the client know the block still exists
                    ((EntityPlayerMP) this.thisPlayerMP).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
                    return;
                }

                if (blockEvent.getInstaBreak())
                {
                    f = 2.0f;
                }

                // CraftBukkit end

                if (i1 > 0 && f >= 1.0F)
                {
                    this.tryHarvestBlock(par1, par2, par3);
                }
                else
                {
                    this.isDestroyingBlock = true;
                    this.partiallyDestroyedBlockX = par1;
                    this.partiallyDestroyedBlockY = par2;
                    this.partiallyDestroyedBlockZ = par3;
                    int j1 = (int)(f * 10.0F);
                    this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, par1, par2, par3, j1);
                    this.durabilityRemainingOnBlock = j1;
                }
            }
            theWorld.spigotConfig.antiXrayInstance.updateNearbyBlocks(theWorld, par1, par2, par3); // Spigot
        }
    }

    public void uncheckedTryHarvestBlock(int par1, int par2, int par3)
    {
        if (par1 == this.partiallyDestroyedBlockX && par2 == this.partiallyDestroyedBlockY && par3 == this.partiallyDestroyedBlockZ)
        {
            this.curblockDamage = MinecraftServer.currentTick; // CraftBukkit
            int l = this.curblockDamage - this.initialDamage;
            int i1 = this.theWorld.getBlockId(par1, par2, par3);

            if (i1 != 0)
            {
                Block block = Block.blocksList[i1];
                float f = block.getPlayerRelativeBlockHardness(this.thisPlayerMP, this.thisPlayerMP.worldObj, par1, par2, par3) * (float)(l + 1);

                if (f >= 0.7F)
                {
                    this.isDestroyingBlock = false;
                    this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, par1, par2, par3, -1);
                    this.tryHarvestBlock(par1, par2, par3);
                }
                else if (!this.receivedFinishDiggingPacket)
                {
                    this.isDestroyingBlock = false;
                    this.receivedFinishDiggingPacket = true;
                    this.posX = par1;
                    this.posY = par2;
                    this.posZ = par3;
                    this.field_73093_n = this.initialDamage;
                }
            }

            // CraftBukkit start - Force block reset to client
        }
        else
        {
            this.thisPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
            // CraftBukkit end
        }
    }

    /**
     * note: this ignores the pars passed in and continues to destroy the onClickedBlock
     */
    public void cancelDestroyingBlock(int par1, int par2, int par3)
    {
        this.isDestroyingBlock = false;
        this.theWorld.destroyBlockInWorldPartially(this.thisPlayerMP.entityId, this.partiallyDestroyedBlockX, this.partiallyDestroyedBlockY, this.partiallyDestroyedBlockZ, -1);
    }

    /**
     * Removes a block and triggers the appropriate events
     */
    private boolean removeBlock(int par1, int par2, int par3)
    {
        Block block = Block.blocksList[this.theWorld.getBlockId(par1, par2, par3)];
        int l = this.theWorld.getBlockMetadata(par1, par2, par3);

        if (block != null)
        {
            block.onBlockHarvested(this.theWorld, par1, par2, par3, l, this.thisPlayerMP);
        }

        boolean flag = (block != null && block.removeBlockByPlayer(theWorld, thisPlayerMP, par1, par2, par3));

        if (block != null && flag)
        {
            block.onBlockDestroyedByPlayer(this.theWorld, par1, par2, par3, l);
        }

        return flag;
    }

    /**
     * Attempts to harvest a block at the given coordinate
     */
    public boolean tryHarvestBlock(int par1, int par2, int par3)
    {
        BlockEvent.BreakEvent event = ForgeHooks.onBlockBreakEvent(theWorld, gameType, thisPlayerMP, par1, par2, par3);
        if (event.isCanceled())
                {
                return false;
            }
        else
        {
        ItemStack stack = thisPlayerMP.getCurrentEquippedItem();
        if (stack != null && stack.getItem().onBlockStartBreak(stack, par1, par2, par3, thisPlayerMP))
        {
            return false;
        }
            int l = this.theWorld.getBlockId(par1, par2, par3);

            if (Block.blocksList[l] == null)
            {
                return false;    // CraftBukkit - A plugin set block to air without cancelling
            }

            int i1 = this.theWorld.getBlockMetadata(par1, par2, par3);

            this.theWorld.playAuxSFXAtEntity(this.thisPlayerMP, 2001, par1, par2, par3, l + (this.theWorld.getBlockMetadata(par1, par2, par3) << 12));
            boolean flag = false;

            if (this.isCreative())
            {
                flag = this.removeBlock(par1, par2, par3);
                this.thisPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par1, par2, par3, this.theWorld));
            }
            else
            {
                ItemStack itemstack = this.thisPlayerMP.getCurrentEquippedItem();
                boolean flag1 = false;
                Block block = Block.blocksList[l];
                if (block != null)
                {
                    flag1 = block.canHarvestBlock(thisPlayerMP, i1);
                }

                if (itemstack != null)
                {
                    itemstack.onBlockDestroyed(this.theWorld, l, par1, par2, par3, this.thisPlayerMP);

                    if (itemstack.stackSize == 0)
                    {
                        this.thisPlayerMP.destroyCurrentEquippedItem();
                    }
                }

                flag = this.removeBlock(par1, par2, par3);
                if (flag && flag1)
                {
                    Block.blocksList[l].harvestBlock(this.theWorld, this.thisPlayerMP, par1, par2, par3, i1);
                }
            }

            // Drop experience
            if (!this.isCreative() && flag && event != null)
            {
                Block.blocksList[l].dropXpOnBlockBreak(this.theWorld, par1, par2, par3, event.getExpToDrop());
            }

            return flag;
        }
    }

    /**
     * Attempts to right-click use an item by the given EntityPlayer in the given World
     */
    public boolean tryUseItem(EntityPlayer par1EntityPlayer, World par2World, ItemStack par3ItemStack)
    {
        int i = par3ItemStack.stackSize;
        int j = par3ItemStack.getItemDamage();
        ItemStack itemstack1 = par3ItemStack.useItemRightClick(par2World, par1EntityPlayer);

        if (itemstack1 == par3ItemStack && (itemstack1 == null || itemstack1.stackSize == i && itemstack1.getMaxItemUseDuration() <= 0 && itemstack1.getItemDamage() == j))
        {
            return false;
        }
        else
        {
            par1EntityPlayer.inventory.mainInventory[par1EntityPlayer.inventory.currentItem] = itemstack1;

            if (this.isCreative())
            {
                itemstack1.stackSize = i;

                if (itemstack1.isItemStackDamageable())
                {
                    itemstack1.setItemDamage(j);
                }
            }

            if (itemstack1.stackSize == 0)
            {
                par1EntityPlayer.inventory.mainInventory[par1EntityPlayer.inventory.currentItem] = null;
                MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(thisPlayerMP, itemstack1));
            }

            if (!par1EntityPlayer.isUsingItem())
            {
                ((EntityPlayerMP)par1EntityPlayer).sendContainerToPlayer(par1EntityPlayer.inventoryContainer);
            }

            return true;
        }
    }

    /**
     * Activate the clicked on block, otherwise use the held item. Args: player, world, itemStack, x, y, z, side,
     * xOffset, yOffset, zOffset
     */
    public boolean activateBlockOrUseItem(EntityPlayer par1EntityPlayer, World par2World, ItemStack par3ItemStack, int par4, int par5, int par6, int par7, float par8, float par9, float par10) // Cauldron - manually merge whole method by hand
    {
        int i1 = par2World.getBlockId(par4, par5, par6);

        // CraftBukkit start - Interact
        boolean result = false;

        if (i1 > 0)
        {
            org.bukkit.event.player.PlayerInteractEvent event = CraftEventFactory.callPlayerInteractEvent(par1EntityPlayer, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, par4, par5, par6, par7, par3ItemStack);
            net.minecraftforge.event.entity.player.PlayerInteractEvent forgeEvent = ForgeEventFactory.onPlayerInteract(par1EntityPlayer, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, par4, par5, par6, par7);
            // Cauldron start
            // if forge event is explicitly cancelled, return
            if (forgeEvent.isCanceled())
            {
                thisPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par4, par5, par6, theWorld));
                return false;
            }
            // if we have no explicit deny, check if item can be used
            if (event.useItemInHand() != org.bukkit.event.Event.Result.DENY && forgeEvent.useItem != net.minecraftforge.event.Event.Result.DENY)
            {
                Item item = (par3ItemStack != null ? par3ItemStack.getItem() : null);
                // try to use an item in hand before activating a block. Used for items such as IC2's wrench.
                if (item != null && item.onItemUseFirst(par3ItemStack, par1EntityPlayer, par2World, par4, par5, par6, par7, par8, par9, par10))
                {
                    if (par3ItemStack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(thisPlayerMP, par3ItemStack);
                        return true;
                }
            }
            // Cauldron end
            if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY || forgeEvent.useBlock == net.minecraftforge.event.Event.Result.DENY)
            {
                // If we denied a door from opening, we need to send a correcting update to the client, as it already opened the door.
                if (i1 == Block.doorWood.blockID)
                {
                    boolean bottom = (par2World.getBlockMetadata(par4, par5, par6) & 8) == 0;
                    ((EntityPlayerMP) par1EntityPlayer).playerNetServerHandler.sendPacketToPlayer(new Packet53BlockChange(par4, par5 + (bottom ? 1 : -1), par6, par2World));
                }

                result = (event.useItemInHand() != org.bukkit.event.Event.Result.ALLOW);
            }
            else if (!par1EntityPlayer.isSneaking() || par3ItemStack == null || par1EntityPlayer.getHeldItem().getItem().shouldPassSneakingClickToBlock(par2World, par4, par5, par6))
            {
                result = Block.blocksList[i1].onBlockActivated(par2World, par4, par5, par6, par1EntityPlayer, par7, par8, par9, par10);
                // Cauldron start - if bukkitView is null, create one. Required for Ender Chests since they do not use NetworkRegistry.openRemoteGUI
                if (thisPlayerMP != null && !(thisPlayerMP.openContainer instanceof ContainerPlayer))
                {
                    if (thisPlayerMP.openContainer.getBukkitView() == null)
                    {
                        TileEntity te = thisPlayerMP.worldObj.getBlockTileEntity(par4, par5, par6);
                        if (te != null && te instanceof IInventory)
                        {
                            IInventory teInv = (IInventory)te;
                            CraftInventory inventory = new CraftInventory(teInv);
                            thisPlayerMP.openContainer.bukkitView = new CraftInventoryView(thisPlayerMP.getBukkitEntity(), inventory, thisPlayerMP.openContainer);
                        }
                        else
                        {
                            thisPlayerMP.openContainer.bukkitView = new CraftInventoryView(thisPlayerMP.getBukkitEntity(), MinecraftServer.getServer().server.createInventory(thisPlayerMP.getBukkitEntity(), InventoryType.CHEST), thisPlayerMP.openContainer);
                        }

                        thisPlayerMP.openContainer = CraftEventFactory.callInventoryOpenEvent(thisPlayerMP, thisPlayerMP.openContainer, false);
                        if (thisPlayerMP.openContainer == null)
                        {
                            thisPlayerMP.openContainer = thisPlayerMP.inventoryContainer;
                            return false;
                        }
                    }
                }
                // Cauldron end
            }

            if (par3ItemStack != null && !result)
            {
                int meta = par3ItemStack.getItemDamage();
                int size = par3ItemStack.stackSize;
                result = par3ItemStack.tryPlaceItemIntoWorld(par1EntityPlayer, par2World, par4, par5, par6, par7, par8, par9, par10);

                // The item count should not decrement in Creative mode.
                if (this.isCreative())
                {
                    par3ItemStack.setItemDamage(meta);
                    par3ItemStack.stackSize = size;
                }

                if (par3ItemStack.stackSize <= 0)
                {
                    ForgeEventFactory.onPlayerDestroyItem(this.thisPlayerMP, par3ItemStack);
                }
            }

            // If we have 'true' and no explicit deny *or* an explicit allow -- run the item part of the hook
            if (par3ItemStack != null && ((!result && event.useItemInHand() != org.bukkit.event.Event.Result.DENY) || event.useItemInHand() == org.bukkit.event.Event.Result.ALLOW))
            {
                this.tryUseItem(par1EntityPlayer, par2World, par3ItemStack);
            }
        }

        return result;
        // CraftBukkit end
    }

    /**
     * Sets the world instance.
     */
    public void setWorld(WorldServer par1WorldServer)
    {
        this.theWorld = par1WorldServer;
    }

    public double getBlockReachDistance()
    {
        return blockReachDistance;
    }
    public void setBlockReachDistance(double distance)
    {
        blockReachDistance = distance;
    }
}