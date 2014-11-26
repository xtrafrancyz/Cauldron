package net.minecraft.dispenser;

import java.util.Random;
import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

final class DispenserBehaviorFireball extends BehaviorDefaultDispenseItem
{
    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing enumfacing = BlockDispenser.getFacing(par1IBlockSource.getBlockMetadata());
        IPosition iposition = BlockDispenser.getIPositionFromBlockSource(par1IBlockSource);
        double d0 = iposition.getX() + (double)((float)enumfacing.getFrontOffsetX() * 0.3F);
        double d1 = iposition.getY() + (double)((float)enumfacing.getFrontOffsetX() * 0.3F);
        double d2 = iposition.getZ() + (double)((float)enumfacing.getFrontOffsetZ() * 0.3F);
        World world = par1IBlockSource.getWorld();
        Random random = world.rand;
        double d3 = random.nextGaussian() * 0.05D + (double)enumfacing.getFrontOffsetX();
        double d4 = random.nextGaussian() * 0.05D + (double)enumfacing.getFrontOffsetY();
        double d5 = random.nextGaussian() * 0.05D + (double)enumfacing.getFrontOffsetZ();
        // CraftBukkit start
        ItemStack itemstack1 = par2ItemStack.splitStack(1);
        org.bukkit.block.Block block = world.getWorld().getBlockAt(par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(d3, d4, d5));

        if (!BlockDispenser.eventFired)
        {
            world.getServer().getPluginManager().callEvent(event);
        }

        if (event.isCancelled())
        {
            par2ItemStack.stackSize++;
            return par2ItemStack;
        }

        if (!event.getItem().equals(craftItem))
        {
            par2ItemStack.stackSize++;
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            IBehaviorDispenseItem ibehaviordispenseitem = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.getObject(eventStack.getItem());

            if (ibehaviordispenseitem != IBehaviorDispenseItem.itemDispenseBehaviorProvider && ibehaviordispenseitem != this)
            {
                ibehaviordispenseitem.dispense(par1IBlockSource, eventStack);
                return par2ItemStack;
            }
        }

        world.spawnEntityInWorld(new EntitySmallFireball(world, d0, d1, d2, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ()));
        // itemstack.a(1); // Handled during event processing
        // CraftBukkit end
        return par2ItemStack;
    }

    /**
     * Play the dispense sound from the specified block.
     */
    protected void playDispenseSound(IBlockSource par1IBlockSource)
    {
        par1IBlockSource.getWorld().playAuxSFX(1009, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
    }
}