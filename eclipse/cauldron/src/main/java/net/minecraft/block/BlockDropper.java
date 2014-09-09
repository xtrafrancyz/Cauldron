package net.minecraft.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityDropper;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.Facing;
import net.minecraft.world.World;
// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import net.minecraft.inventory.InventoryLargeChest;
// CraftBukkit end

public class BlockDropper extends BlockDispenser
{
    private final IBehaviorDispenseItem field_149947_P = new BehaviorDefaultDispenseItem();
    private static final String __OBFID = "CL_00000233";

    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister p_149651_1_)
    {
        this.blockIcon = p_149651_1_.registerIcon("furnace_side");
        this.field_149944_M = p_149651_1_.registerIcon("furnace_top");
        this.field_149945_N = p_149651_1_.registerIcon(this.getTextureName() + "_front_horizontal");
        this.field_149946_O = p_149651_1_.registerIcon(this.getTextureName() + "_front_vertical");
    }

    protected IBehaviorDispenseItem func_149940_a(ItemStack p_149940_1_)
    {
        return this.field_149947_P;
    }

    public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
    {
        return new TileEntityDropper();
    }

    public void func_149941_e(World p_149941_1_, int p_149941_2_, int p_149941_3_, int p_149941_4_)   // CraftBukkit - protected -> public
    {
        BlockSourceImpl blocksourceimpl = new BlockSourceImpl(p_149941_1_, p_149941_2_, p_149941_3_, p_149941_4_);
        TileEntityDispenser tileentitydispenser = (TileEntityDispenser)blocksourceimpl.getBlockTileEntity();

        if (tileentitydispenser != null)
        {
            int l = tileentitydispenser.func_146017_i();

            if (l < 0)
            {
                p_149941_1_.playAuxSFX(1001, p_149941_2_, p_149941_3_, p_149941_4_, 0);
            }
            else
            {
                ItemStack itemstack = tileentitydispenser.getStackInSlot(l);
                int i1 = p_149941_1_.getBlockMetadata(p_149941_2_, p_149941_3_, p_149941_4_) & 7;
                IInventory iinventory = TileEntityHopper.func_145893_b(p_149941_1_, (double)(p_149941_2_ + Facing.offsetsXForSide[i1]), (double)(p_149941_3_ + Facing.offsetsYForSide[i1]), (double)(p_149941_4_ + Facing.offsetsZForSide[i1]));
                ItemStack itemstack1;

                if (iinventory != null)
                {
                    // CraftBukkit start - Fire event when pushing items into other inventories
                    CraftItemStack oitemstack = CraftItemStack.asCraftMirror(itemstack.copy().splitStack(1));
                    org.bukkit.inventory.Inventory destinationInventory;

                    // Have to special case large chests as they work oddly
                    if (iinventory instanceof InventoryLargeChest)
                    {
                        destinationInventory = new org.bukkit.craftbukkit.inventory.CraftInventoryDoubleChest((InventoryLargeChest) iinventory);
                    }
                    else
                    {
                        destinationInventory = iinventory.getOwner().getInventory();
                    }

                    InventoryMoveItemEvent event = new InventoryMoveItemEvent(tileentitydispenser.getOwner().getInventory(), oitemstack.clone(), destinationInventory, true);
                    p_149941_1_.getServer().getPluginManager().callEvent(event);

                    if (event.isCancelled())
                    {
                        return;
                    }

                    itemstack1 = TileEntityHopper.func_145889_a(iinventory, CraftItemStack.asNMSCopy(event.getItem()), Facing.oppositeSide[i1]);

                    if (event.getItem().equals(oitemstack) && itemstack1 == null)
                    {
                        // CraftBukkit end
                        itemstack1 = itemstack.copy();

                        if (--itemstack1.stackSize == 0)
                        {
                            itemstack1 = null;
                        }
                    }
                    else
                    {
                        itemstack1 = itemstack.copy();
                    }
                }
                else
                {
                    itemstack1 = this.field_149947_P.dispense(blocksourceimpl, itemstack);

                    if (itemstack1 != null && itemstack1.stackSize == 0)
                    {
                        itemstack1 = null;
                    }
                }

                tileentitydispenser.setInventorySlotContents(l, itemstack1);
            }
        }
    }
}