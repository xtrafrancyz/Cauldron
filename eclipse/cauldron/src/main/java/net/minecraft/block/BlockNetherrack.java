package net.minecraft.block;

import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

// CraftBukkit start
import org.bukkit.event.block.BlockRedstoneEvent;
import net.minecraft.world.World;
// CraftBukkit end

public class BlockNetherrack extends Block
{
    private static final String __OBFID = "CL_00000275";

    public BlockNetherrack()
    {
        super(Material.rock);
        this.setCreativeTab(CreativeTabs.tabBlock);
    }

    public MapColor getMapColor(int p_149728_1_)
    {
        return MapColor.netherrackColor;
    }

    // CraftBukkit start
    public void doPhysics(World world, int i, int j, int k, int l)
    {
        if (Block.getBlockById(l) != null && Block.getBlockById(l).canProvidePower())
        {
            org.bukkit.block.Block block = world.getWorld().getBlockAt(i, j, k);
            int power = block.getBlockPower();
            BlockRedstoneEvent event = new BlockRedstoneEvent(block, power, power);
            world.getServer().getPluginManager().callEvent(event);
        }
    }
    // CraftBukkit end
}