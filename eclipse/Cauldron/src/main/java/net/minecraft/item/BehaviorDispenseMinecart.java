package net.minecraft.item;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockRailBase;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
// CraftBukkit end

final class BehaviorDispenseMinecart extends BehaviorDefaultDispenseItem
{
    private final BehaviorDefaultDispenseItem behaviourDefaultDispenseItem = new BehaviorDefaultDispenseItem();

    /**
     * Dispense the specified stack, play the dispense sound and spawn particles.
     */
    public ItemStack dispenseStack(IBlockSource par1IBlockSource, ItemStack par2ItemStack)
    {
        EnumFacing enumfacing = BlockDispenser.getFacing(par1IBlockSource.getBlockMetadata());
        World world = par1IBlockSource.getWorld();
        double d0 = par1IBlockSource.getX() + (double)((float)enumfacing.getFrontOffsetX() * 1.125F);
        double d1 = par1IBlockSource.getY() + (double)((float)enumfacing.getFrontOffsetY() * 1.125F);
        double d2 = par1IBlockSource.getZ() + (double)((float)enumfacing.getFrontOffsetZ() * 1.125F);
        int i = par1IBlockSource.getXInt() + enumfacing.getFrontOffsetX();
        int j = par1IBlockSource.getYInt() + enumfacing.getFrontOffsetY();
        int k = par1IBlockSource.getZInt() + enumfacing.getFrontOffsetZ();
        int l = world.getBlockId(i, j, k);
        double d3;

        if (BlockRailBase.isRailBlock(l))
        {
            d3 = 0.0D;
        }
        else
        {
            if (l != 0 || !BlockRailBase.isRailBlock(world.getBlockId(i, j - 1, k)))
            {
                return this.behaviourDefaultDispenseItem.dispense(par1IBlockSource, par2ItemStack);
            }

            d3 = -1.0D;
        }

        // CraftBukkit start
        ItemStack itemstack1 = par2ItemStack.splitStack(1);
        org.bukkit.block.Block block = world.getWorld().getBlockAt(par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector(d0, d1 + d3, d2));

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
        EntityMinecart entityminecart = EntityMinecart.createMinecart(world, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ(), ((ItemMinecart) itemstack1.getItem()).minecartType);
        // CraftBukkit end

        if (par2ItemStack.hasDisplayName())
        {
            entityminecart.setMinecartName(par2ItemStack.getDisplayName());
        }

        world.spawnEntityInWorld(entityminecart);
        // itemstack.splitStack(1); // CraftBukkit - handled during event processing
        return par2ItemStack;
    }

    /**
     * Play the dispense sound from the specified block.
     */
    protected void playDispenseSound(IBlockSource par1IBlockSource)
    {
        par1IBlockSource.getWorld().playAuxSFX(1000, par1IBlockSource.getXInt(), par1IBlockSource.getYInt(), par1IBlockSource.getZInt(), 0);
    }
}