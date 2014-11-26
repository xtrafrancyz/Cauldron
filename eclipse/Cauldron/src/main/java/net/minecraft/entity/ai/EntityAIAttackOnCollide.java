package net.minecraft.entity.ai;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.bukkit.event.entity.EntityTargetEvent; // CraftBukkit

public class EntityAIAttackOnCollide extends EntityAIBase
{
    World worldObj;
    EntityCreature attacker;

    /**
     * An amount of decrementing ticks that allows the entity to attack once the tick reaches 0.
     */
    int attackTick;

    /** The speed with which the mob will approach the target */
    double speedTowardsTarget;

    /**
     * When true, the mob will continue chasing its target, even if it can't find a path to them right now.
     */
    boolean longMemory;

    /** The PathEntity of our entity. */
    PathEntity entityPathEntity;
    Class classTarget;
    private int field_75445_i;
    // Spigot start
    private double pathX;
    private double pathY;
    private double pathZ;
    private boolean prevPathOK;
    // Spigot end
    private int failedPathFindingPenalty;

    public EntityAIAttackOnCollide(EntityCreature par1EntityCreature, Class par2Class, double par3, boolean par5)
    {
        this(par1EntityCreature, par3, par5);
        this.classTarget = par2Class;
    }

    public EntityAIAttackOnCollide(EntityCreature par1EntityCreature, double par2, boolean par4)
    {
        this.attacker = par1EntityCreature;
        this.worldObj = par1EntityCreature.worldObj;
        this.speedTowardsTarget = par2;
        this.longMemory = par4;
        this.setMutexBits(3);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        EntityLivingBase entitylivingbase = this.attacker.getAttackTarget();

        if (entitylivingbase == null)
        {
            return false;
        }
        else if (!entitylivingbase.isEntityAlive())
        {
            return false;
        }
        else if (this.classTarget != null && !this.classTarget.isAssignableFrom(entitylivingbase.getClass()))
        {
            return false;
        }
        else
        {
            if (-- this.field_75445_i <= 0)
            {
                this.entityPathEntity = this.attacker.getNavigator().getPathToEntityLiving(entitylivingbase);
                this.field_75445_i = 4 + this.attacker.getRNG().nextInt(7);
                return this.entityPathEntity != null;
            }
            else
            {
                return true;
            }
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean continueExecuting()
    {
        EntityLivingBase entitylivingbase = this.attacker.getAttackTarget();
        // CraftBukkit start
        EntityTargetEvent.TargetReason reason = this.attacker.getAttackTarget() == null ? EntityTargetEvent.TargetReason.FORGOT_TARGET : EntityTargetEvent.TargetReason.TARGET_DIED;

        if (this.attacker.getAttackTarget() == null || (this.attacker.getAttackTarget() != null && !this.attacker.getAttackTarget().isEntityAlive()))
        {
            org.bukkit.craftbukkit.event.CraftEventFactory.callEntityTargetEvent(attacker, null, reason);
        }

        // CraftBukkit end
        return entitylivingbase == null ? false : (!entitylivingbase.isEntityAlive() ? false : (!this.longMemory ? !this.attacker.getNavigator().noPath() : this.attacker.func_110176_b(MathHelper.floor_double(entitylivingbase.posX), MathHelper.floor_double(entitylivingbase.posY), MathHelper.floor_double(entitylivingbase.posZ))));
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    public void startExecuting()
    {
        this.attacker.getNavigator().setPath(this.entityPathEntity, this.speedTowardsTarget);
        this.field_75445_i = 0;
    }

    /**
     * Resets the task
     */
    public void resetTask()
    {
        this.attacker.getNavigator().clearPathEntity();
    }

    /**
     * Updates the task
     */
    public void updateTask()
    {
        EntityLivingBase entitylivingbase = this.attacker.getAttackTarget();
        this.attacker.getLookHelper().setLookPositionWithEntity(entitylivingbase, 30.0F, 30.0F);
        double goalDistanceSq = this.attacker.getDistanceSq(entitylivingbase.posX, entitylivingbase.boundingBox.minY, entitylivingbase.posZ); // Spigot

        if ((this.longMemory || this.attacker.getEntitySenses().canSee(entitylivingbase)) && --this.field_75445_i <= 0)
        {
            // Spigot start
            double targetMovement = entitylivingbase.getDistanceSq(this.pathX, this.pathY, this.pathZ);
            // If this is true, then we are re-pathing
            if (this.field_75445_i <= 0 && targetMovement >= 1.0D || this.field_75445_i <= 0 && this.attacker.getRNG().nextInt(200) == 0) /* EntityCreature random instance */
            {
                net.minecraft.entity.ai.attributes.AttributeInstance rangeAttr = this.attacker.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.followRange);
                double origRange = rangeAttr.getAttributeValue();

                if (this.failedPathFindingPenalty > 0)
                {
                    double dist = Math.sqrt(goalDistanceSq);

                    if (dist <= 8.0D)
                    {
                        dist = 8.0D;
                    }

                    if (dist > origRange)
                    {
                        dist = origRange;
                    }

                    rangeAttr.setAttribute(dist);
                }

                this.prevPathOK = this.attacker.getNavigator().tryMoveToEntityLiving(entitylivingbase, this.speedTowardsTarget);

                if (this.failedPathFindingPenalty > 0)
                {
                    --this.failedPathFindingPenalty;

                    if (origRange > 40.0D)
                    {
                        origRange = 40.0D;
                    }

                    rangeAttr.setAttribute(origRange);
                }

                this.pathX = entitylivingbase.posX;
                this.pathY = entitylivingbase.boundingBox.minY;
                this.pathZ = entitylivingbase.posZ;
                this.field_75445_i = 4 + this.attacker.getRNG().nextInt(7); /* EntityCreature random instance */

                if (goalDistanceSq > 256.0D)
                {
                    if (goalDistanceSq > 1024.0D)
                    {
                        this.field_75445_i += 8;
                    }
                    else
                    {
                        this.field_75445_i += 16;
                    }
                }
                else if (!this.prevPathOK)
                {
                    this.field_75445_i += 24;
                }

                if ((!this.prevPathOK || goalDistanceSq <= 256.0D) && this.failedPathFindingPenalty <= 0)
                {
                    this.failedPathFindingPenalty = 4 + this.attacker.getRNG().nextInt(4); /* EntityCreature random instance */
                }
            }
        }
        // Spigot end
        this.attackTick = Math.max(this.attackTick - 1, 0);
        double d0 = (double)(this.attacker.width * 2.0F * this.attacker.width * 2.0F + entitylivingbase.width);

        if (goalDistanceSq <= d0 && this.attackTick <= 0) // Spigot
        {
            this.attackTick = 20;

            if (this.attacker.getHeldItem() != null)
            {
                this.attacker.swingItem();
            }

            this.attacker.attackEntityAsMob(entitylivingbase);
        }
    }
}