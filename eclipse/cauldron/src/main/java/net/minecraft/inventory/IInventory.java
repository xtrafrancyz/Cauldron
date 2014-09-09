package net.minecraft.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.bukkit.craftbukkit.entity.CraftHumanEntity; // CraftBukkit

public interface IInventory
{
    int getSizeInventory();

    ItemStack getStackInSlot(int p_70301_1_);

    ItemStack decrStackSize(int p_70298_1_, int p_70298_2_);

    ItemStack getStackInSlotOnClosing(int p_70304_1_);

    void setInventorySlotContents(int p_70299_1_, ItemStack p_70299_2_);

    String getInventoryName();

    boolean hasCustomInventoryName();

    int getInventoryStackLimit();

    void markDirty();

    boolean isUseableByPlayer(EntityPlayer p_70300_1_);

    void openInventory();

    void closeInventory();

    boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_);

    // CraftBukkit start
    ItemStack[] getContents();

    void onOpen(CraftHumanEntity who);

    void onClose(CraftHumanEntity who);

    java.util.List<org.bukkit.entity.HumanEntity> getViewers();

    org.bukkit.inventory.InventoryHolder getOwner();

    void setMaxStackSize(int size);

    int MAX_STACK = 64;
    // CraftBukkit end
}