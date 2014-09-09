package net.minecraft.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityEnderChest;

// CraftBukkit start
import java.util.List;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
// CraftBukkit end


public class InventoryEnderChest extends InventoryBasic
{
    private TileEntityEnderChest associatedChest;

    // CraftBukkit start
    public List<HumanEntity> transaction = new java.util.ArrayList<HumanEntity>();
    public org.bukkit.entity.Player player;
    private int maxStack = MAX_STACK;

    public ItemStack[] getContents()
    {
        return this.inventoryContents;
    }

    public void onOpen(CraftHumanEntity who)
    {
        transaction.add(who);
    }

    public void onClose(CraftHumanEntity who)
    {
        transaction.remove(who);
    }

    public List<HumanEntity> getViewers()
    {
        return transaction;
    }

    public org.bukkit.inventory.InventoryHolder getOwner()
    {
        return this.player;
    }

    public void setMaxStackSize(int size)
    {
        maxStack = size;
    }

    /**
     * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended. *Isn't
     * this more of a set than a get?*
     */
    public int getInventoryStackLimit()
    {
        return maxStack;
    }
    // CraftBukkit end
    private static final String __OBFID = "CL_00001759";

    public InventoryEnderChest()
    {
        super("container.enderchest", false, 27);
    }

    public void func_146031_a(TileEntityEnderChest p_146031_1_)
    {
        this.associatedChest = p_146031_1_;
    }

    public void loadInventoryFromNBT(NBTTagList p_70486_1_)
    {
        int i;

        for (i = 0; i < this.getSizeInventory(); ++i)
        {
            this.setInventorySlotContents(i, (ItemStack)null);
        }

        for (i = 0; i < p_70486_1_.tagCount(); ++i)
        {
            NBTTagCompound nbttagcompound = p_70486_1_.getCompoundTagAt(i);
            int j = nbttagcompound.getByte("Slot") & 255;

            if (j >= 0 && j < this.getSizeInventory())
            {
                this.setInventorySlotContents(j, ItemStack.loadItemStackFromNBT(nbttagcompound));
            }
        }
    }

    public NBTTagList saveInventoryToNBT()
    {
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.getSizeInventory(); ++i)
        {
            ItemStack itemstack = this.getStackInSlot(i);

            if (itemstack != null)
            {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                nbttagcompound.setByte("Slot", (byte)i);
                itemstack.writeToNBT(nbttagcompound);
                nbttaglist.appendTag(nbttagcompound);
            }
        }

        return nbttaglist;
    }

    public boolean isUseableByPlayer(EntityPlayer p_70300_1_)
    {
        return this.associatedChest != null && !this.associatedChest.func_145971_a(p_70300_1_) ? false : super.isUseableByPlayer(p_70300_1_);
    }

    public void openInventory()
    {
        if (this.associatedChest != null)
        {
            this.associatedChest.func_145969_a();
        }

        super.openInventory();
    }

    public void closeInventory()
    {
        if (this.associatedChest != null)
        {
            this.associatedChest.func_145970_b();
        }

        super.closeInventory();
        this.associatedChest = null;
    }
}