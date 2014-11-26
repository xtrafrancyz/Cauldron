package net.minecraft.server.management;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.craftbukkit.chunkio.ChunkIOExecutor;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet51MapChunk;
import net.minecraft.network.packet.Packet52MultiBlockChange;
import net.minecraft.network.packet.Packet53BlockChange;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeDummyContainer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;

public class PlayerInstance
{
    private final List playersInChunk;

    /** note: this is final */
    private final ChunkCoordIntPair chunkLocation;
    private short[] locationOfBlockChange;
    private int numberOfTilesToUpdate;

    /**
     * Integer field where each bit means to make update 16x16x16 division of chunk (from bottom).
     */
    private int flagsYAreasToUpdate;

    /** time what is using when chunk InhabitedTime is being calculated */
    private long previousWorldTime;
    final PlayerManager thePlayerManager;
    // CraftBukkit start
    private final HashMap<EntityPlayerMP, Runnable> players = new HashMap<EntityPlayerMP, Runnable>();
    private boolean loaded = false;
    private Runnable loadedRunnable = new Runnable()
    {
        public void run()
        {
            PlayerInstance.this.loaded = true;
        }
    };
    // CraftBukkit end

    public PlayerInstance(PlayerManager par1PlayerManager, int par2, int par3)
    {
        this.thePlayerManager = par1PlayerManager;
        this.playersInChunk = new ArrayList();
        this.locationOfBlockChange = new short[64];
        this.chunkLocation = new ChunkCoordIntPair(par2, par3);
        par1PlayerManager.getWorldServer().theChunkProviderServer.getChunkAt(par2, par3, this.loadedRunnable); // CraftBukkit
    }

    public void addPlayer(final EntityPlayerMP par1EntityPlayerMP)   // CraftBukkit - added final to argument
    {
        if (this.playersInChunk.contains(par1EntityPlayerMP))
        {
            throw new IllegalStateException("Failed to add player. " + par1EntityPlayerMP + " already is in chunk " + this.chunkLocation.chunkXPos + ", " + this.chunkLocation.chunkZPos);
        }
        else
        {
            if (this.playersInChunk.isEmpty())
            {
                this.previousWorldTime = PlayerManager.getWorldServer(this.thePlayerManager).getTotalWorldTime();
            }

            this.playersInChunk.add(par1EntityPlayerMP);
            // CraftBukkit start
            Runnable playerRunnable;

            if (this.loaded)
            {
                playerRunnable = null;
                par1EntityPlayerMP.loadedChunks.add(this.chunkLocation);
            }
            else
            {
                playerRunnable = new Runnable()
                {
                    public void run()
                    {
                        par1EntityPlayerMP.loadedChunks.add(PlayerInstance.this.chunkLocation);
                    }
                };
                this.thePlayerManager.getWorldServer().theChunkProviderServer.getChunkAt(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos, playerRunnable);
            }

            this.players.put(par1EntityPlayerMP, playerRunnable);
            // CraftBukkit end
        }
    }

    public void removePlayer(EntityPlayerMP par1EntityPlayerMP)
    {
        if (this.playersInChunk.contains(par1EntityPlayerMP))
        {
            // CraftBukkit start - If we haven't loaded yet don't load the chunk just so we can clean it up
            if (!this.loaded)
            {
                ChunkIOExecutor.dropQueuedChunkLoad(this.thePlayerManager.getWorldServer(), this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos, this.players.get(par1EntityPlayerMP));
                this.playersInChunk.remove(par1EntityPlayerMP);
                this.players.remove(par1EntityPlayerMP);

                if (this.playersInChunk.isEmpty())
                {
                    ChunkIOExecutor.dropQueuedChunkLoad(this.thePlayerManager.getWorldServer(), this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos, this.loadedRunnable);
                    long i = (long) this.chunkLocation.chunkXPos + 2147483647L | (long) this.chunkLocation.chunkZPos + 2147483647L << 32;
                    PlayerManager.getChunkWatchers(this.thePlayerManager).remove(i);
                    PlayerManager.getChunkWatcherList(this.thePlayerManager).remove(this);
                }

                return;
            }

            // CraftBukkit end
            Chunk chunk = this.thePlayerManager.getWorldServer().getChunkFromChunkCoords(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos);
            par1EntityPlayerMP.playerNetServerHandler.sendPacketToPlayer(new Packet51MapChunk(chunk, true, 0));
            this.players.remove(par1EntityPlayerMP); // CraftBukkit
            this.playersInChunk.remove(par1EntityPlayerMP);
            par1EntityPlayerMP.loadedChunks.remove(this.chunkLocation);

            MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.UnWatch(chunkLocation, par1EntityPlayerMP));

            if (this.playersInChunk.isEmpty())
            {
                long i = (long)this.chunkLocation.chunkXPos + 2147483647L | (long)this.chunkLocation.chunkZPos + 2147483647L << 32;
                this.increaseInhabitedTime(chunk);
                PlayerManager.getChunkWatchers(this.thePlayerManager).remove(i);
                PlayerManager.getChunkWatcherList(this.thePlayerManager).remove(this);

                if (this.numberOfTilesToUpdate > 0)
                {
                    PlayerManager.getChunkWatchersWithPlayers(this.thePlayerManager).remove(this);
                }

                this.thePlayerManager.getWorldServer().theChunkProviderServer.unloadChunksIfNotNearSpawn(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos);
            }
        }
    }

    /**
     * This method currently only increases chunk inhabited time. Extension is possible in next versions
     */
    public void processChunk()
    {
        this.increaseInhabitedTime(PlayerManager.getWorldServer(this.thePlayerManager).getChunkFromChunkCoords(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos));
    }

    /**
     * Increases chunk inhabited time every 8000 ticks
     */
    private void increaseInhabitedTime(Chunk par1Chunk)
    {
        par1Chunk.inhabitedTime += PlayerManager.getWorldServer(this.thePlayerManager).getTotalWorldTime() - this.previousWorldTime;
        this.previousWorldTime = PlayerManager.getWorldServer(this.thePlayerManager).getTotalWorldTime();
    }

    public void flagChunkForUpdate(int par1, int par2, int par3)
    {
        if (this.numberOfTilesToUpdate == 0)
        {
            PlayerManager.getChunkWatchersWithPlayers(this.thePlayerManager).add(this);
        }

        this.flagsYAreasToUpdate |= 1 << (par2 >> 4);

        //if (this.numberOfTilesToUpdate < 64) //Forge; Cache everything, so always run
        {
            short short1 = (short)(par1 << 12 | par3 << 8 | par2);

            for (int l = 0; l < this.numberOfTilesToUpdate; ++l)
            {
                if (this.locationOfBlockChange[l] == short1)
                {
                    return;
                }
            }

            if (numberOfTilesToUpdate == locationOfBlockChange.length)
            {
                locationOfBlockChange = Arrays.copyOf(locationOfBlockChange, locationOfBlockChange.length << 1);
            }
            this.locationOfBlockChange[this.numberOfTilesToUpdate++] = short1;
        }
    }

    public void sendToAllPlayersWatchingChunk(Packet par1Packet)
    {
        for (int i = 0; i < this.playersInChunk.size(); ++i)
        {
            EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playersInChunk.get(i);

            if (!entityplayermp.loadedChunks.contains(this.chunkLocation))
            {
                entityplayermp.playerNetServerHandler.sendPacketToPlayer(par1Packet);
            }
        }
    }

    public void sendChunkUpdate()
    {
        if (this.numberOfTilesToUpdate != 0)
        {
            int i;
            int j;
            int k;

            if (this.numberOfTilesToUpdate == 1)
            {
                i = this.chunkLocation.chunkXPos * 16 + (this.locationOfBlockChange[0] >> 12 & 15);
                j = this.locationOfBlockChange[0] & 255;
                k = this.chunkLocation.chunkZPos * 16 + (this.locationOfBlockChange[0] >> 8 & 15);
                this.sendToAllPlayersWatchingChunk(new Packet53BlockChange(i, j, k, PlayerManager.getWorldServer(this.thePlayerManager)));

                if (PlayerManager.getWorldServer(this.thePlayerManager).blockHasTileEntity(i, j, k))
                {
                    this.sendTileToAllPlayersWatchingChunk(PlayerManager.getWorldServer(this.thePlayerManager).getBlockTileEntity(i, j, k));
                }
            }
            else
            {
                int l;

                if (this.numberOfTilesToUpdate >= ForgeDummyContainer.clumpingThreshold)
                {
                    i = this.chunkLocation.chunkXPos * 16;
                    j = this.chunkLocation.chunkZPos * 16;
                    this.sendToAllPlayersWatchingChunk(new Packet51MapChunk(PlayerManager.getWorldServer(this.thePlayerManager).getChunkFromChunkCoords(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos), false, this.flagsYAreasToUpdate));

                    /* Forge: Grabs ALL tile entities is costly on a modded server, only send needed ones
                    for (k = 0; k < 16; ++k)
                    {
                        if ((this.flagsYAreasToUpdate & 1 << k) != 0)
                        {
                            l = k << 4;
                            List list = PlayerManager.getWorldServer(this.thePlayerManager).getAllTileEntityInBox(i, l, j, i + 16, l + 16, j + 16);

                            for (int i1 = 0; i1 < list.size(); ++i1)
                            {
                                this.sendTileToAllPlayersWatchingChunk((TileEntity)list.get(i1));
                            }
                        }
                    }
                    */
                }
                else
                {
                    this.sendToAllPlayersWatchingChunk(new Packet52MultiBlockChange(this.chunkLocation.chunkXPos, this.chunkLocation.chunkZPos, this.locationOfBlockChange, this.numberOfTilesToUpdate, PlayerManager.getWorldServer(this.thePlayerManager)));
                }

                { //Forge: Send only the tile entities that are updated, Adding this brace lets us keep the indent and the patch small
                    for (i = 0; i < this.numberOfTilesToUpdate; ++i)
                    {
                        j = this.chunkLocation.chunkXPos * 16 + (this.locationOfBlockChange[i] >> 12 & 15);
                        k = this.locationOfBlockChange[i] & 255;
                        l = this.chunkLocation.chunkZPos * 16 + (this.locationOfBlockChange[i] >> 8 & 15);

                        if (PlayerManager.getWorldServer(this.thePlayerManager).blockHasTileEntity(j, k, l))
                        {
                            this.sendTileToAllPlayersWatchingChunk(PlayerManager.getWorldServer(this.thePlayerManager).getBlockTileEntity(j, k, l));
                        }
                    }
                }
            }

            this.numberOfTilesToUpdate = 0;
            this.flagsYAreasToUpdate = 0;
        }
    }

    private void sendTileToAllPlayersWatchingChunk(TileEntity par1TileEntity)
    {
        if (par1TileEntity != null)
        {
            Packet packet = par1TileEntity.getDescriptionPacket();

            if (packet != null)
            {
                this.sendToAllPlayersWatchingChunk(packet);
            }
        }
    }

    static ChunkCoordIntPair getChunkLocation(PlayerInstance par0PlayerInstance)
    {
        return par0PlayerInstance.chunkLocation;
    }

    static List getPlayersInChunk(PlayerInstance par0PlayerInstance)
    {
        return par0PlayerInstance.playersInChunk;
    }
}