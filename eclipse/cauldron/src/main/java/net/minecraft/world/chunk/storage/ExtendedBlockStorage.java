package net.minecraft.world.chunk.storage;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.NibbleArray;

public class ExtendedBlockStorage
{
    private int yBase;
    private int blockRefCount;
    private int tickRefCount;
    private byte[] blockLSBArray;
    private NibbleArray blockMSBArray;
    private NibbleArray blockMetadataArray;
    private NibbleArray blocklightArray;
    private NibbleArray skylightArray;
    private static final String __OBFID = "CL_00000375";

    public ExtendedBlockStorage(int p_i1997_1_, boolean p_i1997_2_)
    {
        this.yBase = p_i1997_1_;
        this.blockLSBArray = new byte[4096];
        this.blockMetadataArray = new NibbleArray(this.blockLSBArray.length, 4);
        this.blocklightArray = new NibbleArray(this.blockLSBArray.length, 4);

        if (p_i1997_2_)
        {
            this.skylightArray = new NibbleArray(this.blockLSBArray.length, 4);
        }
    }

    // CraftBukkit start
    public ExtendedBlockStorage(int y, boolean flag, byte[] blkIds, byte[] extBlkIds)
    {
        this.yBase = y;
        this.blockLSBArray = blkIds;

        if (extBlkIds != null)
        {
            this.blockMSBArray = new NibbleArray(extBlkIds, 4);
        }

        this.blockMetadataArray = new NibbleArray(this.blockLSBArray.length, 4);
        this.blocklightArray = new NibbleArray(this.blockLSBArray.length, 4);

        if (flag)
        {
            this.skylightArray = new NibbleArray(this.blockLSBArray.length, 4);
        }

        this.removeInvalidBlocks();
    }
    // CraftBukkit end

    public Block getBlockByExtId(int p_150819_1_, int p_150819_2_, int p_150819_3_)
    {
        int l = this.blockLSBArray[p_150819_2_ << 8 | p_150819_3_ << 4 | p_150819_1_] & 255;

        if (this.blockMSBArray != null)
        {
            l |= this.blockMSBArray.get(p_150819_1_, p_150819_2_, p_150819_3_) << 8;
        }

        return Block.getBlockById(l);
    }

    public void func_150818_a(int p_150818_1_, int p_150818_2_, int p_150818_3_, Block p_150818_4_)
    {
        int l = this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] & 255;

        if (this.blockMSBArray != null)
        {
            l |= this.blockMSBArray.get(p_150818_1_, p_150818_2_, p_150818_3_) << 8;
        }

        Block block1 = Block.getBlockById(l);

        if (block1 != Blocks.air)
        {
            --this.blockRefCount;

            if (block1.getTickRandomly())
            {
                --this.tickRefCount;
            }
        }

        if (p_150818_4_ != Blocks.air)
        {
            ++this.blockRefCount;

            if (p_150818_4_.getTickRandomly())
            {
                ++this.tickRefCount;
            }
        }

        int i1 = Block.getIdFromBlock(p_150818_4_);
        this.blockLSBArray[p_150818_2_ << 8 | p_150818_3_ << 4 | p_150818_1_] = (byte)(i1 & 255);

        if (i1 > 255)
        {
            if (this.blockMSBArray == null)
            {
                this.blockMSBArray = new NibbleArray(this.blockLSBArray.length, 4);
            }

            this.blockMSBArray.set(p_150818_1_, p_150818_2_, p_150818_3_, (i1 & 3840) >> 8);
        }
        else if (this.blockMSBArray != null)
        {
            this.blockMSBArray.set(p_150818_1_, p_150818_2_, p_150818_3_, 0);
        }
    }

    public int getExtBlockMetadata(int p_76665_1_, int p_76665_2_, int p_76665_3_)
    {
        return this.blockMetadataArray.get(p_76665_1_, p_76665_2_, p_76665_3_);
    }

    public void setExtBlockMetadata(int p_76654_1_, int p_76654_2_, int p_76654_3_, int p_76654_4_)
    {
        this.blockMetadataArray.set(p_76654_1_, p_76654_2_, p_76654_3_, p_76654_4_);
    }

    public boolean isEmpty()
    {
        return this.blockRefCount == 0;
    }

    public boolean getNeedsRandomTick()
    {
        return this.tickRefCount > 0;
    }

    public int getYLocation()
    {
        return this.yBase;
    }

    public void setExtSkylightValue(int p_76657_1_, int p_76657_2_, int p_76657_3_, int p_76657_4_)
    {
        this.skylightArray.set(p_76657_1_, p_76657_2_, p_76657_3_, p_76657_4_);
    }

    public int getExtSkylightValue(int p_76670_1_, int p_76670_2_, int p_76670_3_)
    {
        return this.skylightArray.get(p_76670_1_, p_76670_2_, p_76670_3_);
    }

    public void setExtBlocklightValue(int p_76677_1_, int p_76677_2_, int p_76677_3_, int p_76677_4_)
    {
        this.blocklightArray.set(p_76677_1_, p_76677_2_, p_76677_3_, p_76677_4_);
    }

    public int getExtBlocklightValue(int p_76674_1_, int p_76674_2_, int p_76674_3_)
    {
        return this.blocklightArray.get(p_76674_1_, p_76674_2_, p_76674_3_);
    }

    public void removeInvalidBlocks()
    {
        // CraftBukkit start - Optimize for speed
        byte[] blkIds = this.blockLSBArray;
        int cntNonEmpty = 0;
        int cntTicking = 0;

        if (this.blockMSBArray == null)   // No extended block IDs?  Don't waste time messing with them
        {
            for (int off = 0; off < blkIds.length; off++)
            {
                int l = blkIds[off] & 0xFF;

                if (l > 0)
                {
                    if (Block.getBlockById(l) == null)
                    {
                        blkIds[off] = 0;
                    }
                    else
                    {
                        ++cntNonEmpty;

                        if (Block.getBlockById(l).getTickRandomly())
                        {
                            ++cntTicking;
                        }
                    }
                }
            }
        }
        else
        {
            this.blockMSBArray.forceToNonTrivialArray(); // Spigot
            byte[] ext = this.blockMSBArray.getValueArray();

            for (int off = 0, off2 = 0; off < blkIds.length;)
            {
                byte extid = ext[off2];
                int l = (blkIds[off] & 0xFF) | ((extid & 0xF) << 8); // Even data

                if (l > 0)
                {
                    if (Block.getBlockById(l) == null)
                    {
                        blkIds[off] = 0;
                        ext[off2] &= 0xF0;
                    }
                    else
                    {
                        ++cntNonEmpty;

                        if (Block.getBlockById(l).getTickRandomly())
                        {
                            ++cntTicking;
                        }
                    }
                }

                off++;
                l = (blkIds[off] & 0xFF) | ((extid & 0xF0) << 4); // Odd data

                if (l > 0)
                {
                    if (Block.getBlockById(l) == null)
                    {
                        blkIds[off] = 0;
                        ext[off2] &= 0x0F;
                    }
                    else
                    {
                        ++cntNonEmpty;

                        if (Block.getBlockById(l).getTickRandomly())
                        {
                            ++cntTicking;
                        }
                    }
                }

                off++;
                off2++;
            }

            // Spigot start
            this.blockMSBArray.detectAndProcessTrivialArray();

            if (this.blockMSBArray.isTrivialArray() && (this.blockMSBArray.getTrivialArrayValue() == 0))
            {
                this.blockMSBArray = null;
            }

            // Spigot end
        }

        this.blockRefCount = cntNonEmpty;
        this.tickRefCount = cntTicking;
    }

    public void old_recalcBlockCounts()
    {
        // CraftBukkit end
        this.blockRefCount = 0;
        this.tickRefCount = 0;

        for (int i = 0; i < 16; ++i)
        {
            for (int j = 0; j < 16; ++j)
            {
                for (int k = 0; k < 16; ++k)
                {
                    Block block = this.getBlockByExtId(i, j, k);

                    if (block != Blocks.air)
                    {
                        ++this.blockRefCount;

                        if (block.getTickRandomly())
                        {
                            ++this.tickRefCount;
                        }
                    }
                }
            }
        }
    }

    public byte[] getBlockLSBArray()
    {
        return this.blockLSBArray;
    }

    @SideOnly(Side.CLIENT)
    public void clearMSBArray()
    {
        this.blockMSBArray = null;
    }

    public NibbleArray getBlockMSBArray()
    {
        return this.blockMSBArray;
    }

    public NibbleArray getMetadataArray()
    {
        return this.blockMetadataArray;
    }

    public NibbleArray getBlocklightArray()
    {
        return this.blocklightArray;
    }

    public NibbleArray getSkylightArray()
    {
        return this.skylightArray;
    }

    public void setBlockLSBArray(byte[] p_76664_1_)
    {
        this.blockLSBArray = this.validateByteArray(p_76664_1_); // CraftBukkit - Validate data
    }

    public void setBlockMSBArray(NibbleArray p_76673_1_)
    {
        // CraftBukkit start - Don't hang on to an empty nibble array
        boolean empty = true;

        // Spigot start
        if ((!p_76673_1_.isTrivialArray()) || (p_76673_1_.getTrivialArrayValue() != 0))
        {
            empty = false;
        }

        // Spigot end

        if (empty)
        {
            return;
        }

        // CraftBukkit end
        this.blockMSBArray = this.validateNibbleArray(p_76673_1_); // CraftBukkit - Validate data
    }

    public void setBlockMetadataArray(NibbleArray p_76668_1_)
    {
        this.blockMetadataArray = this.validateNibbleArray(p_76668_1_); // CraftBukkit - Validate data
    }

    public void setBlocklightArray(NibbleArray p_76659_1_)
    {
        this.blocklightArray = this.validateNibbleArray(p_76659_1_); // CraftBukkit - Validate data
    }

    public void setSkylightArray(NibbleArray p_76666_1_)
    {
        this.skylightArray = this.validateNibbleArray(p_76666_1_); // CraftBukkit - Validate data
    }

    // CraftBukkit start - Validate array lengths
    private NibbleArray validateNibbleArray(NibbleArray nibbleArray)
    {
        // Spigot start - fix for more awesome nibble arrays
        if (nibbleArray != null && nibbleArray.getByteLength() < 2048)
        {
            nibbleArray.resizeArray(2048);
        }

        // Spigot end
        return nibbleArray;
    }

    private byte[] validateByteArray(byte[] byteArray)
    {
        if (byteArray != null && byteArray.length < 4096)
        {
            byte[] newArray = new byte[4096];
            System.arraycopy(byteArray, 0, newArray, 0, byteArray.length);
            byteArray = newArray;
        }

        return byteArray;
    }
    // CraftBukkit end

    @SideOnly(Side.CLIENT)
    public NibbleArray createBlockMSBArray()
    {
        this.blockMSBArray = new NibbleArray(this.blockLSBArray.length, 4);
        return this.blockMSBArray;
    }
}