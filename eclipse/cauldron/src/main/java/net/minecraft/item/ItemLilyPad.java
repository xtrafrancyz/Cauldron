package net.minecraft.item;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class ItemLilyPad extends ItemColored
{
    private static final String __OBFID = "CL_00000074";

    public ItemLilyPad(Block p_i45357_1_)
    {
        super(p_i45357_1_, false);
    }

    public ItemStack onItemRightClick(ItemStack p_77659_1_, World p_77659_2_, EntityPlayer p_77659_3_)
    {
        MovingObjectPosition movingobjectposition = this.getMovingObjectPositionFromPlayer(p_77659_2_, p_77659_3_, true);

        if (movingobjectposition == null)
        {
            return p_77659_1_;
        }
        else
        {
            if (movingobjectposition.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK)
            {
                int i = movingobjectposition.blockX;
                int j = movingobjectposition.blockY;
                int k = movingobjectposition.blockZ;

                if (!p_77659_2_.canMineBlock(p_77659_3_, i, j, k))
                {
                    return p_77659_1_;
                }

                if (!p_77659_3_.canPlayerEdit(i, j, k, movingobjectposition.sideHit, p_77659_1_))
                {
                    return p_77659_1_;
                }

                if (p_77659_2_.getBlock(i, j, k).getMaterial() == Material.water && p_77659_2_.getBlockMetadata(i, j, k) == 0 && p_77659_2_.isAirBlock(i, j + 1, k))
                {
                    // Cauldron start - special case for handling block placement with water lilies
                    org.bukkit.block.BlockState blockstate = org.bukkit.craftbukkit.block.CraftBlockState.getBlockState(p_77659_2_, i, j + 1, k);
                    p_77659_2_.setBlock(i, j + 1, k, Blocks.waterlily);
                    org.bukkit.event.block.BlockPlaceEvent placeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callBlockPlaceEvent(p_77659_2_, p_77659_3_, blockstate, i, j, k);
                    if (placeEvent != null && (placeEvent.isCancelled() || !placeEvent.canBuild())) {
                        blockstate.update(true, false);
                        return p_77659_1_;
                    }
                    // Cauldron end

                    if (!p_77659_3_.capabilities.isCreativeMode)
                    {
                        --p_77659_1_.stackSize;
                    }
                }
            }

            return p_77659_1_;
        }
    }

    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        cpw.mods.fml.relauncher.FMLRelaunchLog.info("onItemUse par1ItemStack = " + par1ItemStack);
        return super.onItemUse(par1ItemStack, par2EntityPlayer, par3World, par4, par5, par6, par7, par8, par9, par10);
    }

    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack p_82790_1_, int p_82790_2_)
    {
        return Blocks.waterlily.getRenderColor(p_82790_1_.getItemDamage());
    }
}