package net.minecraft.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
// CraftBukkit start
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
// CraftBukkit end

public class InventoryCraftResult implements IInventory
{
    private ItemStack[] stackResult = new ItemStack[1];

    // CraftBukkit start
    private int maxStack = MAX_STACK;

    public ItemStack[] getContents()
    {
        return this.stackResult;
    }

    public org.bukkit.inventory.InventoryHolder getOwner()
    {
        return null; // Result slots don't get an owner
    }

    // Don't need a transaction; the InventoryCrafting keeps track of it for us
    public void onOpen(CraftHumanEntity who) {}
    public void onClose(CraftHumanEntity who) {}
    public java.util.List<HumanEntity> getViewers()
    {
        return new java.util.ArrayList<HumanEntity>();
    }

    public void setMaxStackSize(int size)
    {
        maxStack = size;
    }
    // CraftBukkit end
    private static final String __OBFID = "CL_00001760";

    public int getSizeInventory()
    {
        return 1;
    }

    public ItemStack getStackInSlot(int p_70301_1_)
    {
        return this.stackResult[0];
    }

    public String getInventoryName()
    {
        return "Result";
    }

    public boolean hasCustomInventoryName()
    {
        return false;
    }

    public ItemStack decrStackSize(int p_70298_1_, int p_70298_2_)
    {
        if (this.stackResult[0] != null)
        {
            ItemStack itemstack = this.stackResult[0];
            this.stackResult[0] = null;
            return itemstack;
        }
        else
        {
            return null;
        }
    }

    public ItemStack getStackInSlotOnClosing(int p_70304_1_)
    {
        if (this.stackResult[0] != null)
        {
            ItemStack itemstack = this.stackResult[0];
            this.stackResult[0] = null;
            return itemstack;
        }
        else
        {
            return null;
        }
    }

    public void setInventorySlotContents(int p_70299_1_, ItemStack p_70299_2_)
    {
        this.stackResult[0] = p_70299_2_;
    }

    public int getInventoryStackLimit()
    {
        return maxStack; // CraftBukkit
    }

    public void markDirty() {}

    public boolean isUseableByPlayer(EntityPlayer p_70300_1_)
    {
        return true;
    }

    public void openInventory() {}

    public void closeInventory() {}

    public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_)
    {
        return true;
    }
}