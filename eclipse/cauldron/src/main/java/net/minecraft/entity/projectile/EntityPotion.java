package net.minecraft.entity.projectile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

// CraftBukkit start
import java.util.HashMap;
import net.minecraft.entity.player.EntityPlayerMP;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
// CraftBukkit end

public class EntityPotion extends EntityThrowable
{
    public ItemStack potionDamage; // CraftBukkit private --> public
    private static final String __OBFID = "CL_00001727";

    public EntityPotion(World p_i1788_1_)
    {
        super(p_i1788_1_);
    }

    public EntityPotion(World p_i1789_1_, EntityLivingBase p_i1789_2_, int p_i1789_3_)
    {
        this(p_i1789_1_, p_i1789_2_, new ItemStack(Items.potionitem, 1, p_i1789_3_));
    }

    public EntityPotion(World p_i1790_1_, EntityLivingBase p_i1790_2_, ItemStack p_i1790_3_)
    {
        super(p_i1790_1_, p_i1790_2_);
        this.potionDamage = p_i1790_3_;
    }

    @SideOnly(Side.CLIENT)
    public EntityPotion(World p_i1791_1_, double p_i1791_2_, double p_i1791_4_, double p_i1791_6_, int p_i1791_8_)
    {
        this(p_i1791_1_, p_i1791_2_, p_i1791_4_, p_i1791_6_, new ItemStack(Items.potionitem, 1, p_i1791_8_));
    }

    public EntityPotion(World p_i1792_1_, double p_i1792_2_, double p_i1792_4_, double p_i1792_6_, ItemStack p_i1792_8_)
    {
        super(p_i1792_1_, p_i1792_2_, p_i1792_4_, p_i1792_6_);
        this.potionDamage = p_i1792_8_;
    }

    protected float getGravityVelocity()
    {
        return 0.05F;
    }

    protected float func_70182_d()
    {
        return 0.5F;
    }

    protected float func_70183_g()
    {
        return -20.0F;
    }

    public void setPotionDamage(int p_82340_1_)
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Items.potionitem, 1, 0);
        }

        this.potionDamage.setItemDamage(p_82340_1_);
    }

    public int getPotionDamage()
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Items.potionitem, 1, 0);
        }

        return this.potionDamage.getItemDamage();
    }

    protected void onImpact(MovingObjectPosition p_70184_1_)
    {
        if (!this.worldObj.isRemote)
        {
            List list = Items.potionitem.getEffects(this.potionDamage);

            if (true || list != null && !list.isEmpty())   // CraftBukkit - Call event even if no effects to apply
            {
                AxisAlignedBB axisalignedbb = this.boundingBox.expand(4.0D, 2.0D, 4.0D);
                List list1 = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, axisalignedbb);

                if (list1 != null)   // CraftBukkit - Run code even if there are no entities around
                {
                    Iterator iterator = list1.iterator();
                    // CraftBukkit
                    HashMap<LivingEntity, Double> affected = new HashMap<LivingEntity, Double>();

                    while (iterator.hasNext())
                    {
                        EntityLivingBase entitylivingbase = (EntityLivingBase)iterator.next();
                        double d0 = this.getDistanceSqToEntity(entitylivingbase);

                        if (d0 < 16.0D)
                        {
                            double d1 = 1.0D - Math.sqrt(d0) / 4.0D;

                            if (entitylivingbase == p_70184_1_.entityHit)
                            {
                                d1 = 1.0D;
                            }

                            // CraftBukkit start
                            affected.put((LivingEntity) entitylivingbase.getBukkitEntity(), d1);
                        }
                    }

                    org.bukkit.event.entity.PotionSplashEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callPotionSplashEvent(this, affected);

                    if (!event.isCancelled() && list != null && !list.isEmpty())   // do not process effects if there are no effects to process
                    {
                        for (LivingEntity victim : event.getAffectedEntities())
                        {
                            if (!(victim instanceof CraftLivingEntity))
                            {
                                continue;
                            }

                            EntityLivingBase entitylivingbase = ((CraftLivingEntity) victim).getHandle();
                            double d1 = event.getIntensity(victim);
                            // CraftBukkit end
                            Iterator iterator1 = list.iterator();

                            while (iterator1.hasNext())
                            {
                                PotionEffect potioneffect = (PotionEffect)iterator1.next();
                                int i = potioneffect.getPotionID();

                                // CraftBukkit start - Abide by PVP settings - for players only!
                                if (!this.worldObj.pvpMode && this.getThrower() instanceof EntityPlayerMP && entitylivingbase instanceof EntityPlayerMP && entitylivingbase != this.getThrower())
                                {
                                    // Block SLOWER_MOVEMENT, SLOWER_DIG, HARM, BLINDNESS, HUNGER, WEAKNESS and POISON potions
                                    if (i == 2 || i == 4 || i == 7 || i == 15 || i == 17 || i == 18 || i == 19)
                                    {
                                        continue;
                                    }
                                }

                                // CraftBukkit end

                                if (Potion.potionTypes[i].isInstant())
                                {
                                    // CraftBukkit - Added 'this'
                                    Potion.potionTypes[i].applyInstantEffect(this.getThrower(), entitylivingbase, potioneffect.getAmplifier(), d1, this);
                                }
                                else
                                {
                                    int j = (int)(d1 * (double)potioneffect.getDuration() + 0.5D);

                                    if (j > 20)
                                    {
                                        entitylivingbase.addPotionEffect(new PotionEffect(i, j, potioneffect.getAmplifier()));
                                    }
                                }
                            }
                        }
                    }
                }
            }

            this.worldObj.playAuxSFX(2002, (int)Math.round(this.posX), (int)Math.round(this.posY), (int)Math.round(this.posZ), this.getPotionDamage());
            this.setDead();
        }
    }

    public void readEntityFromNBT(NBTTagCompound p_70037_1_)
    {
        super.readEntityFromNBT(p_70037_1_);

        if (p_70037_1_.hasKey("Potion", 10))
        {
            this.potionDamage = ItemStack.loadItemStackFromNBT(p_70037_1_.getCompoundTag("Potion"));
        }
        else
        {
            this.setPotionDamage(p_70037_1_.getInteger("potionValue"));
        }

        if (this.potionDamage == null)
        {
            this.setDead();
        }
    }

    public void writeEntityToNBT(NBTTagCompound p_70014_1_)
    {
        super.writeEntityToNBT(p_70014_1_);

        if (this.potionDamage != null)
        {
            p_70014_1_.setTag("Potion", this.potionDamage.writeToNBT(new NBTTagCompound()));
        }
    }
}