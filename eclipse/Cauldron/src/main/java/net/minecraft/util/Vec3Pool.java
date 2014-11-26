package net.minecraft.util;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;

public class Vec3Pool
{
    private final int truncateArrayResetThreshold;
    private final int minimumSize;
    // CraftBukkit start
    // private final List pool = new ArrayList();
    private Vec3 freelist = null;
    private Vec3 alloclist = null;
    private Vec3 freelisthead = null;
    private Vec3 alloclisthead = null;
    private int total_size = 0;
    // CraftBukkit end
    private int nextFreeSpace;
    private int maximumSizeSinceLastTruncation;
    private int resetCount;

    public Vec3Pool(int par1, int par2)
    {
        this.truncateArrayResetThreshold = par1;
        this.minimumSize = par2;
    }

    /**
     * extends the pool if all vecs are currently "out"
     */
    public synchronized final Vec3 getVecFromPool(double par1, double par3, double par5)   // CraftBukkit - add final // Cauldron - synchronize
    {
        if (this.resetCount == 0)
        {
            return Vec3.createVectorHelper(par1, par3, par5);    // CraftBukkit - Don't pool objects indefinitely if thread doesn't adhere to contract
        }

        Vec3 vec3;

        if (this.freelist == null)   // CraftBukkit
        {
            vec3 = new Vec3(this, par1, par3, par5);
            this.total_size++; // CraftBukkit
        }
        else
        {
            // CraftBukkit start
            vec3 = this.freelist;
            this.freelist = vec3.next;
            // CraftBukkit end
            vec3.setComponents(par1, par3, par5);
        }

        // CraftBukkit start
        if (this.alloclist == null)
        {
            this.alloclisthead = vec3;
        }

        vec3.next = this.alloclist; // Add to allocated list
        this.alloclist = vec3;
        // CraftBukkit end
        ++this.nextFreeSpace;
        return vec3;
    }

    // CraftBukkit start - Offer back vector (can save LOTS of unneeded bloat) - works about 90% of the time
    public synchronized void release(Vec3 v) // Cauldron - synchronize
    {
        if (this.alloclist == v)
        {
            this.alloclist = v.next; // Pop off alloc list

            // Push on to free list
            if (this.freelist == null)
            {
                this.freelisthead = v;
            }

            v.next = this.freelist;
            this.freelist = v;
            this.nextFreeSpace--;
        }
    }
    // CraftBukkit end

    /**
     * Will truncate the array everyN clears to the maximum size observed since the last truncation.
     */
    public synchronized void clear() // Cauldron - synchronize
    {
        if (this.nextFreeSpace > this.maximumSizeSinceLastTruncation)
        {
            this.maximumSizeSinceLastTruncation = this.nextFreeSpace;
        }

        // CraftBukkit start - Intelligent cache
        // Take any allocated blocks and put them on free list
        if (this.alloclist != null)
        {
            if (this.freelist == null)
            {
                this.freelist = this.alloclist;
                this.freelisthead = this.alloclisthead;
            }
            else
            {
                this.alloclisthead.next = this.freelist;
                this.freelist = this.alloclist;
                this.freelisthead = this.alloclisthead;
            }

            this.alloclist = null;
        }

        if ((this.resetCount++ & 0xff) == 0)
        {
            int newSize = total_size - (total_size >> 3);

            if (newSize > this.maximumSizeSinceLastTruncation)   // newSize will be 87.5%, but if we were not in that range, we clear some of the cache
            {
                for (int i = total_size; i > newSize; i--)
                {
                    freelist = freelist.next;
                }

                total_size = newSize;
            }

            this.maximumSizeSinceLastTruncation = 0;
            // this.f = 0; // We do not reset to zero; it doubles for a flag
        }

        this.nextFreeSpace = 0;
        // CraftBukkit end
    }

    @SideOnly(Side.CLIENT)
    public void clearAndFreeCache()
    {
        if (!this.func_82589_e())
        {
            this.nextFreeSpace = 0;
            // Cauldron start
            freelist = null;
            alloclist = null;
            freelisthead = null;
            alloclisthead = null;
            total_size = 0;
            // Cauldron end
        }
    }

    public int getPoolSize()
    {
        return this.total_size; // CraftBukkit
    }

    public int func_82590_d()
    {
        return this.nextFreeSpace;
    }

    private boolean func_82589_e()
    {
        return this.minimumSize < 0 || this.truncateArrayResetThreshold < 0;
    }
}