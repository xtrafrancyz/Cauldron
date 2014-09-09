package net.minecraft.util;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.EnumDifficulty;
// CraftBukkit start
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
// CraftBukkit end

public class FoodStats
{
    // CraftBukkit start - All made public
    public int foodLevel = 20;
    public float foodSaturationLevel = 5.0F;
    public float foodExhaustionLevel;
    public int foodTimer;
    private EntityPlayer entityplayer;
    // CraftBukkit end
    private int prevFoodLevel = 20;
    private static final String __OBFID = "CL_00001729";

    // CraftBukkit start - added EntityPlayer constructor
    public FoodStats(EntityPlayer entityplayer)
    {
        org.apache.commons.lang.Validate.notNull(entityplayer);
        this.entityplayer = entityplayer;
    }
    // CraftBukkit end

    public void addStats(int p_75122_1_, float p_75122_2_)
    {
        this.foodLevel = Math.min(p_75122_1_ + this.foodLevel, 20);
        this.foodSaturationLevel = Math.min(this.foodSaturationLevel + (float)p_75122_1_ * p_75122_2_ * 2.0F, (float)this.foodLevel);
    }

    public void func_151686_a(ItemFood p_151686_1_, ItemStack p_151686_2_)
    {
        // CraftBukkit start
        int oldFoodLevel = foodLevel;
        org.bukkit.event.entity.FoodLevelChangeEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callFoodLevelChangeEvent(entityplayer, p_151686_1_.func_150905_g(p_151686_2_) + oldFoodLevel);

        if (!event.isCancelled())
        {
            this.addStats(event.getFoodLevel() - oldFoodLevel, p_151686_1_.func_150906_h(p_151686_2_));
        }

        ((EntityPlayerMP) entityplayer).playerNetServerHandler.sendPacket(new S06PacketUpdateHealth(((EntityPlayerMP) entityplayer).getBukkitEntity().getScaledHealth(), entityplayer.getFoodStats().foodLevel, entityplayer.getFoodStats().foodSaturationLevel));
        // CraftBukkit end
    }

    public void onUpdate(EntityPlayer p_75118_1_)
    {
        EnumDifficulty enumdifficulty = p_75118_1_.worldObj.difficultySetting;
        this.prevFoodLevel = this.foodLevel;

        if (this.foodExhaustionLevel > 4.0F)
        {
            this.foodExhaustionLevel -= 4.0F;

            if (this.foodSaturationLevel > 0.0F)
            {
                this.foodSaturationLevel = Math.max(this.foodSaturationLevel - 1.0F, 0.0F);
            }
            else if (enumdifficulty != EnumDifficulty.PEACEFUL)
            {
                // CraftBukkit start
                org.bukkit.event.entity.FoodLevelChangeEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callFoodLevelChangeEvent(p_75118_1_, Math.max(this.foodLevel - 1, 0));

                if (!event.isCancelled())
                {
                    this.foodLevel = event.getFoodLevel();
                }

                ((EntityPlayerMP) p_75118_1_).playerNetServerHandler.sendPacket(new S06PacketUpdateHealth(((EntityPlayerMP) p_75118_1_).getBukkitEntity().getScaledHealth(), this.foodLevel, this.foodSaturationLevel));
                // CraftBukkit end
            }
        }

        if (p_75118_1_.worldObj.getGameRules().getGameRuleBooleanValue("naturalRegeneration") && this.foodLevel >= 18 && p_75118_1_.shouldHeal())
        {
            ++this.foodTimer;

            if (this.foodTimer >= 80)
            {
                // CraftBukkit - added RegainReason
                p_75118_1_.heal(1.0F, org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.SATIATED);
                this.addExhaustion(3.0F);
                this.foodTimer = 0;
            }
        }
        else if (this.foodLevel <= 0)
        {
            ++this.foodTimer;

            if (this.foodTimer >= 80)
            {
                if (p_75118_1_.getHealth() > 10.0F || enumdifficulty == EnumDifficulty.HARD || p_75118_1_.getHealth() > 1.0F && enumdifficulty == EnumDifficulty.NORMAL)
                {
                    p_75118_1_.attackEntityFrom(DamageSource.starve, 1.0F);
                }

                this.foodTimer = 0;
            }
        }
        else
        {
            this.foodTimer = 0;
        }
    }

    public void readNBT(NBTTagCompound p_75112_1_)
    {
        if (p_75112_1_.hasKey("foodLevel", 99))
        {
            this.foodLevel = p_75112_1_.getInteger("foodLevel");
            this.foodTimer = p_75112_1_.getInteger("foodTickTimer");
            this.foodSaturationLevel = p_75112_1_.getFloat("foodSaturationLevel");
            this.foodExhaustionLevel = p_75112_1_.getFloat("foodExhaustionLevel");
        }
    }

    public void writeNBT(NBTTagCompound p_75117_1_)
    {
        p_75117_1_.setInteger("foodLevel", this.foodLevel);
        p_75117_1_.setInteger("foodTickTimer", this.foodTimer);
        p_75117_1_.setFloat("foodSaturationLevel", this.foodSaturationLevel);
        p_75117_1_.setFloat("foodExhaustionLevel", this.foodExhaustionLevel);
    }

    public int getFoodLevel()
    {
        return this.foodLevel;
    }

    @SideOnly(Side.CLIENT)
    public int getPrevFoodLevel()
    {
        return this.prevFoodLevel;
    }

    public boolean needFood()
    {
        return this.foodLevel < 20;
    }

    public void addExhaustion(float p_75113_1_)
    {
        this.foodExhaustionLevel = Math.min(this.foodExhaustionLevel + p_75113_1_, 40.0F);
    }

    public float getSaturationLevel()
    {
        return this.foodSaturationLevel;
    }

    @SideOnly(Side.CLIENT)
    public void setFoodLevel(int p_75114_1_)
    {
        this.foodLevel = p_75114_1_;
    }

    @SideOnly(Side.CLIENT)
    public void setFoodSaturationLevel(float p_75119_1_)
    {
        this.foodSaturationLevel = p_75119_1_;
    }
}