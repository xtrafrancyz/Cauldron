package net.minecraft.dispenser;

import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import net.minecraft.world.World;
// CraftBukkit end

final class DispenserBehaviorFireworks extends BehaviorDefaultDispenseItem
{
    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing enumfacing = BlockDispenser.getFacing(par1IBlockSource.getBlockMetadata());
        double d0 = par1IBlockSource.getX() + (double)enumfacing.getFrontOffsetX();
        double d1 = (double)((float)par1IBlockSource.getYInt() + 0.2F);
        double d2 = par1IBlockSource.getZ() + (double)enumfacing.getFrontOffsetZ();
        // CraftBukkit start
        World world = par1IBlockSource.getWorld();
        ItemStack itemstack1 = par2ItemStack.splitStack(1);
        org.bukkit.block.Block block = world.getWorld().getBlockAt(par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(d0, d1, d2));

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

        itemstack1 = CraftItemStack.asNMSCopy(event.getItem());
        EntityFireworkRocket entityfireworkrocket = new EntityFireworkRocket(par1IBlockSource.getWorld(), event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ(), itemstack1);
        par1IBlockSource.getWorld().spawnEntityInWorld(entityfireworkrocket);
        // itemstack.a(1); // Handled during event processing
        // CraftBukkit end
        return par2ItemStack;
    }

    /**
     * Play the dispense sound from the specified block.
     */
    protected void playDispenseSound(IBlockSource par1IBlockSource)
    {
        par1IBlockSource.getWorld().playAuxSFX(1002, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
    }
}