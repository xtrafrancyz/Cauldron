package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event;
import net.minecraftforge.event.entity.player.FillBucketEvent;
// CraftBukkit start
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
// CraftBukkit end

public class ItemBucket extends Item
{
    /** field for checking if the bucket has been filled. */
    private int isFull;

    public ItemBucket(int par1, int par2)
    {
        super(par1);
        this.maxStackSize = 1;
        this.isFull = par2;
        this.setCreativeTab(CreativeTabs.tabMisc);
    }

    /**
     * Called whenever this item is equipped and the right mouse button is pressed. Args: itemStack, world, entityPlayer
     */
    public ItemStack onItemRightClick(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer)
    {
        float f = 1.0F;
        double d0 = par3EntityPlayer.prevPosX + (par3EntityPlayer.posX - par3EntityPlayer.prevPosX) * (double)f;
        double d1 = par3EntityPlayer.prevPosY + (par3EntityPlayer.posY - par3EntityPlayer.prevPosY) * (double)f + 1.62D - (double)par3EntityPlayer.yOffset;
        double d2 = par3EntityPlayer.prevPosZ + (par3EntityPlayer.posZ - par3EntityPlayer.prevPosZ) * (double)f;
        boolean flag = this.isFull == 0;
        MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(par2World, par3EntityPlayer, flag);

        if (movingobjectposition == null)
        {
            return par1ItemStack;
        }
        else
        {
            // Cauldron start - rename to forgeEvent fixing naming clash
            FillBucketEvent forgeEvent = new FillBucketEvent(par3EntityPlayer, par1ItemStack, par2World, movingobjectposition);
            if (MinecraftForge.EVENT_BUS.post(forgeEvent))
            {
                return par1ItemStack;
            }

            if (forgeEvent.getResult() == Event.Result.ALLOW)
            {
                if (par3EntityPlayer.capabilities.isCreativeMode)
                {
                    return par1ItemStack;
                }

                if (--par1ItemStack.stackSize <= 0)
                {
                    return forgeEvent.result;
                }

                if (!par3EntityPlayer.inventory.addItemStackToInventory(forgeEvent.result))
                {
                    par3EntityPlayer.dropPlayerItem(forgeEvent.result);
                }

                return par1ItemStack;
            }
            // Cauldron end

            if (movingobjectposition.typeOfHit == EnumMovingObjectType.TILE)
            {
                int i = movingobjectposition.blockX;
                int j = movingobjectposition.blockY;
                int k = movingobjectposition.blockZ;

                if (!par2World.canMineBlock(par3EntityPlayer, i, j, k))
                {
                    return par1ItemStack;
                }

                if (this.isFull == 0)
                {
                    if (!par3EntityPlayer.canPlayerEdit(i, j, k, movingobjectposition.sideHit, par1ItemStack))
                    {
                        return par1ItemStack;
                    }

                    if (par2World.getBlockMaterial(i, j, k) == Material.water && par2World.getBlockMetadata(i, j, k) == 0)
                    {
                        // CraftBukkit start
                        PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent(par3EntityPlayer, i, j, k, -1, par1ItemStack, Item.bucketWater);

                        if (event.isCancelled())
                        {
                            return par1ItemStack;
                        }

                        // CraftBukkit end
                        par2World.setBlockToAir(i, j, k);

                        if (par3EntityPlayer.capabilities.isCreativeMode)
                        {
                            return par1ItemStack;
                        }

                        ItemStack result = CraftItemStack.asNMSCopy(event.getItemStack()); // CraftBukkit - TODO: Check this stuff later... Not sure how this behavior should work

                        if (--par1ItemStack.stackSize <= 0)
                        {
                            return result; // CraftBukkit
                        }

                        if (!par3EntityPlayer.inventory.addItemStackToInventory(result))   // CraftBukkit
                        {
                            par3EntityPlayer.dropPlayerItem(CraftItemStack.asNMSCopy(event.getItemStack())); // CraftBukkit
                        }

                        return par1ItemStack;
                    }

                    if (par2World.getBlockMaterial(i, j, k) == Material.lava && par2World.getBlockMetadata(i, j, k) == 0)
                    {
                        // CraftBukkit start
                        PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent(par3EntityPlayer, i, j, k, -1, par1ItemStack, Item.bucketLava);

                        if (event.isCancelled())
                        {
                            return par1ItemStack;
                        }

                        // CraftBukkit end
                        par2World.setBlockToAir(i, j, k);

                        if (par3EntityPlayer.capabilities.isCreativeMode)
                        {
                            return par1ItemStack;
                        }

                        ItemStack result = CraftItemStack.asNMSCopy(event.getItemStack()); // CraftBukkit - TODO: Check this stuff later... Not sure how this behavior should work

                        if (--par1ItemStack.stackSize <= 0)
                        {
                            return result; // CraftBukkit
                        }

                        if (!par3EntityPlayer.inventory.addItemStackToInventory(result))   // CraftBukkit
                        {
                            par3EntityPlayer.dropPlayerItem(CraftItemStack.asNMSCopy(event.getItemStack())); // CraftBukkit
                        }

                        return par1ItemStack;
                    }
                }
                else
                {
                    if (this.isFull < 0)
                    {
                        // CraftBukkit start
                        PlayerBucketEmptyEvent event = CraftEventFactory.callPlayerBucketEmptyEvent(par3EntityPlayer, i, j, k, movingobjectposition.sideHit, par1ItemStack);

                        if (event.isCancelled())
                        {
                            return par1ItemStack;
                        }

                        return CraftItemStack.asNMSCopy(event.getItemStack());
                    }

                    int clickedX = i, clickedY = j, clickedZ = k;
                    // CraftBukkit end

                    if (movingobjectposition.sideHit == 0)
                    {
                        --j;
                    }

                    if (movingobjectposition.sideHit == 1)
                    {
                        ++j;
                    }

                    if (movingobjectposition.sideHit == 2)
                    {
                        --k;
                    }

                    if (movingobjectposition.sideHit == 3)
                    {
                        ++k;
                    }

                    if (movingobjectposition.sideHit == 4)
                    {
                        --i;
                    }

                    if (movingobjectposition.sideHit == 5)
                    {
                        ++i;
                    }

                    if (!par3EntityPlayer.canPlayerEdit(i, j, k, movingobjectposition.sideHit, par1ItemStack))
                    {
                        return par1ItemStack;
                    }

                    // CraftBukkit start
                    PlayerBucketEmptyEvent event = CraftEventFactory.callPlayerBucketEmptyEvent(par3EntityPlayer, clickedX, clickedY, clickedZ, movingobjectposition.sideHit, par1ItemStack);

                    if (event.isCancelled())
                    {
                        return par1ItemStack;
                    }

                    // CraftBukkit end

                    if (this.tryPlaceContainedLiquid(par2World, i, j, k) && !par3EntityPlayer.capabilities.isCreativeMode)
                    {
                        return CraftItemStack.asNMSCopy(event.getItemStack()); // CraftBukkit
                    }
                }
            }
            else if (this.isFull == 0 && movingobjectposition.entityHit instanceof EntityCow)
            {
                // CraftBukkit start - This codepath seems to be *NEVER* called
                org.bukkit.Location loc = movingobjectposition.entityHit.getBukkitEntity().getLocation();
                PlayerBucketFillEvent event = CraftEventFactory.callPlayerBucketFillEvent(par3EntityPlayer, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), -1, par1ItemStack, Item.bucketMilk);

                if (event.isCancelled())
                {
                    return par1ItemStack;
                }

                return CraftItemStack.asNMSCopy(event.getItemStack());
                // CraftBukkit end
            }

            return par1ItemStack;
        }
    }

    /**
     * Attempts to place the liquid contained inside the bucket.
     */
    public boolean tryPlaceContainedLiquid(World par1World, int par2, int par3, int par4)
    {
        if (this.isFull <= 0)
        {
            return false;
        }
        else
        {
            Material material = par1World.getBlockMaterial(par2, par3, par4);
            boolean flag = !material.isSolid();

            if (!par1World.isAirBlock(par2, par3, par4) && !flag)
            {
                return false;
            }
            else
            {
                if (par1World.provider.isHellWorld && this.isFull == Block.waterMoving.blockID)
                {
                    par1World.playSoundEffect((double)((float)par2 + 0.5F), (double)((float)par3 + 0.5F), (double)((float)par4 + 0.5F), "random.fizz", 0.5F, 2.6F + (par1World.rand.nextFloat() - par1World.rand.nextFloat()) * 0.8F);

                    for (int l = 0; l < 8; ++l)
                    {
                        par1World.spawnParticle("largesmoke", (double)par2 + Math.random(), (double)par3 + Math.random(), (double)par4 + Math.random(), 0.0D, 0.0D, 0.0D);
                    }
                }
                else
                {
                    if (!par1World.isRemote && flag && !material.isLiquid())
                    {
                        par1World.destroyBlock(par2, par3, par4, true);
                    }

                    par1World.setBlock(par2, par3, par4, this.isFull, 0, 3);
                }

                return true;
            }
        }
    }
}