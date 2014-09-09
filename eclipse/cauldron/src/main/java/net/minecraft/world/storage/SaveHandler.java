package net.minecraft.world.storage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.StartupQuery;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import java.util.UUID;

import org.bukkit.craftbukkit.entity.CraftPlayer;
// CraftBukkit end
import cpw.mods.fml.common.registry.GameData; // Cauldron

public class SaveHandler implements ISaveHandler, IPlayerFileData
{
    private static final Logger logger = LogManager.getLogger();
    private final File worldDirectory;
    private final File playersDirectory;
    private final File mapDataDir;
    private final long initializationTime = MinecraftServer.getSystemTimeMillis();
    private final String saveDirectoryName;
    private UUID uuid = null; // CraftBukkit
    private static boolean initializedBukkit = false; // Cauldron
    private static final String __OBFID = "CL_00000585";

    public SaveHandler(File p_i2146_1_, String p_i2146_2_, boolean p_i2146_3_)
    {
        this.worldDirectory = new File(p_i2146_1_, p_i2146_2_);
        this.worldDirectory.mkdirs();
        this.playersDirectory = new File(this.worldDirectory, "playerdata");
        this.mapDataDir = new File(this.worldDirectory, "data");
        this.mapDataDir.mkdirs();
        this.saveDirectoryName = p_i2146_2_;

        if (p_i2146_3_)
        {
            this.playersDirectory.mkdirs();
        }

        this.setSessionLock();
    }

    private void setSessionLock()
    {
        try
        {
            File file1 = new File(this.worldDirectory, "session.lock");
            DataOutputStream dataoutputstream = new DataOutputStream(new FileOutputStream(file1));

            try
            {
                dataoutputstream.writeLong(this.initializationTime);
            }
            finally
            {
                dataoutputstream.close();
            }
        }
        catch (IOException ioexception)
        {
            ioexception.printStackTrace();
            throw new RuntimeException("Failed to check session lock for world " + this.worldDirectory + ", aborting"); // Cauldron
        }
    }

    public File getWorldDirectory()
    {
        return this.worldDirectory;
    }

    public void checkSessionLock() throws MinecraftException
    {
        try
        {
            File file1 = new File(this.worldDirectory, "session.lock");
            DataInputStream datainputstream = new DataInputStream(new FileInputStream(file1));

            try
            {
                if (datainputstream.readLong() != this.initializationTime)
                {
                    throw new MinecraftException("The save folder for world " + this.worldDirectory + " is being accessed from another location, aborting"); // Cauldron
                }
            }
            finally
            {
                datainputstream.close();
            }
        }
        catch (IOException ioexception)
        {
            // Cauldron start
            ioexception.printStackTrace();
            throw new MinecraftException("Failed to check session lock for world " + this.worldDirectory + ", aborting");
            // Cauldron end
        }
    }

    public IChunkLoader getChunkLoader(WorldProvider p_75763_1_)
    {
        throw new RuntimeException("Old Chunk Storage is no longer supported.");
    }

    public WorldInfo loadWorldInfo()
    {
        File file1 = new File(this.worldDirectory, "level.dat");
        NBTTagCompound nbttagcompound;
        NBTTagCompound nbttagcompound1;

        WorldInfo worldInfo = null;

        if (file1.exists())
        {
            try
            {
                nbttagcompound = CompressedStreamTools.readCompressed(new FileInputStream(file1));
                nbttagcompound1 = nbttagcompound.getCompoundTag("Data");
                worldInfo = new WorldInfo(nbttagcompound1);
                FMLCommonHandler.instance().handleWorldDataLoad(this, worldInfo, nbttagcompound);
                this.initBukkitData(worldInfo); // Cauldron
                return worldInfo;
            }
            catch (StartupQuery.AbortedException e)
            {
                throw e;
            }
            catch (Exception exception1)
            {
                exception1.printStackTrace();
            }
        }

        FMLCommonHandler.instance().confirmBackupLevelDatUse(this);
        file1 = new File(this.worldDirectory, "level.dat_old");

        if (file1.exists())
        {
            try
            {
                nbttagcompound = CompressedStreamTools.readCompressed(new FileInputStream(file1));
                nbttagcompound1 = nbttagcompound.getCompoundTag("Data");
                worldInfo = new WorldInfo(nbttagcompound1);
                FMLCommonHandler.instance().handleWorldDataLoad(this, worldInfo, nbttagcompound);
                this.initBukkitData(worldInfo); // Cauldron
                return worldInfo;
            }
            catch (StartupQuery.AbortedException e)
            {
                throw e;
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }
        }
        this.initBukkitData(worldInfo); // Cauldron
        return null;
    }

    public void saveWorldInfoWithPlayer(WorldInfo p_75755_1_, NBTTagCompound p_75755_2_)
    {
        NBTTagCompound nbttagcompound1 = p_75755_1_.cloneNBTCompound(p_75755_2_);
        NBTTagCompound nbttagcompound2 = new NBTTagCompound();
        nbttagcompound2.setTag("Data", nbttagcompound1);

        FMLCommonHandler.instance().handleWorldDataSave(this, p_75755_1_, nbttagcompound2);

        try
        {
            File file1 = new File(this.worldDirectory, "level.dat_new");
            File file2 = new File(this.worldDirectory, "level.dat_old");
            File file3 = new File(this.worldDirectory, "level.dat");
            CompressedStreamTools.writeCompressed(nbttagcompound2, new FileOutputStream(file1));

            if (file2.exists())
            {
                file2.delete();
            }

            file3.renameTo(file2);

            if (file3.exists())
            {
                file3.delete();
            }

            file1.renameTo(file3);

            if (file1.exists())
            {
                file1.delete();
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    public void saveWorldInfo(WorldInfo p_75761_1_)
    {
        NBTTagCompound nbttagcompound = p_75761_1_.getNBTTagCompound();
        NBTTagCompound nbttagcompound1 = new NBTTagCompound();
        nbttagcompound1.setTag("Data", nbttagcompound);

        FMLCommonHandler.instance().handleWorldDataSave(this, p_75761_1_, nbttagcompound1);

        try
        {
            File file1 = new File(this.worldDirectory, "level.dat_new");
            File file2 = new File(this.worldDirectory, "level.dat_old");
            File file3 = new File(this.worldDirectory, "level.dat");
            CompressedStreamTools.writeCompressed(nbttagcompound1, new FileOutputStream(file1));

            if (file2.exists())
            {
                file2.delete();
            }

            file3.renameTo(file2);

            if (file3.exists())
            {
                file3.delete();
            }

            file1.renameTo(file3);

            if (file1.exists())
            {
                file1.delete();
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    public void writePlayerData(EntityPlayer p_75753_1_)
    {
        try
        {
            NBTTagCompound nbttagcompound = new NBTTagCompound();
            p_75753_1_.writeToNBT(nbttagcompound);
            File file1 = new File(this.playersDirectory, p_75753_1_.getUniqueID().toString() + ".dat.tmp");
            File file2 = new File(this.playersDirectory, p_75753_1_.getUniqueID().toString() + ".dat");
            CompressedStreamTools.writeCompressed(nbttagcompound, new FileOutputStream(file1));

            if (file2.exists())
            {
                file2.delete();
            }

            file1.renameTo(file2);
            net.minecraftforge.event.ForgeEventFactory.firePlayerSavingEvent(p_75753_1_, this.playersDirectory, p_75753_1_.getUniqueID().toString());
        }
        catch (Exception exception)
        {
            logger.warn("Failed to save player data for " + p_75753_1_.getCommandSenderName());
        }
    }

    public NBTTagCompound readPlayerData(EntityPlayer p_75752_1_)
    {
        NBTTagCompound nbttagcompound = null;

        try
        {
            File file1 = new File(this.playersDirectory, p_75752_1_.getUniqueID().toString() + ".dat");

            if (file1.exists() && file1.isFile())
            {
                nbttagcompound = CompressedStreamTools.readCompressed(new FileInputStream(file1));
            }
        }
        catch (Exception exception)
        {
            logger.warn("Failed to load player data for " + p_75752_1_.getCommandSenderName());
        }

        if (nbttagcompound != null)
        {
            // CraftBukkit start
            if (p_75752_1_ instanceof EntityPlayerMP)
            {
                CraftPlayer player = (CraftPlayer) p_75752_1_.getBukkitEntity(); // Cauldron
                // Only update first played if it is older than the one we have
                long modified = new File(playersDirectory, p_75752_1_.getCommandSenderName() + ".dat").lastModified();
                if (modified < player.getFirstPlayed()) {
                    player.setFirstPlayed(modified);
                }
            }
            // CraftBukkit end

            p_75752_1_.readFromNBT(nbttagcompound);
        }

        net.minecraftforge.event.ForgeEventFactory.firePlayerLoadingEvent(p_75752_1_, playersDirectory, p_75752_1_.getUniqueID().toString());
        return nbttagcompound;
    }

    // CraftBukkit start
    public NBTTagCompound getPlayerData(String par1Str)
    {
        try
        {
            File file1 = new File(this.playersDirectory, par1Str + ".dat");

            if (file1.exists())
            {
                return CompressedStreamTools.readCompressed(new FileInputStream(file1));
            }
        }
        catch (Exception exception)
        {
            logger.warn("Failed to load player data for " + par1Str);
        }

        return null;
    }
    // CraftBukkit end

    public IPlayerFileData getSaveHandler()
    {
        return this;
    }

    public String[] getAvailablePlayerDat()
    {
        String[] astring = this.playersDirectory.list();

        for (int i = 0; i < astring.length; ++i)
        {
            if (astring[i].endsWith(".dat"))
            {
                astring[i] = astring[i].substring(0, astring[i].length() - 4);
            }
        }

        return astring;
    }

    public void flush() {}

    public File getMapFileFromName(String p_75758_1_)
    {
        return new File(this.mapDataDir, p_75758_1_ + ".dat");
    }

    public String getWorldDirectoryName()
    {
        return this.saveDirectoryName;
    }

    // CraftBukkit start
    public UUID getUUID()
    {
        if (uuid != null)
        {
            return uuid;
        }

        File file1 = new File(this.worldDirectory, "uid.dat");

        if (file1.exists())
        {
            DataInputStream dis = null;

            try
            {
                dis = new DataInputStream(new FileInputStream(file1));
                return uuid = new UUID(dis.readLong(), dis.readLong());
            }
            catch (IOException ex)
            {
                logger.warn("Failed to read " + file1 + ", generating new random UUID", ex);
            }
            finally
            {
                if (dis != null)
                {
                    try
                    {
                        dis.close();
                    }
                    catch (IOException ex)
                    {
                        // NOOP
                    }
                }
            }
        }

        uuid = UUID.randomUUID();
        DataOutputStream dos = null;

        try
        {
            dos = new DataOutputStream(new FileOutputStream(file1));
            dos.writeLong(uuid.getMostSignificantBits());
            dos.writeLong(uuid.getLeastSignificantBits());
        }
        catch (IOException ex)
        {
            logger.warn("Failed to write " + file1, ex);
        }
        finally
        {
            if (dos != null)
            {
                try
                {
                    dos.close();
                }
                catch (IOException ex)
                {
                    // NOOP
                }
            }
        }

        return uuid;
    }

    public File getPlayerDir()
    {
        return playersDirectory;
    }
    // CraftBukkit end

    // Cauldron start
    public void initBukkitData(WorldInfo worldInfo)
    {
        // inject bukkit materials before plugins load
        if (!this.initializedBukkit && (worldInfo == null || worldInfo.getDimension() == 0))
        {
            GameData.injectBlockBukkitMaterials();
            GameData.injectItemBukkitMaterials();
            // since we modify bukkit enums, we need to guarantee that plugins are
            // loaded after all mods have been loaded by FML to avoid race conditions.
            MinecraftServer.getServer().server.loadPlugins();
            MinecraftServer.getServer().server.enablePlugins(org.bukkit.plugin.PluginLoadOrder.STARTUP);
            this.initializedBukkit = true;
        }
    }
    // Cauldron end
}