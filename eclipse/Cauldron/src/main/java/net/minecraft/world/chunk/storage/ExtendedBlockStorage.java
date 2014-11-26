package net.minecraft.world.chunk.storage;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.world.chunk.NibbleArray;

public class ExtendedBlockStorage
{
    /**
     * Contains the bottom-most Y block represented by this ExtendedBlockStorage. Typically a multiple of 16.
     */
    private int yBase;

    /**
     * A total count of the number of non-air blocks in this block storage's Chunk.
     */
    private int blockRefCount;

    /**
     * Contains the number of blocks in this block storage's parent chunk that require random ticking. Used to cull the
     * Chunk from random tick updates for performance reasons.
     */
    private int tickRefCount;

    /**
     * Contains the least significant 8 bits of each block ID belonging to this block storage's parent Chunk.
     */
    private byte[] blockLSBArray;

    /**
     * Contains the most significant 4 bits of each block ID belonging to this block storage's parent Chunk.
     */
    private NibbleArray blockMSBArray;

    /**
     * Stores the metadata associated with blocks in this ExtendedBlockStorage.
     */
    private NibbleArray blockMetadataArray;

    /** The NibbleArray containing a block of Block-light data. */
    private NibbleArray blocklightArray;

    /** The NibbleArray containing a block of Sky-light data. */
    private NibbleArray skylightArray;

    public ExtendedBlockStorage(int par1, boolean par2)
    {
        this.yBase = par1;
        this.blockLSBArray = new byte[4096];
        this.blockMetadataArray = new NibbleArray(this.blockLSBArray.length, 4);
        this.blocklightArray = new NibbleArray(this.blockLSBArray.length, 4);

        if (par2)
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

    /**
     * Returns the extended block ID for a location in a chunk, merged from a byte array and a NibbleArray to form a
     * full 12-bit block ID.
     */
    public int getExtBlockID(int par1, int par2, int par3)
    {
        int l = this.blockLSBArray[par2 << 8 | par3 << 4 | par1] & 255;
        return this.blockMSBArray != null ? this.blockMSBArray.get(par1, par2, par3) << 8 | l : l;
    }

    /**
     * Sets the extended block ID for a location in a chunk, splitting bits 11..8 into a NibbleArray and bits 7..0 into
     * a byte array. Also performs reference counting to determine whether or not to broadly cull this Chunk from the
     * random-update tick list.
     */
    public void setExtBlockID(int par1, int par2, int par3, int par4)
    {
        int i1 = this.blockLSBArray[par2 << 8 | par3 << 4 | par1] & 255;

        if (this.blockMSBArray != null)
        {
            i1 |= this.blockMSBArray.get(par1, par2, par3) << 8;
        }

        if (i1 == 0 && par4 != 0)
        {
            ++this.blockRefCount;

            if (Block.blocksList[par4] != null && Block.blocksList[par4].getTickRandomly())
            {
                ++this.tickRefCount;
            }
        }
        else if (i1 != 0 && par4 == 0)
        {
            --this.blockRefCount;

            if (Block.blocksList[i1] != null && Block.blocksList[i1].getTickRandomly())
            {
                --this.tickRefCount;
            }
        }
        else if (Block.blocksList[i1] != null && Block.blocksList[i1].getTickRandomly() && (Block.blocksList[par4] == null || !Block.blocksList[par4].getTickRandomly()))
        {
            --this.tickRefCount;
        }
        else if ((Block.blocksList[i1] == null || !Block.blocksList[i1].getTickRandomly()) && Block.blocksList[par4] != null && Block.blocksList[par4].getTickRandomly())
        {
            ++this.tickRefCount;
        }

        this.blockLSBArray[par2 << 8 | par3 << 4 | par1] = (byte)(par4 & 255);

        if (par4 > 255)
        {
            if (this.blockMSBArray == null)
            {
                this.blockMSBArray = new NibbleArray(this.blockLSBArray.length, 4);
            }

            this.blockMSBArray.set(par1, par2, par3, (par4 & 3840) >> 8);
        }
        else if (this.blockMSBArray != null)
        {
            this.blockMSBArray.set(par1, par2, par3, 0);
        }
    }

    /**
     * Returns the metadata associated with the block at the given coordinates in this ExtendedBlockStorage.
     */
    public int getExtBlockMetadata(int par1, int par2, int par3)
    {
        return this.blockMetadataArray.get(par1, par2, par3);
    }

    /**
     * Sets the metadata of the Block at the given coordinates in this ExtendedBlockStorage to the given metadata.
     */
    public void setExtBlockMetadata(int par1, int par2, int par3, int par4)
    {
        this.blockMetadataArray.set(par1, par2, par3, par4);
    }

    /**
     * Returns whether or not this block storage's Chunk is fully empty, based on its internal reference count.
     */
    public boolean isEmpty()
    {
        return this.blockRefCount == 0;
    }

    /**
     * Returns whether or not this block storage's Chunk will require random ticking, used to avoid looping through
     * random block ticks when there are no blocks that would randomly tick.
     */
    public boolean getNeedsRandomTick()
    {
        return this.tickRefCount > 0;
    }

    /**
     * Returns the Y location of this ExtendedBlockStorage.
     */
    public int getYLocation()
    {
        return this.yBase;
    }

    /**
     * Sets the saved Sky-light value in the extended block storage structure.
     */
    public void setExtSkylightValue(int par1, int par2, int par3, int par4)
    {
        this.skylightArray.set(par1, par2, par3, par4);
    }

    /**
     * Gets the saved Sky-light value in the extended block storage structure.
     */
    public int getExtSkylightValue(int par1, int par2, int par3)
    {
        return this.skylightArray.get(par1, par2, par3);
    }

    /**
     * Sets the saved Block-light value in the extended block storage structure.
     */
    public void setExtBlocklightValue(int par1, int par2, int par3, int par4)
    {
        this.blocklightArray.set(par1, par2, par3, par4);
    }

    /**
     * Gets the saved Block-light value in the extended block storage structure.
     */
    public int getExtBlocklightValue(int par1, int par2, int par3)
    {
        return this.blocklightArray.get(par1, par2, par3);
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
                    if (Block.blocksList[l] == null)
                    {
                        blkIds[off] = 0;
                    }
                    else
                    {
                        ++cntNonEmpty;

                        if (Block.blocksList[l].getTickRandomly())
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
                    if (Block.blocksList[l] == null)
                    {
                        blkIds[off] = 0;
                        ext[off2] &= 0xF0;
                    }
                    else
                    {
                        ++cntNonEmpty;

                        if (Block.blocksList[l].getTickRandomly())
                        {
                            ++cntTicking;
                        }
                    }
                }

                off++;
                l = (blkIds[off] & 0xFF) | ((extid & 0xF0) << 4); // Odd data

                if (l > 0)
                {
                    if (Block.blocksList[l] == null)
                    {
                        blkIds[off] = 0;
                        ext[off2] &= 0x0F;
                    }
                    else
                    {
                        ++cntNonEmpty;

                        if (Block.blocksList[l].getTickRandomly())
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
                    int l = this.getExtBlockID(i, j, k);

                    if (l > 0)
                    {
                        if (Block.blocksList[l] == null)
                        {
                            this.blockLSBArray[j << 8 | k << 4 | i] = 0;

                            if (this.blockMSBArray != null)
                            {
                                this.blockMSBArray.set(i, j, k, 0);
                            }
                        }
                        else
                        {
                            ++this.blockRefCount;

                            if (Block.blocksList[l].getTickRandomly())
                            {
                                ++this.tickRefCount;
                            }
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

    /**
     * Returns the block ID MSB (bits 11..8) array for this storage array's Chunk.
     */
    public NibbleArray getBlockMSBArray()
    {
        return this.blockMSBArray;
    }

    public NibbleArray getMetadataArray()
    {
        return this.blockMetadataArray;
    }

    /**
     * Returns the NibbleArray instance containing Block-light data.
     */
    public NibbleArray getBlocklightArray()
    {
        return this.blocklightArray;
    }

    /**
     * Returns the NibbleArray instance containing Sky-light data.
     */
    public NibbleArray getSkylightArray()
    {
        return this.skylightArray;
    }

    /**
     * Sets the array of block ID least significant bits for this ExtendedBlockStorage.
     */
    public void setBlockLSBArray(byte[] par1ArrayOfByte)
    {
        this.blockLSBArray = this.validateByteArray(par1ArrayOfByte); // CraftBukkit - Validate data
    }

    /**
     * Sets the array of blockID most significant bits (blockMSBArray) for this ExtendedBlockStorage.
     */
    public void setBlockMSBArray(NibbleArray par1NibbleArray)
    {
        // CraftBukkit start - Don't hang on to an empty nibble array
        boolean empty = true;

        // Spigot start
        if ((!par1NibbleArray.isTrivialArray()) || (par1NibbleArray.getTrivialArrayValue() != 0))
            {
                empty = false;
        }

        // Spigot end

        if (empty)
        {
            return;
        }

        // CraftBukkit end
        this.blockMSBArray = this.validateNibbleArray(par1NibbleArray); // CraftBukkit - Validate data
    }

    /**
     * Sets the NibbleArray of block metadata (blockMetadataArray) for this ExtendedBlockStorage.
     */
    public void setBlockMetadataArray(NibbleArray par1NibbleArray)
    {
        this.blockMetadataArray = this.validateNibbleArray(par1NibbleArray); // CraftBukkit - Validate data
    }

    /**
     * Sets the NibbleArray instance used for Block-light values in this particular storage block.
     */
    public void setBlocklightArray(NibbleArray par1NibbleArray)
    {
        this.blocklightArray = this.validateNibbleArray(par1NibbleArray); // CraftBukkit - Validate data
    }

    /**
     * Sets the NibbleArray instance used for Sky-light values in this particular storage block.
     */
    public void setSkylightArray(NibbleArray par1NibbleArray)
    {
        this.skylightArray = this.validateNibbleArray(par1NibbleArray); // CraftBukkit - Validate data
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

    /**
     * Called by a Chunk to initialize the MSB array if getBlockMSBArray returns null. Returns the newly-created
     * NibbleArray instance.
     */
    public NibbleArray createBlockMSBArray()
    {
        this.blockMSBArray = new NibbleArray(this.blockLSBArray.length, 4);
        return this.blockMSBArray;
    }
}