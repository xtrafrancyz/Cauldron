package net.minecraft.inventory;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.network.packet.Packet103SetSlot;
import net.minecraft.world.World;
// CraftBukkit end

public class ContainerWorkbench extends Container
{
    /** The crafting matrix inventory (3x3). */
    public InventoryCrafting craftMatrix; // CraftBukkit - move initialization into constructor
    public IInventory craftResult; // CraftBukkit - move initialization into constructor
    private World worldObj;
    private int posX;
    private int posY;
    private int posZ;
    // CraftBukkit start
    private CraftInventoryView bukkitEntity = null;
    private InventoryPlayer player;
    // CraftBukkit end

    public ContainerWorkbench(InventoryPlayer par1InventoryPlayer, World par2World, int par3, int par4, int par5)
    {
        // CraftBukkit start - Switched order of IInventory construction and stored player
        this.craftResult = new InventoryCraftResult();
        this.craftMatrix = new InventoryCrafting(this, 3, 3, par1InventoryPlayer.player); // CraftBukkit - pass player
        this.craftMatrix.resultInventory = this.craftResult;
        this.player = par1InventoryPlayer;
        // CraftBukkit end
        this.worldObj = par2World;
        this.posX = par3;
        this.posY = par4;
        this.posZ = par5;
        this.addSlotToContainer(new SlotCrafting(par1InventoryPlayer.player, this.craftMatrix, this.craftResult, 0, 124, 35));
        int l;
        int i1;

        for (l = 0; l < 3; ++l)
        {
            for (i1 = 0; i1 < 3; ++i1)
            {
                this.addSlotToContainer(new Slot(this.craftMatrix, i1 + l * 3, 30 + i1 * 18, 17 + l * 18));
            }
        }

        for (l = 0; l < 3; ++l)
        {
            for (i1 = 0; i1 < 9; ++i1)
            {
                this.addSlotToContainer(new Slot(par1InventoryPlayer, i1 + l * 9 + 9, 8 + i1 * 18, 84 + l * 18));
            }
        }

        for (l = 0; l < 9; ++l)
        {
            this.addSlotToContainer(new Slot(par1InventoryPlayer, l, 8 + l * 18, 142));
        }

        this.onCraftMatrixChanged(this.craftMatrix);
    }

    /**
     * Callback for when the crafting matrix is changed.
     */
    public void onCraftMatrixChanged(IInventory par1IInventory)
    {
        // CraftBukkit start
        CraftingManager.getInstance().lastCraftView = getBukkitView();
        ItemStack craftResult = CraftingManager.getInstance().findMatchingRecipe(this.craftMatrix, this.worldObj);
        this.craftResult.setInventorySlotContents(0, craftResult);

        if (super.crafters.size() < 1)
        {
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) super.crafters.get(0); // TODO: Is this _always_ correct? Seems like it.
        player.playerNetServerHandler.sendPacketToPlayer(new Packet103SetSlot(player.openContainer.windowId, 0, craftResult));
        // CraftBukkit end
    }

    /**
     * Called when the container is closed.
     */
    public void onContainerClosed(EntityPlayer par1EntityPlayer)
    {
        super.onContainerClosed(par1EntityPlayer);

        if (!this.worldObj.isRemote)
        {
            for (int i = 0; i < 9; ++i)
            {
                ItemStack itemstack = this.craftMatrix.getStackInSlotOnClosing(i);

                if (itemstack != null)
                {
                    par1EntityPlayer.dropPlayerItem(itemstack);
                }
            }
        }
    }

    public boolean canInteractWith(EntityPlayer par1EntityPlayer)
    {
        if (!this.checkReachable)
        {
            return true;    // CraftBukkit
        }

        return this.worldObj.getBlockId(this.posX, this.posY, this.posZ) != Block.workbench.blockID ? false : par1EntityPlayer.getDistanceSq((double)this.posX + 0.5D, (double)this.posY + 0.5D, (double)this.posZ + 0.5D) <= 64.0D;
    }

    /**
     * Called when a player shift-clicks on a slot. You must override this or you will crash when someone does that.
     */
    public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2)
    {
        ItemStack itemstack = null;
        Slot slot = (Slot)this.inventorySlots.get(par2);

        if (slot != null && slot.getHasStack())
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (par2 == 0)
            {
                if (!this.mergeItemStack(itemstack1, 10, 46, true))
                {
                    return null;
                }

                slot.onSlotChange(itemstack1, itemstack);
            }
            else if (par2 >= 10 && par2 < 37)
            {
                if (!this.mergeItemStack(itemstack1, 37, 46, false))
                {
                    return null;
                }
            }
            else if (par2 >= 37 && par2 < 46)
            {
                if (!this.mergeItemStack(itemstack1, 10, 37, false))
                {
                    return null;
                }
            }
            else if (!this.mergeItemStack(itemstack1, 10, 46, false))
            {
                return null;
            }

            if (itemstack1.stackSize == 0)
            {
                slot.putStack((ItemStack)null);
            }
            else
            {
                slot.onSlotChanged();
            }

            if (itemstack1.stackSize == itemstack.stackSize)
            {
                return null;
            }

            slot.onPickupFromSlot(par1EntityPlayer, itemstack1);
        }

        return itemstack;
    }

    public boolean func_94530_a(ItemStack par1ItemStack, Slot par2Slot)
    {
        return par2Slot.inventory != this.craftResult && super.func_94530_a(par1ItemStack, par2Slot);
    }

    // CraftBukkit start
    public CraftInventoryView getBukkitView()
    {
        if (bukkitEntity != null)
        {
            return bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftMatrix, this.craftResult);
        bukkitEntity = new CraftInventoryView(this.player.player.getBukkitEntity(), inventory, this);
        return bukkitEntity;
    }
    // CraftBukkit end
}