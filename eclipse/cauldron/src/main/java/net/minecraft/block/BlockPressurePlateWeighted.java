package net.minecraft.block;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

// CraftBukkit start
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import org.bukkit.event.entity.EntityInteractEvent;
// CraftBukkit end

public class BlockPressurePlateWeighted extends BlockBasePressurePlate
{
    private final int field_150068_a;
    private static final String __OBFID = "CL_00000334";

    protected BlockPressurePlateWeighted(String p_i45436_1_, Material p_i45436_2_, int p_i45436_3_)
    {
        super(p_i45436_1_, p_i45436_2_);
        this.field_150068_a = p_i45436_3_;
    }

    protected int func_150065_e(World p_150065_1_, int p_150065_2_, int p_150065_3_, int p_150065_4_)
    {
        // CraftBukkit start
        int l = 0;
        java.util.Iterator iterator = p_150065_1_.getEntitiesWithinAABB(Entity.class, this.func_150061_a(p_150065_2_, p_150065_3_, p_150065_4_)).iterator();

        while (iterator.hasNext())
        {
            Entity entity = (Entity) iterator.next();
            org.bukkit.event.Cancellable cancellable;

            if (entity instanceof EntityPlayer)
            {
                cancellable = org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerInteractEvent((EntityPlayer) entity, org.bukkit.event.block.Action.PHYSICAL, p_150065_2_, p_150065_3_, p_150065_4_, -1, null);
        }
        else
        {
                cancellable = new EntityInteractEvent(entity.getBukkitEntity(), p_150065_1_.getWorld().getBlockAt(p_150065_2_, p_150065_3_, p_150065_4_));
                p_150065_1_.getServer().getPluginManager().callEvent((EntityInteractEvent) cancellable);
            }

            // We only want to block turning the plate on if all events are cancelled
            if (!cancellable.isCancelled())
            {
                l++;
            }
        }

        l = Math.min(l, this.field_150068_a);
        // CraftBukkit end

        if (l <= 0)
        {
            return 0;
        }

            float f = (float)Math.min(this.field_150068_a, l) / (float)this.field_150068_a;
            return MathHelper.ceiling_float_int(f * 15.0F);
        }

    protected int func_150060_c(int p_150060_1_)
    {
        return p_150060_1_;
    }

    protected int func_150066_d(int p_150066_1_)
    {
        return p_150066_1_;
    }

    public int tickRate(World p_149738_1_)
    {
        return 10;
    }
}