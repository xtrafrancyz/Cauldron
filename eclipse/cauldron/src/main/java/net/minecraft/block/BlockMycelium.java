package net.minecraft.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

// CraftBukkit start
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
// CraftBukkit end

public class BlockMycelium extends Block
{
    @SideOnly(Side.CLIENT)
    private IIcon field_150200_a;
    @SideOnly(Side.CLIENT)
    private IIcon field_150199_b;
    private static final String __OBFID = "CL_00000273";

    protected BlockMycelium()
    {
        super(Material.grass);
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.tabBlock);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int p_149691_1_, int p_149691_2_)
    {
        return p_149691_1_ == 1 ? this.field_150200_a : (p_149691_1_ == 0 ? Blocks.dirt.getBlockTextureFromSide(p_149691_1_) : this.blockIcon);
    }

    public void updateTick(World p_149674_1_, int p_149674_2_, int p_149674_3_, int p_149674_4_, Random p_149674_5_)
    {
        if (!p_149674_1_.isRemote)
        {
            if (p_149674_1_.getBlockLightValue(p_149674_2_, p_149674_3_ + 1, p_149674_4_) < 4 && p_149674_1_.getBlockLightOpacity(p_149674_2_, p_149674_3_ + 1, p_149674_4_) > 2)
            {
                // CraftBukkit start
                org.bukkit.World bworld = p_149674_1_.getWorld();
                BlockState blockState = bworld.getBlockAt(p_149674_2_, p_149674_3_, p_149674_4_).getState();
                blockState.setTypeId(Block.getIdFromBlock(Blocks.dirt));
                BlockFadeEvent event = new BlockFadeEvent(blockState.getBlock(), blockState);
                p_149674_1_.getServer().getPluginManager().callEvent(event);

                if (!event.isCancelled())
                {
                    blockState.update(true);
                }

                // CraftBukkit end
            }
            else if (p_149674_1_.getBlockLightValue(p_149674_2_, p_149674_3_ + 1, p_149674_4_) >= 9)
            {
                int numGrowth = Math.min(4, Math.max(20, (int)(4 * 100F / p_149674_1_.growthOdds)));  // Spigot

                for (int l = 0; l < numGrowth; ++l)   // Spigot
                {
                    int i1 = p_149674_2_ + p_149674_5_.nextInt(3) - 1;
                    int j1 = p_149674_3_ + p_149674_5_.nextInt(5) - 3;
                    int k1 = p_149674_4_ + p_149674_5_.nextInt(3) - 1;
                    Block block = p_149674_1_.getBlock(i1, j1 + 1, k1);

                    if (p_149674_1_.getBlock(i1, j1, k1) == Blocks.dirt && p_149674_1_.getBlockMetadata(i1, j1, k1) == 0 && p_149674_1_.getBlockLightValue(i1, j1 + 1, k1) >= 4 && p_149674_1_.getBlockLightOpacity(i1, j1 + 1, k1) <= 2)
                    {
                        // CraftBukkit start
                        org.bukkit.World bworld = p_149674_1_.getWorld();
                        BlockState blockState = bworld.getBlockAt(i1, j1, k1).getState();
                        blockState.setTypeId(Block.getIdFromBlock(this));
                        BlockSpreadEvent event = new BlockSpreadEvent(blockState.getBlock(), bworld.getBlockAt(p_149674_2_, p_149674_3_, p_149674_4_), blockState);
                        p_149674_1_.getServer().getPluginManager().callEvent(event);

                        if (!event.isCancelled())
                        {
                            blockState.update(true);
                        }

                        // CraftBukkit end
                    }
                }
            }
        }
    }

    public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_)
    {
        return Blocks.dirt.getItemDropped(0, p_149650_2_, p_149650_3_);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess p_149673_1_, int p_149673_2_, int p_149673_3_, int p_149673_4_, int p_149673_5_)
    {
        if (p_149673_5_ == 1)
        {
            return this.field_150200_a;
        }
        else if (p_149673_5_ == 0)
        {
            return Blocks.dirt.getBlockTextureFromSide(p_149673_5_);
        }
        else
        {
            Material material = p_149673_1_.getBlock(p_149673_2_, p_149673_3_ + 1, p_149673_4_).getMaterial();
            return material != Material.snow && material != Material.craftedSnow ? this.blockIcon : this.field_150199_b;
        }
    }

    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister p_149651_1_)
    {
        this.blockIcon = p_149651_1_.registerIcon(this.getTextureName() + "_side");
        this.field_150200_a = p_149651_1_.registerIcon(this.getTextureName() + "_top");
        this.field_150199_b = p_149651_1_.registerIcon("grass_side_snowed");
    }

    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World p_149734_1_, int p_149734_2_, int p_149734_3_, int p_149734_4_, Random p_149734_5_)
    {
        super.randomDisplayTick(p_149734_1_, p_149734_2_, p_149734_3_, p_149734_4_, p_149734_5_);

        if (p_149734_5_.nextInt(10) == 0)
        {
            p_149734_1_.spawnParticle("townaura", (double)((float)p_149734_2_ + p_149734_5_.nextFloat()), (double)((float)p_149734_3_ + 1.1F), (double)((float)p_149734_4_ + p_149734_5_.nextFloat()), 0.0D, 0.0D, 0.0D);
        }
    }
}