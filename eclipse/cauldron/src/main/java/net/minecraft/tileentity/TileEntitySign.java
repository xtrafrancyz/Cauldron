package net.minecraft.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S33PacketUpdateSign;

public class TileEntitySign extends TileEntity
{
    public String[] signText = new String[] {"", "", "", ""};
    public int lineBeingEdited = -1;
    public boolean field_145916_j = true; // CraftBukkit - private -> public
    private EntityPlayer field_145917_k;
    private static final String __OBFID = "CL_00000363";

    public void writeToNBT(NBTTagCompound p_145841_1_)
    {
        super.writeToNBT(p_145841_1_);
        p_145841_1_.setString("Text1", this.signText[0]);
        p_145841_1_.setString("Text2", this.signText[1]);
        p_145841_1_.setString("Text3", this.signText[2]);
        p_145841_1_.setString("Text4", this.signText[3]);
    }

    public void readFromNBT(NBTTagCompound p_145839_1_)
    {
        this.field_145916_j = false;
        super.readFromNBT(p_145839_1_);

        for (int i = 0; i < 4; ++i)
        {
            this.signText[i] = p_145839_1_.getString("Text" + (i + 1));

            if (this.signText[i].length() > 15)
            {
                this.signText[i] = this.signText[i].substring(0, 15);
            }
        }
    }

    public Packet getDescriptionPacket()
    {
        String[] astring = new String[4];

        // CraftBukkit start - Limit sign text to 15 chars per line
        for (int i = 0; i < 4; ++i)
        {
            astring[i] = this.signText[i];

            if (this.signText[i].length() > 15)
            {
                astring[i] = this.signText[i].substring(0, 15);
            }
        }

        // CraftBukkit end
        return new S33PacketUpdateSign(this.xCoord, this.yCoord, this.zCoord, astring);
    }

    public boolean func_145914_a()
    {
        return this.field_145916_j;
    }

    @SideOnly(Side.CLIENT)
    public void setEditable(boolean p_145913_1_)
    {
        this.field_145916_j = p_145913_1_;

        if (!p_145913_1_)
        {
            this.field_145917_k = null;
        }
    }

    public void func_145912_a(EntityPlayer p_145912_1_)
    {
        this.field_145917_k = p_145912_1_;
    }

    public EntityPlayer func_145911_b()
    {
        return this.field_145917_k;
    }

    // CraftBukkit start - central method to limit sign text to 15 chars per line
    public static String[] sanitizeLines(String[] lines) {
        String[] astring = new String[4];
        for (int i = 0; i < 4; ++i) {
            astring[i] = lines[i];

            if (lines[i].length() > 15) {
                astring[i] = lines[i].substring(0, 15);
            }
        }
        return astring;
    }
    // CraftBukkit end

    // Cauldron start
    @Override
    public boolean canUpdate()
    {
        return false;
    }
    // Cauldron end
}