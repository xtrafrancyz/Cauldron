package net.minecraft.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

public class TileEntityCommandBlock extends TileEntity
{
    private final TileEntityCommandBlockListener field_145994_a_CB = new TileEntityCommandBlockListener(this); // CraftBukkit
    private final CommandBlockLogic field_145994_a = field_145994_a_CB; // Cauldron

    private static final String __OBFID = "CL_00000347";

    public void writeToNBT(NBTTagCompound p_145841_1_)
    {
        super.writeToNBT(p_145841_1_);
        this.field_145994_a.func_145758_a(p_145841_1_);
    }

    public void readFromNBT(NBTTagCompound p_145839_1_)
    {
        super.readFromNBT(p_145839_1_);
        this.field_145994_a.func_145759_b(p_145839_1_);
    }

    public Packet getDescriptionPacket()
    {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        this.writeToNBT(nbttagcompound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 2, nbttagcompound);
    }

    public CommandBlockLogic func_145993_a()
    {
        return this.field_145994_a;
    }

    // Cauldron start
    @Override
    public boolean canUpdate()
    {
        return false;
    }
    // Cauldron end
}