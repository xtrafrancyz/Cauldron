package net.minecraft.entity.projectile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
// CraftBukkit start
import java.util.HashMap;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import net.minecraft.entity.player.EntityPlayerMP;
// CraftBukkit end

public class EntityPotion extends EntityThrowable
{
    /**
     * The damage value of the thrown potion that this EntityPotion represents.
     */
    public ItemStack potionDamage; // CraftBukkit private --> public

    public EntityPotion(World par1World)
    {
        super(par1World);
    }

    public EntityPotion(World par1World, EntityLivingBase par2EntityLivingBase, int par3)
    {
        this(par1World, par2EntityLivingBase, new ItemStack(Item.potion, 1, par3));
    }

    public EntityPotion(World par1World, EntityLivingBase par2EntityLivingBase, ItemStack par3ItemStack)
    {
        super(par1World, par2EntityLivingBase);
        this.potionDamage = par3ItemStack;
    }

    @SideOnly(Side.CLIENT)
    public EntityPotion(World par1World, double par2, double par4, double par6, int par8)
    {
        this(par1World, par2, par4, par6, new ItemStack(Item.potion, 1, par8));
    }

    public EntityPotion(World par1World, double par2, double par4, double par6, ItemStack par8ItemStack)
    {
        super(par1World, par2, par4, par6);
        this.potionDamage = par8ItemStack;
    }

    /**
     * Gets the amount of gravity to apply to the thrown entity with each tick.
     */
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

    public void setPotionDamage(int par1)
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }

        this.potionDamage.setItemDamage(par1);
    }

    /**
     * Returns the damage value of the thrown potion that this EntityPotion represents.
     */
    public int getPotionDamage()
    {
        if (this.potionDamage == null)
        {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }

        return this.potionDamage.getItemDamage();
    }

    /**
     * Called when this EntityThrowable hits a block or entity.
     */
    protected void onImpact(MovingObjectPosition par1MovingObjectPosition)
    {
        if (!this.worldObj.isRemote)
        {
            List list = Item.potion.getEffects(this.potionDamage);

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

                            if (entitylivingbase == par1MovingObjectPosition.entityHit)
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

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readEntityFromNBT(par1NBTTagCompound);

        if (par1NBTTagCompound.hasKey("Potion"))
        {
            this.potionDamage = ItemStack.loadItemStackFromNBT(par1NBTTagCompound.getCompoundTag("Potion"));
        }
        else
        {
            this.setPotionDamage(par1NBTTagCompound.getInteger("potionValue"));
        }

        if (this.potionDamage == null)
        {
            this.setDead();
        }
    }

    /**
     * (abstract) Protected helper method to write subclass entity data to NBT.
     */
    public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeEntityToNBT(par1NBTTagCompound);

        if (this.potionDamage != null)
        {
            par1NBTTagCompound.setCompoundTag("Potion", this.potionDamage.writeToNBT(new NBTTagCompound()));
        }
    }
}