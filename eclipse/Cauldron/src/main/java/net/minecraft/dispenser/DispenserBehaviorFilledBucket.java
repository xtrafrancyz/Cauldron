package net.minecraft.dispenser;

import net.minecraft.block.BlockDispenser;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.world.World;
// CraftBukkit end

final class DispenserBehaviorFilledBucket extends BehaviorDefaultDispenseItem
{
    private final BehaviorDefaultDispenseItem defaultDispenserItemBehavior = new BehaviorDefaultDispenseItem();

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        ItemBucket itembucket = (ItemBucket)par2ItemStack.getItem();
        int i = par1IBlockSource.getXInt();
        int j = par1IBlockSource.getYInt();
        int k = par1IBlockSource.getZInt();
        EnumFacing enumfacing = BlockDispenser.getFacing(par1IBlockSource.getBlockMetadata());
        // CraftBukkit start
        World world = par1IBlockSource.getWorld();
        int x = i + enumfacing.getFrontOffsetX();
        int y = j + enumfacing.getFrontOffsetY();
        int z = k + enumfacing.getFrontOffsetZ();

        if (world.isAirBlock(x, y, z) || !world.getBlockMaterial(x, y, z).isSolid())
        {
            org.bukkit.block.Block block = world.getWorld().getBlockAt(i, j, k);
            CraftItemStack craftItem = CraftItemStack.asCraftMirror(par2ItemStack);
            BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(x, y, z));

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

            itembucket = (ItemBucket) CraftItemStack.asNMSCopy(event.getItem()).getItem();
        }

        // CraftBukkit end

        if (itembucket.tryPlaceContainedLiquid(par1IBlockSource.getWorld(), i + enumfacing.getFrontOffsetX(), j + enumfacing.getFrontOffsetY(), k + enumfacing.getFrontOffsetZ()))
        {
            // CraftBukkit start - Handle stacked buckets
            Item item = Item.bucketEmpty;

            if (--par2ItemStack.stackSize == 0)
            {
                par2ItemStack.itemID = item.itemID;
                par2ItemStack.stackSize = 1;
            }
            else if (((TileEntityDispenser) par1IBlockSource.getBlockTileEntity()).addItem(new ItemStack(item)) < 0)
            {
                this.defaultDispenserItemBehavior.dispense(par1IBlockSource, new ItemStack(item));
            }

            // CraftBukkit end
            return par2ItemStack;
        }
        else
        {
            return this.defaultDispenserItemBehavior.dispense(par1IBlockSource, par2ItemStack);
        }
    }
}