package net.minecraft.dispenser;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

final class DispenserBehaviorFire extends BehaviorDefaultDispenseItem
{
    private boolean field_96466_b = true;

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    protected ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing enumfacing = BlockDispenser.getFacing(par1IBlockSource.getBlockMetadata());
        World world = par1IBlockSource.getWorld();
        int i = par1IBlockSource.getXInt() + enumfacing.getFrontOffsetX();
        int j = par1IBlockSource.getYInt() + enumfacing.getFrontOffsetY();
        int k = par1IBlockSource.getZInt() + enumfacing.getFrontOffsetZ();
        // CraftBukkit start
        org.bukkit.block.Block block = world.getWorld().getBlockAt(par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(par2ItemStack);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(0, 0, 0));

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

        if (world.isAirBlock(i, j, k))
        {
            // CraftBukkit start - Ignition by dispensing flint and steel
            if (!org.bukkit.craftbukkit.event.CraftEventFactory.callBlockIgniteEvent(world, i, j, k, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt()).isCancelled())
            {
                world.setBlock(i, j, k, Block.fire.blockID);

                if (par2ItemStack.attemptDamageItem(1, world.rand))
                {
                    par2ItemStack.stackSize = 0;
                }
            }
            // CraftBukkit end
        }
        else if (world.getBlockId(i, j, k) == Block.tnt.blockID)
        {
            Block.tnt.onBlockDestroyedByPlayer(world, i, j, k, 1);
            world.setBlockToAir(i, j, k);
        }
        else
        {
            this.field_96466_b = false;
        }

        return par2ItemStack;
    }

    /**
     * Play the dispense sound from the specified block.
     */
    protected void playDispenseSound(IBlockSource par1IBlockSource)
    {
        if (this.field_96466_b)
        {
            par1IBlockSource.getWorld().playAuxSFX(1000, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
        }
        else
        {
            par1IBlockSource.getWorld().playAuxSFX(1001, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
        }
    }
}