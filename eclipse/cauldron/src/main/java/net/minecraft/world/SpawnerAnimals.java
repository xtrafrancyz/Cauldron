package net.minecraft.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import cpw.mods.fml.common.eventhandler.Event.Result;
import net.minecraftforge.event.ForgeEventFactory;

// CraftBukkit start
import org.bukkit.craftbukkit.util.LongHash;
import org.bukkit.craftbukkit.util.LongObjectHashMap;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
// CraftBukkit end

public final class SpawnerAnimals
{
    private LongObjectHashMap<Boolean> eligibleChunksForSpawning = new LongObjectHashMap<Boolean>(); // CraftBukkit - HashMap -> LongObjectHashMap
    private static final String __OBFID = "CL_00000152";

    protected static ChunkPosition func_151350_a(World p_151350_0_, int p_151350_1_, int p_151350_2_)
    {
        Chunk chunk = p_151350_0_.getChunkFromChunkCoords(p_151350_1_, p_151350_2_);
        int k = p_151350_1_ * 16 + p_151350_0_.rand.nextInt(16);
        int l = p_151350_2_ * 16 + p_151350_0_.rand.nextInt(16);
        int i1 = p_151350_0_.rand.nextInt(chunk == null ? p_151350_0_.getActualHeight() : chunk.getTopFilledSegment() + 16 - 1);
        return new ChunkPosition(k, i1, l);
    }

    public int findChunksForSpawning(WorldServer p_77192_1_, boolean p_77192_2_, boolean p_77192_3_, boolean p_77192_4_)
    {
        if (!p_77192_2_ && !p_77192_3_)
        {
            return 0;
        }
        else
        {
            this.eligibleChunksForSpawning.clear();
            int i;
            int k;

            for (i = 0; i < p_77192_1_.playerEntities.size(); ++i)
            {
                EntityPlayer entityplayer = (EntityPlayer)p_77192_1_.playerEntities.get(i);
                int j = MathHelper.floor_double(entityplayer.posX / 16.0D);
                k = MathHelper.floor_double(entityplayer.posZ / 16.0D);
                byte b0 = 8;
                // Spigot Start
                b0 = p_77192_1_.getSpigotConfig().mobSpawnRange; // Cauldron
                b0 = (b0 > p_77192_1_.getSpigotConfig().viewDistance) ? (byte) p_77192_1_.spigotConfig.viewDistance : b0; // Cauldron
                b0 = (b0 > 8) ? 8 : b0;
                // Spigot End

                for (int l = -b0; l <= b0; ++l)
                {
                    for (int i1 = -b0; i1 <= b0; ++i1)
                    {
                        boolean flag3 = l == -b0 || l == b0 || i1 == -b0 || i1 == b0;
                        // CraftBukkit start
                        long chunkCoords = LongHash.toLong(l + j, i1 + k);

                        if (!flag3)
                        {
                            this.eligibleChunksForSpawning.put(chunkCoords, false);
                        }
                        else if (!this.eligibleChunksForSpawning.containsKey(chunkCoords))
                        {
                            this.eligibleChunksForSpawning.put(chunkCoords, true);
                        }

                        // CraftBukkit end
                    }
                }
            }

            i = 0;
            ChunkCoordinates chunkcoordinates = p_77192_1_.getSpawnPoint();
            EnumCreatureType[] aenumcreaturetype = EnumCreatureType.values();
            k = aenumcreaturetype.length;

            for (int k3 = 0; k3 < k; ++k3)
            {
                EnumCreatureType enumcreaturetype = aenumcreaturetype[k3];
                // CraftBukkit start - Use per-world spawn limits
                int limit = enumcreaturetype.getMaxNumberOfCreature();

                switch (enumcreaturetype)
                {
                    case monster:
                        limit = p_77192_1_.getWorld().getMonsterSpawnLimit();
                        break;
                    case creature:
                        limit = p_77192_1_.getWorld().getAnimalSpawnLimit();
                        break;
                    case waterCreature:
                        limit = p_77192_1_.getWorld().getWaterAnimalSpawnLimit();
                        break;
                    case ambient:
                        limit = p_77192_1_.getWorld().getAmbientSpawnLimit();
                        break;
                }

                if (limit == 0)
                {
                    continue;
                }

                // CraftBukkit end

                if ((!enumcreaturetype.getPeacefulCreature() || p_77192_3_) && (enumcreaturetype.getPeacefulCreature() || p_77192_2_) && (!enumcreaturetype.getAnimal() || p_77192_4_) && p_77192_1_.countEntities(enumcreaturetype.getCreatureClass()) <= limit * eligibleChunksForSpawning.size() / 256)   // CraftBukkit - use per-world limits
                {
                    Iterator iterator = this.eligibleChunksForSpawning.keySet().iterator();
                    label110:

                    while (iterator.hasNext())
                    {
                        // CraftBukkit start
                        long key = ((Long) iterator.next()).longValue();

                        if (!(this.eligibleChunksForSpawning.get(key)))
                        {
                            ChunkPosition chunkposition = func_151350_a(p_77192_1_, LongHash.msw(key), LongHash.lsw(key));
                            // CraftBukkit end
                            int j1 = chunkposition.chunkPosX;
                            int k1 = chunkposition.chunkPosY;
                            int l1 = chunkposition.chunkPosZ;

                            if (!p_77192_1_.getBlock(j1, k1, l1).isNormalCube() && p_77192_1_.getBlock(j1, k1, l1).getMaterial() == enumcreaturetype.getCreatureMaterial())
                            {
                                int i2 = 0;
                                int j2 = 0;

                                while (j2 < 3)
                                {
                                    int k2 = j1;
                                    int l2 = k1;
                                    int i3 = l1;
                                    byte b1 = 6;
                                    BiomeGenBase.SpawnListEntry spawnlistentry = null;
                                    IEntityLivingData ientitylivingdata = null;
                                    int j3 = 0;

                                    while (true)
                                    {
                                        if (j3 < 4)
                                        {
                                            label103:
                                            {
                                                k2 += p_77192_1_.rand.nextInt(b1) - p_77192_1_.rand.nextInt(b1);
                                                l2 += p_77192_1_.rand.nextInt(1) - p_77192_1_.rand.nextInt(1);
                                                i3 += p_77192_1_.rand.nextInt(b1) - p_77192_1_.rand.nextInt(b1);

                                                if (canCreatureTypeSpawnAtLocation(enumcreaturetype, p_77192_1_, k2, l2, i3))
                                                {
                                                    float f = (float)k2 + 0.5F;
                                                    float f1 = (float)l2;
                                                    float f2 = (float)i3 + 0.5F;

                                                    if (p_77192_1_.getClosestPlayer((double)f, (double)f1, (double)f2, 24.0D) == null)
                                                    {
                                                        float f3 = f - (float)chunkcoordinates.posX;
                                                        float f4 = f1 - (float)chunkcoordinates.posY;
                                                        float f5 = f2 - (float)chunkcoordinates.posZ;
                                                        float f6 = f3 * f3 + f4 * f4 + f5 * f5;

                                                        if (f6 >= 576.0F)
                                                        {
                                                            if (spawnlistentry == null)
                                                            {
                                                                spawnlistentry = p_77192_1_.spawnRandomCreature(enumcreaturetype, k2, l2, i3);

                                                                if (spawnlistentry == null)
                                                                {
                                                                    break label103;
                                                                }
                                                            }

                                                            EntityLiving entityliving;

                                                            try
                                                            {
                                                                entityliving = (EntityLiving)spawnlistentry.entityClass.getConstructor(new Class[] {World.class}).newInstance(new Object[] {p_77192_1_});
                                                            }
                                                            catch (Exception exception)
                                                            {
                                                                exception.printStackTrace();
                                                                return i;
                                                            }

                                                            entityliving.setLocationAndAngles((double)f, (double)f1, (double)f2, p_77192_1_.rand.nextFloat() * 360.0F, 0.0F);

                                                            Result canSpawn = ForgeEventFactory.canEntitySpawn(entityliving, p_77192_1_, f, f1, f2);
                                                            if (canSpawn == Result.ALLOW || (canSpawn == Result.DEFAULT && entityliving.getCanSpawnHere()))
                                                            {
                                                                ++i2;
                                                                // CraftBukkit start - Added a reason for spawning this creature, moved entityliving.onSpawnWithEgg(ientitylivingdata) up
                                                                p_77192_1_.addEntity(entityliving, SpawnReason.NATURAL);
                                                                if (!ForgeEventFactory.doSpecialSpawn(entityliving, p_77192_1_, f, f1, f2))
                                                                {
                                                                    ientitylivingdata = entityliving.onSpawnWithEgg(ientitylivingdata);
                                                                }

                                                                if (j2 >= ForgeEventFactory.getMaxSpawnPackSize(entityliving))
                                                                {
                                                                    continue label110;
                                                                }
                                                            }

                                                            i += i2;
                                                        }
                                                    }
                                                }

                                                ++j3;
                                                continue;
                                            }
                                        }

                                        ++j2;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return i;
        }
    }

    public static boolean canCreatureTypeSpawnAtLocation(EnumCreatureType p_77190_0_, World p_77190_1_, int p_77190_2_, int p_77190_3_, int p_77190_4_)
    {
        if (p_77190_0_.getCreatureMaterial() == Material.water)
        {
            return p_77190_1_.getBlock(p_77190_2_, p_77190_3_, p_77190_4_).getMaterial().isLiquid() && p_77190_1_.getBlock(p_77190_2_, p_77190_3_ - 1, p_77190_4_).getMaterial().isLiquid() && !p_77190_1_.getBlock(p_77190_2_, p_77190_3_ + 1, p_77190_4_).isNormalCube();
        }
        else if (!World.doesBlockHaveSolidTopSurface(p_77190_1_, p_77190_2_, p_77190_3_ - 1, p_77190_4_))
        {
            return false;
        }
        else
        {
            Block block = p_77190_1_.getBlock(p_77190_2_, p_77190_3_ - 1, p_77190_4_);
            boolean spawnBlock = block.canCreatureSpawn(p_77190_0_, p_77190_1_, p_77190_2_, p_77190_3_ - 1, p_77190_4_);
            return spawnBlock && block != Blocks.bedrock && !p_77190_1_.getBlock(p_77190_2_, p_77190_3_, p_77190_4_).isNormalCube() && !p_77190_1_.getBlock(p_77190_2_, p_77190_3_, p_77190_4_).getMaterial().isLiquid() && !p_77190_1_.getBlock(p_77190_2_, p_77190_3_ + 1, p_77190_4_).isNormalCube();
        }
    }

    public static void performWorldGenSpawning(World p_77191_0_, BiomeGenBase p_77191_1_, int p_77191_2_, int p_77191_3_, int p_77191_4_, int p_77191_5_, Random p_77191_6_)
    {
        List list = p_77191_1_.getSpawnableList(EnumCreatureType.creature);

        if (!list.isEmpty())
        {
            while (p_77191_6_.nextFloat() < p_77191_1_.getSpawningChance())
            {
                BiomeGenBase.SpawnListEntry spawnlistentry = (BiomeGenBase.SpawnListEntry)WeightedRandom.getRandomItem(p_77191_0_.rand, list);
                IEntityLivingData ientitylivingdata = null;
                int i1 = spawnlistentry.minGroupCount + p_77191_6_.nextInt(1 + spawnlistentry.maxGroupCount - spawnlistentry.minGroupCount);
                int j1 = p_77191_2_ + p_77191_6_.nextInt(p_77191_4_);
                int k1 = p_77191_3_ + p_77191_6_.nextInt(p_77191_5_);
                int l1 = j1;
                int i2 = k1;

                for (int j2 = 0; j2 < i1; ++j2)
                {
                    boolean flag = false;

                    for (int k2 = 0; !flag && k2 < 4; ++k2)
                    {
                        int l2 = p_77191_0_.getTopSolidOrLiquidBlock(j1, k1);

                        if (canCreatureTypeSpawnAtLocation(EnumCreatureType.creature, p_77191_0_, j1, l2, k1))
                        {
                            float f = (float)j1 + 0.5F;
                            float f1 = (float)l2;
                            float f2 = (float)k1 + 0.5F;
                            EntityLiving entityliving;

                            try
                            {
                                entityliving = (EntityLiving)spawnlistentry.entityClass.getConstructor(new Class[] {World.class}).newInstance(new Object[] {p_77191_0_});
                            }
                            catch (Exception exception)
                            {
                                exception.printStackTrace();
                                continue;
                            }

                            entityliving.setLocationAndAngles((double)f, (double)f1, (double)f2, p_77191_6_.nextFloat() * 360.0F, 0.0F);
                            // CraftBukkit start - Added a reason for spawning this creature, moved entityliving.a(ientitylivingdata) up
                            ientitylivingdata = entityliving.onSpawnWithEgg(ientitylivingdata);
                            p_77191_0_.addEntity(entityliving, SpawnReason.CHUNK_GEN);
                            // CraftBukkit end
                            flag = true;
                        }

                        j1 += p_77191_6_.nextInt(5) - p_77191_6_.nextInt(5);

                        for (k1 += p_77191_6_.nextInt(5) - p_77191_6_.nextInt(5); j1 < p_77191_2_ || j1 >= p_77191_2_ + p_77191_4_ || k1 < p_77191_3_ || k1 >= p_77191_3_ + p_77191_4_; k1 = i2 + p_77191_6_.nextInt(5) - p_77191_6_.nextInt(5))
                        {
                            j1 = l1 + p_77191_6_.nextInt(5) - p_77191_6_.nextInt(5);
                        }
                    }
                }
            }
        }
    }
}