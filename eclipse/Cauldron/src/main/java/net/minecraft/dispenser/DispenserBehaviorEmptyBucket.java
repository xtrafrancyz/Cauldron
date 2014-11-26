package net.minecraft.dispenser;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

final class DispenserBehaviorEmptyBucket extends BehaviorDefaultDispenseItem
{
    private final BehaviorDefaultDispenseItem defaultDispenserItemBehavior = new BehaviorDefaultDispenseItem();

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing enumfacing = BlockDispenser.getFacing(par1IBlockSource.getBlockMetadata());
        World world = par1IBlockSource.getWorld();
        int i = par1IBlockSource.getXInt() + enumfacing.getFrontOffsetX();
        int j = par1IBlockSource.getYInt() + enumfacing.getFrontOffsetY();
        int k = par1IBlockSource.getZInt() + enumfacing.getFrontOffsetZ();
        Material material = world.getBlockMaterial(i, j, k);
        int l = world.getBlockMetadata(i, j, k);
        Item item;

        if (Material.water.equals(material) && l == 0)
        {
            item = Item.bucketWater;
        }
        else
        {
            if (!Material.lava.equals(material) || l != 0)
            {
                return super.dispenseStack(par1IBlockSource, par2ItemStack);
            }

            item = Item.bucketLava;
        }

        // CraftBukkit start
        org.bukkit.block.Block block = world.getWorld().getBlockAt(par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(par2ItemStack);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(i, j, k));

        if (!BlockDispenser.eventFired)
        {
            world.getServer().getPluginManager().callEvent(event);
        }

        if (event.isCancelled())
        {
            return par2ItemStack;
        }

        if (!event.getItem().equals(craftItem))
        {
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            IBehaviorDispenseItem ibehaviordispenseitem = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.getObject(eventStack.getItem());

            if (ibehaviordispenseitem != IBehaviorDispenseItem.itemDispenseBehaviorProvider && ibehaviordispenseitem != this)
            {
                ibehaviordispenseitem.dispense(par1IBlockSource, eventStack);
                return par2ItemStack;
            }
        }
        // CraftBukkit end
        world.setBlockToAir(i, j, k);

        if (--par2ItemStack.stackSize == 0)
        {
            par2ItemStack.itemID = item.itemID;
            par2ItemStack.stackSize = 1;
        }
        else if (((TileEntityDispenser)par1IBlockSource.getBlockTileEntity()).addItem(new ItemStack(item)) < 0)
        {
            this.defaultDispenserItemBehavior.dispense(par1IBlockSource, new ItemStack(item));
        }

        return par2ItemStack;
    }
}