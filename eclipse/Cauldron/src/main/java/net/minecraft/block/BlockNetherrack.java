package net.minecraft.block;

import org.bukkit.event.block.BlockRedstoneEvent; // CraftBukkit
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.world.World;

public class BlockNetherrack extends Block
{
    public BlockNetherrack(int par1)
    {
        super(par1, Material.rock);
        this.setCreativeTab(CreativeTabs.tabBlock);
    }

    // CraftBukkit start

    /**
     * Lets the block know when one of its neighbor changes. Doesn't know which neighbor changed (coordinates passed are
     * their own) Args: x, y, z, neighbor blockID
     */
    public void onNeighborBlockChange(World world, int i, int j, int k, int l)
    {
        if (Block.blocksList[l] != null && Block.blocksList[l].canProvidePower())
        {
            org.bukkit.block.Block block = world.getWorld().getBlockAt(i, j, k);
            int power = block.getBlockPower();
            BlockRedstoneEvent event = new BlockRedstoneEvent(block, power, power);
            world.getServer().getPluginManager().callEvent(event);
        }
    }
    // CraftBukkit end
}