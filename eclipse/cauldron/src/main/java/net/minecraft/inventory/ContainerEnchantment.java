package net.minecraft.inventory;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import java.util.Random;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

// CraftBukkit start
import java.util.Map;

import org.bukkit.craftbukkit.inventory.CraftInventoryEnchanting;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.entity.Player;
// CraftBukkit end

public class ContainerEnchantment extends Container
{
    // CraftBukkit - make type specific (changed from IInventory)
    public ContainerEnchantTableInventory tableInventory_CB = new ContainerEnchantTableInventory(this, "Enchant", true, 1); // CraftBukkit
    public IInventory tableInventory = tableInventory_CB;
    private World worldPointer;
    private int posX;
    private int posY;
    private int posZ;
    private Random rand = new Random();
    public long nameSeed;
    public int[] enchantLevels = new int[3];
    // CraftBukkit start
    private CraftInventoryView bukkitEntity = null;
    private Player player;
    // CraftBukkit end
    private static final String __OBFID = "CL_00001745";

    public ContainerEnchantment(InventoryPlayer p_i1811_1_, World p_i1811_2_, int p_i1811_3_, int p_i1811_4_, int p_i1811_5_)
    {
        this.worldPointer = p_i1811_2_;
        this.posX = p_i1811_3_;
        this.posY = p_i1811_4_;
        this.posZ = p_i1811_5_;
        this.addSlotToContainer(new Slot(this.tableInventory, 0, 25, 47)
        {
            private static final String __OBFID = "CL_00001747";
            public boolean isItemValid(ItemStack p_75214_1_)
            {
                return true;
            }
        });
        int l;

        for (l = 0; l < 3; ++l)
        {
            for (int i1 = 0; i1 < 9; ++i1)
            {
                this.addSlotToContainer(new Slot(p_i1811_1_, i1 + l * 9 + 9, 8 + i1 * 18, 84 + l * 18));
            }
        }

        for (l = 0; l < 9; ++l)
        {
            this.addSlotToContainer(new Slot(p_i1811_1_, l, 8 + l * 18, 142));
        }

        // CraftBukkit start
        player = (Player) p_i1811_1_.player.getBukkitEntity();
        tableInventory_CB.player = player; // Cauldron
        // CraftBukkit end
    }

    public void addCraftingToCrafters(ICrafting p_75132_1_)
    {
        super.addCraftingToCrafters(p_75132_1_);
        p_75132_1_.sendProgressBarUpdate(this, 0, this.enchantLevels[0]);
        p_75132_1_.sendProgressBarUpdate(this, 1, this.enchantLevels[1]);
        p_75132_1_.sendProgressBarUpdate(this, 2, this.enchantLevels[2]);
    }

    public void detectAndSendChanges()
    {
        super.detectAndSendChanges();

        for (int i = 0; i < this.crafters.size(); ++i)
        {
            ICrafting icrafting = (ICrafting)this.crafters.get(i);
            icrafting.sendProgressBarUpdate(this, 0, this.enchantLevels[0]);
            icrafting.sendProgressBarUpdate(this, 1, this.enchantLevels[1]);
            icrafting.sendProgressBarUpdate(this, 2, this.enchantLevels[2]);
        }
    }

    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int p_75137_1_, int p_75137_2_)
    {
        if (p_75137_1_ >= 0 && p_75137_1_ <= 2)
        {
            this.enchantLevels[p_75137_1_] = p_75137_2_;
        }
        else
        {
            super.updateProgressBar(p_75137_1_, p_75137_2_);
        }
    }

    public void onCraftMatrixChanged(IInventory p_75130_1_)
    {
        if (p_75130_1_ == this.tableInventory)
        {
            ItemStack itemstack = p_75130_1_.getStackInSlot(0);
            int i;

            if (itemstack != null)   // CraftBukkit - relax condition
            {
                this.nameSeed = this.rand.nextLong();

                if (!this.worldPointer.isRemote)
                {
                    i = 0;
                    int j;
                    float power = 0;

                    for (j = -1; j <= 1; ++j)
                    {
                        for (int k = -1; k <= 1; ++k)
                        {
                            if ((j != 0 || k != 0) && this.worldPointer.isAirBlock(this.posX + k, this.posY, this.posZ + j) && this.worldPointer.isAirBlock(this.posX + k, this.posY + 1, this.posZ + j))
                            {
                                power += ForgeHooks.getEnchantPower(worldPointer, posX + k * 2, posY,     posZ + j * 2);
                                power += ForgeHooks.getEnchantPower(worldPointer, posX + k * 2, posY + 1, posZ + j * 2);

                                if (k != 0 && j != 0)
                                {
                                    power += ForgeHooks.getEnchantPower(worldPointer, posX + k * 2, posY,     posZ + j    );
                                    power += ForgeHooks.getEnchantPower(worldPointer, posX + k * 2, posY + 1, posZ + j    );
                                    power += ForgeHooks.getEnchantPower(worldPointer, posX + k,     posY,     posZ + j * 2);
                                    power += ForgeHooks.getEnchantPower(worldPointer, posX + k,     posY + 1, posZ + j * 2);
                                }
                            }
                        }
                    }

                    for (j = 0; j < 3; ++j)
                    {
                        this.enchantLevels[j] = EnchantmentHelper.calcItemStackEnchantability(this.rand, j, (int)power, itemstack);
                    }

                    // CraftBukkit start
                    CraftItemStack item = CraftItemStack.asCraftMirror(itemstack);
                    PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(player, this.getBukkitView(), this.worldPointer.getWorld().getBlockAt(this.posX, this.posY, this.posZ), item, this.enchantLevels, i);
                    event.setCancelled(!itemstack.isItemEnchantable());
                    if (this.getBukkitView() != null) this.worldPointer.getServer().getPluginManager().callEvent(event); // Cauldron - allow vanilla mods to byp

                    if (event.isCancelled())
                    {
                        for (i = 0; i < 3; ++i)
                        {
                            this.enchantLevels[i] = 0;
                        }

                        return;
                    }

                    // CraftBukkit end
                    this.detectAndSendChanges();
                }
            }
            else
            {
                for (i = 0; i < 3; ++i)
                {
                    this.enchantLevels[i] = 0;
                }
            }
        }
    }

    public boolean enchantItem(EntityPlayer p_75140_1_, int p_75140_2_)
    {
        ItemStack itemstack = this.tableInventory.getStackInSlot(0);

        if (this.enchantLevels[p_75140_2_] > 0 && itemstack != null && (p_75140_1_.experienceLevel >= this.enchantLevels[p_75140_2_] || p_75140_1_.capabilities.isCreativeMode))
        {
            if (!this.worldPointer.isRemote)
            {
                List list = EnchantmentHelper.buildEnchantmentList(this.rand, itemstack, this.enchantLevels[p_75140_2_]);
                boolean flag = itemstack.getItem() == Items.book;

                if (list != null)
                {
                    // CraftBukkit start
                    Map<org.bukkit.enchantments.Enchantment, Integer> enchants = new java.util.HashMap<org.bukkit.enchantments.Enchantment, Integer>();

                    for (Object obj : list)
                    {
                        EnchantmentData instance = (EnchantmentData) obj;
                        enchants.put(org.bukkit.enchantments.Enchantment.getById(instance.enchantmentobj.effectId), instance.enchantmentLevel);
                    }

                    CraftItemStack item = CraftItemStack.asCraftMirror(itemstack);
                    EnchantItemEvent event = new EnchantItemEvent((Player) p_75140_1_.getBukkitEntity(), this.getBukkitView(), this.worldPointer.getWorld().getBlockAt(this.posX, this.posY, this.posZ), item, this.enchantLevels[p_75140_2_], enchants, p_75140_2_);
                    if (this.getBukkitView() != null) this.worldPointer.getServer().getPluginManager().callEvent(event); // Cauldron - allow vanilla mods to bypass
                    int level = event.getExpLevelCost();

                    if (event.isCancelled() || (level > p_75140_1_.experienceLevel && !p_75140_1_.capabilities.isCreativeMode) || enchants.isEmpty())
                    {
                        return false;
                    }

                    boolean applied = !flag;

                    for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : event.getEnchantsToAdd().entrySet())
                    {
                        try
                        {
                            if (flag)
                            {
                                int enchantId = entry.getKey().getId();

                                if (Enchantment.enchantmentsList[enchantId] == null)
                                {
                                    continue;
                                }

                                EnchantmentData enchantment = new EnchantmentData(enchantId, entry.getValue());
                                Items.enchanted_book.addEnchantment(itemstack, enchantment);
                                applied = true;
                                itemstack.func_150996_a(Items.enchanted_book);
                                break;
                            }
                            else
                            {
                                item.addEnchantment(entry.getKey(), entry.getValue());
                            }
                        }
                        catch (IllegalArgumentException e)
                        {
                            /* Just swallow invalid enchantments */
                        }
                    }

                    // Only down level if we've applied the enchantments
                    if (applied)
                    {
                        p_75140_1_.addExperienceLevel(-level);
                    }

                    // CraftBukkit end
                    this.onCraftMatrixChanged(this.tableInventory);
                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    public void onContainerClosed(EntityPlayer p_75134_1_)
    {
        super.onContainerClosed(p_75134_1_);

        if (!this.worldPointer.isRemote)
        {
            ItemStack itemstack = this.tableInventory.getStackInSlotOnClosing(0);

            if (itemstack != null)
            {
                p_75134_1_.dropPlayerItemWithRandomChoice(itemstack, false);
            }
        }
    }

    public boolean canInteractWith(EntityPlayer p_75145_1_)
    {
        if (!this.checkReachable)
        {
            return true;    // CraftBukkit
        }

        return this.worldPointer.getBlock(this.posX, this.posY, this.posZ) != Blocks.enchanting_table ? false : p_75145_1_.getDistanceSq((double)this.posX + 0.5D, (double)this.posY + 0.5D, (double)this.posZ + 0.5D) <= 64.0D;
    }

    public ItemStack transferStackInSlot(EntityPlayer p_82846_1_, int p_82846_2_)
    {
        ItemStack itemstack = null;
        Slot slot = (Slot)this.inventorySlots.get(p_82846_2_);

        if (slot != null && slot.getHasStack())
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (p_82846_2_ == 0)
            {
                if (!this.mergeItemStack(itemstack1, 1, 37, true))
                {
                    return null;
                }
            }
            else
            {
                if (((Slot)this.inventorySlots.get(0)).getHasStack() || !((Slot)this.inventorySlots.get(0)).isItemValid(itemstack1))
                {
                    return null;
                }

                if (itemstack1.hasTagCompound() && itemstack1.stackSize == 1)
                {
                    ((Slot)this.inventorySlots.get(0)).putStack(itemstack1.copy());
                    itemstack1.stackSize = 0;
                }
                else if (itemstack1.stackSize >= 1)
                {
                    ((Slot)this.inventorySlots.get(0)).putStack(new ItemStack(itemstack1.getItem(), 1, itemstack1.getItemDamage()));
                    --itemstack1.stackSize;
                }
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

            slot.onPickupFromSlot(p_82846_1_, itemstack1);
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

        CraftInventoryEnchanting inventory = new CraftInventoryEnchanting(this.tableInventory_CB);
        bukkitEntity = new CraftInventoryView(this.player, inventory, this);
        return bukkitEntity;
    }
    // CraftBukkit end
}