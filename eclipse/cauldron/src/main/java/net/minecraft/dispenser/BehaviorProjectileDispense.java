package net.minecraft.dispenser;

import net.minecraft.block.BlockDispenser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IProjectile;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

// CraftBukkit start
import net.minecraft.tileentity.TileEntityDispenser;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockDispenseEvent;
// CraftBukkit end

public abstract class BehaviorProjectileDispense extends BehaviorDefaultDispenseItem
{
    private static final String __OBFID = "CL_00001394";

    public ItemStack dispenseStack(IBlockSource p_82487_1_, ItemStack p_82487_2_)
    {
        World world = p_82487_1_.getWorld();
        IPosition iposition = BlockDispenser.func_149939_a(p_82487_1_);
        EnumFacing enumfacing = BlockDispenser.func_149937_b(p_82487_1_.getBlockMetadata());
        IProjectile iprojectile = this.getProjectileEntity(world, iposition);
        // CraftBukkit start
        ItemStack itemstack1 = p_82487_2_.splitStack(1);
        org.bukkit.block.Block block = world.getWorld().getBlockAt(p_82487_1_.getXInt(), p_82487_1_.getYInt(), p_82487_1_.getZInt());
        CraftItemStack craftItem = CraftItemStack.asCraftMirror(itemstack1);
        BlockDispenseEvent event = new BlockDispenseEvent(block, craftItem.clone(), new org.bukkit.util.Vector((double) enumfacing.getFrontOffsetX(), (double)((float) enumfacing.getFrontOffsetY() + 0.1F), (double) enumfacing.getFrontOffsetZ()));

        if (!BlockDispenser.eventFired)
        {
            world.getServer().getPluginManager().callEvent(event);
        }

        if (event.isCancelled())
        {
            p_82487_2_.stackSize++;
            return p_82487_2_;
        }

        if (!event.getItem().equals(craftItem))
        {
            p_82487_2_.stackSize++;
            // Chain to handler for new item
            ItemStack eventStack = CraftItemStack.asNMSCopy(event.getItem());
            IBehaviorDispenseItem ibehaviordispenseitem = (IBehaviorDispenseItem) BlockDispenser.dispenseBehaviorRegistry.getObject(eventStack.getItem());

            if (ibehaviordispenseitem != IBehaviorDispenseItem.itemDispenseBehaviorProvider && ibehaviordispenseitem != this)
            {
                ibehaviordispenseitem.dispense(p_82487_1_, eventStack);
                return p_82487_2_;
            }
        }

        iprojectile.setThrowableHeading((double)enumfacing.getFrontOffsetX(), (double)((float)enumfacing.getFrontOffsetY() + 0.1F), (double)enumfacing.getFrontOffsetZ(), this.func_82500_b(), this.func_82498_a());
        ((Entity) iprojectile).projectileSource = new org.bukkit.craftbukkit.projectiles.CraftBlockProjectileSource((TileEntityDispenser) p_82487_1_.getBlockTileEntity());
        // CraftBukkit end
        world.spawnEntityInWorld((Entity)iprojectile);
        // p_82487_2_.splitStack(1); // CraftBukkit - Handled during event processing
        return p_82487_2_;
    }

    protected void playDispenseSound(IBlockSource p_82485_1_)
    {
        p_82485_1_.getWorld().playAuxSFX(1002, p_82485_1_.getXInt(), p_82485_1_.getYInt(), p_82485_1_.getZInt(), 0);
    }

    protected abstract IProjectile getProjectileEntity(World p_82499_1_, IPosition p_82499_2_);

    protected float func_82498_a()
    {
        return 6.0F;
    }

    protected float func_82500_b()
    {
        return 1.1F;
    }
}