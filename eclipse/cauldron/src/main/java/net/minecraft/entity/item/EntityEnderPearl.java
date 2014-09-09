package net.minecraft.entity.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

// CraftBukkit start
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.player.PlayerTeleportEvent;
// CraftBukkit end

public class EntityEnderPearl extends EntityThrowable
{
    private static final String __OBFID = "CL_00001725";

    public EntityEnderPearl(World p_i1782_1_)
    {
        super(p_i1782_1_);
    }

    public EntityEnderPearl(World p_i1783_1_, EntityLivingBase p_i1783_2_)
    {
        super(p_i1783_1_, p_i1783_2_);
    }

    @SideOnly(Side.CLIENT)
    public EntityEnderPearl(World p_i1784_1_, double p_i1784_2_, double p_i1784_4_, double p_i1784_6_)
    {
        super(p_i1784_1_, p_i1784_2_, p_i1784_4_, p_i1784_6_);
    }

    protected void onImpact(MovingObjectPosition p_70184_1_)
    {
        if (p_70184_1_.entityHit != null)
        {
            p_70184_1_.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 0.0F);
        }

        for (int i = 0; i < 32; ++i)
        {
            this.worldObj.spawnParticle("portal", this.posX, this.posY + this.rand.nextDouble() * 2.0D, this.posZ, this.rand.nextGaussian(), 0.0D, this.rand.nextGaussian());
        }

        if (!this.worldObj.isRemote)
        {
            if (this.getThrower() != null && this.getThrower() instanceof EntityPlayerMP)
            {
                EntityPlayerMP entityplayermp = (EntityPlayerMP)this.getThrower();

                if (entityplayermp.playerNetServerHandler.func_147362_b().isChannelOpen() && entityplayermp.worldObj == this.worldObj)
                {
                    EnderTeleportEvent event = new EnderTeleportEvent(entityplayermp, this.posX, this.posY, this.posZ, 5);
                    // Cauldron start - invert condition; return if cancelled otherwise fall through to CB event
                    if (MinecraftForge.EVENT_BUS.post(event)){
                        this.setDead();
                        return;
                    }
                    // Cauldron end
                    // CraftBukkit start
                    org.bukkit.craftbukkit.entity.CraftPlayer player = entityplayermp.getBukkitEntity();
                    org.bukkit.Location location = getBukkitEntity().getLocation();
                    location.setPitch(player.getLocation().getPitch());
                    location.setYaw(player.getLocation().getYaw());
                    PlayerTeleportEvent teleEvent = new PlayerTeleportEvent(player, player.getLocation(), location, PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
                    Bukkit.getPluginManager().callEvent(teleEvent);

                    if (!teleEvent.isCancelled() && !entityplayermp.playerNetServerHandler.isDisconnected())
                    {
                        entityplayermp.playerNetServerHandler.teleport(teleEvent.getTo());
                        this.getThrower().fallDistance = 0.0F;
                        CraftEventFactory.entityDamage = this;
                        this.getThrower().attackEntityFrom(DamageSource.fall, 5.0F);
                        CraftEventFactory.entityDamage = null;
                    }

                    // CraftBukkit end
                }
            }

            this.setDead();
        }
    }
}