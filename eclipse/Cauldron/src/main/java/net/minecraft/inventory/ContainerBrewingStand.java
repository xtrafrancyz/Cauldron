package net.minecraft.inventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBrewingStand;
// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftInventoryBrewer;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
// CraftBukkit end

public class ContainerBrewingStand extends Container
{
    private TileEntityBrewingStand tileBrewingStand;

    /** Instance of Slot. */
    private final Slot theSlot;
    private int brewTime;
    // CraftBukkit start
    private CraftInventoryView bukkitEntity = null;
    private InventoryPlayer player;
    // CraftBukkit end

    public ContainerBrewingStand(InventoryPlayer par1InventoryPlayer, TileEntityBrewingStand par2TileEntityBrewingStand)
    {
        player = par1InventoryPlayer; // CraftBukkit
        this.tileBrewingStand = par2TileEntityBrewingStand;
        this.addSlotToContainer(new SlotBrewingStandPotion(par1InventoryPlayer.player, par2TileEntityBrewingStand, 0, 56, 46));
        this.addSlotToContainer(new SlotBrewingStandPotion(par1InventoryPlayer.player, par2TileEntityBrewingStand, 1, 79, 53));
        this.addSlotToContainer(new SlotBrewingStandPotion(par1InventoryPlayer.player, par2TileEntityBrewingStand, 2, 102, 46));
        this.theSlot = this.addSlotToContainer(new SlotBrewingStandIngredient(this, par2TileEntityBrewingStand, 3, 79, 17));
        int i;

        for (i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 9; ++j)
            {
                this.addSlotToContainer(new Slot(par1InventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (i = 0; i < 9; ++i)
        {
            this.addSlotToContainer(new Slot(par1InventoryPlayer, i, 8 + i * 18, 142));
        }
    }

    public void addCraftingToCrafters(ICrafting par1ICrafting)
    {
        super.addCraftingToCrafters(par1ICrafting);
        par1ICrafting.sendProgressBarUpdate(this, 0, this.tileBrewingStand.getBrewTime());
    }

    /**
     * Looks for changes made in the container, sends them to every listener.
     */
    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        for (int i = 0; i < this.crafters.size(); ++i)
        {
            ICrafting icrafting = (ICrafting)this.crafters.get(i);

            if (this.brewTime != this.tileBrewingStand.getBrewTime())
            {
                icrafting.sendProgressBarUpdate(this, 0, this.tileBrewingStand.getBrewTime());
            }
        }

        this.brewTime = this.tileBrewingStand.getBrewTime();
    }

    public boolean canInteractWith(EntityPlayer par1EntityPlayer)
    {
        if (!this.checkReachable)
        {
            return true;    // CraftBukkit
        }

        return this.tileBrewingStand.isUseableByPlayer(par1EntityPlayer);
    }

    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int par1, int par2)
    {
        if (par1 == 0)
        {
            this.tileBrewingStand.setBrewTime(par2);
        }
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

            if ((par2 < 0 || par2 > 2) && par2 != 3)
            {
                if (!this.theSlot.getHasStack() && this.theSlot.isItemValid(itemstack1))
                {
                    if (!this.mergeItemStack(itemstack1, 3, 4, false))
                    {
                        return null;
                    }
                }
                else if (SlotBrewingStandPotion.canHoldPotion(itemstack))
                {
                    if (!this.mergeItemStack(itemstack1, 0, 3, false))
                    {
                        return null;
                    }
                }
                else if (par2 >= 4 && par2 < 31)
                {
                    if (!this.mergeItemStack(itemstack1, 31, 40, false))
                    {
                        return null;
                    }
                }
                else if (par2 >= 31 && par2 < 40)
                {
                    if (!this.mergeItemStack(itemstack1, 4, 31, false))
                    {
                        return null;
                    }
                }
                else if (!this.mergeItemStack(itemstack1, 4, 40, false))
                {
                    return null;
                }
            }
            else
            {
                if (!this.mergeItemStack(itemstack1, 4, 40, true))
                {
                    return null;
                }

                slot.onSlotChange(itemstack1, itemstack);
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

    // CraftBukkit start
    public CraftInventoryView getBukkitView()
    {
        if (bukkitEntity != null)
        {
            return bukkitEntity;
        }

        CraftInventoryBrewer inventory = new CraftInventoryBrewer(this.tileBrewingStand);
        bukkitEntity = new CraftInventoryView(this.player.player.getBukkitEntity(), inventory, this);
        return bukkitEntity;
    }
    // CraftBukkit end
}