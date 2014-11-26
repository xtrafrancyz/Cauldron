package net.minecraft.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
// CraftBukkit start
import java.util.ArrayList;
import com.google.common.base.Joiner;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.player.EntityPlayerMP;
// CraftBukkit end

public class TileEntityCommandBlock extends TileEntity implements ICommandSender
{
    private int succesCount;

    /** The command this block will execute when powered. */
    public String command = ""; // CraftBukkit - private -> public

    /** The name of command sender (usually username, but possibly "Rcon") */
    private String commandSenderName = "@";
    // CraftBukkit start
    private final org.bukkit.command.BlockCommandSender sender;

    public TileEntityCommandBlock()
    {
        sender = new org.bukkit.craftbukkit.command.CraftBlockCommandSender(this);
    }
    // CraftBukkit end

    /**
     * Sets the command this block will execute when powered.
     */
    public void setCommand(String par1Str)
    {
        this.command = par1Str;
        this.onInventoryChanged();
    }

    /**
     * Execute the command, called when the command block is powered.
     */
    public int executeCommandOnPowered(World par1World)
    {
        if (par1World.isRemote)
        {
            return 0;
        }
        else
        {
            MinecraftServer minecraftserver = MinecraftServer.getServer();

            if (minecraftserver != null && minecraftserver.isCommandBlockEnabled())
            {
                // CraftBukkit start - Handle command block commands using Bukkit dispatcher
                org.bukkit.command.SimpleCommandMap commandMap = minecraftserver.server.getCommandMap();
                Joiner joiner = Joiner.on(" ");
                String command = this.command;

                if (this.command.startsWith("/"))
                {
                    command = this.command.substring(1);
                }

                String[] args = command.split(" ");
                ArrayList<String[]> commands = new ArrayList<String[]>();

                // Block disallowed commands
                if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("op") ||
                        args[0].equalsIgnoreCase("deop") || args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("ban-ip") ||
                        args[0].equalsIgnoreCase("pardon") || args[0].equalsIgnoreCase("pardon-ip") || args[0].equalsIgnoreCase("reload"))
                {
                    return 0;
                }

                // Make sure this is a valid command
                if (commandMap.getCommand(args[0]) == null)
                {
                    // Cauldron start -- execute using the vanilla command manager if it isn't in the bukkit command map
                    net.minecraft.command.ICommandManager icommandmanager = minecraftserver.getCommandManager();
                    return icommandmanager.executeCommand(this, this.command);
                    // Cauldron end
                }

                // If the world has no players don't run
                if (this.worldObj.playerEntities.isEmpty())
                {
                    return 0;
                }

                // testfor command requires special handling
                if (args[0].equalsIgnoreCase("testfor"))
                {
                    if (args.length < 2)
                    {
                        return 0;
                    }

                    EntityPlayerMP[] players = PlayerSelector.matchPlayers(this, args[1]);

                    if (players != null && players.length > 0)
                    {
                        return players.length;
                    }
                    else
                    {
                        EntityPlayerMP player = MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername(args[1]);

                        if (player == null)
                        {
                            return 0;
                        }
                        else
                        {
                            return 1;
                        }
                    }
                }

                commands.add(args);
                // Find positions of command block syntax, if any
                ArrayList<String[]> newCommands = new ArrayList<String[]>();

                for (int i = 0; i < args.length; i++)
                {
                    if (PlayerSelector.hasArguments(args[i]))
                    {
                        for (int j = 0; j < commands.size(); j++)
                        {
                            newCommands.addAll(this.buildCommands(commands.get(j), i));
                        }

                        ArrayList<String[]> temp = commands;
                        commands = newCommands;
                        newCommands = temp;
                        newCommands.clear();
                    }
                }

                int completed = 0;

                // Now dispatch all of the commands we ended up with
                for (int i = 0; i < commands.size(); i++)
                {
                    try
                    {
                        if (commandMap.dispatch(sender, joiner.join(java.util.Arrays.asList(commands.get(i)))))
                        {
                            completed++;
                        }
                    }
                    catch (Throwable exception)
                    {
                        minecraftserver.getLogAgent().logWarningException(String.format("CommandBlock at (%d,%d,%d) failed to handle command", this.xCoord, this.yCoord, this.zCoord), exception);
                    }
                }

                return completed;
                // CraftBukkit end
            }
            else
            {
                return 0;
            }
        }
    }

    // CraftBukkit start
    private ArrayList<String[]> buildCommands(String[] args, int pos)
    {
        ArrayList<String[]> commands = new ArrayList<String[]>();
        EntityPlayerMP[] players = PlayerSelector.matchPlayers(this, args[pos]);

        if (players != null)
        {
            for (EntityPlayerMP player : players)
            {
                if (player.worldObj != this.worldObj)
                {
                    continue;
                }

                String[] command = args.clone();
                command[pos] = player.getEntityName();
                commands.add(command);
            }
        }

        return commands;
    }
    // CraftBukkit end

    @SideOnly(Side.CLIENT)

    /**
     * Return the command this command block is set to execute.
     */
    public String getCommand()
    {
        return this.command;
    }

    /**
     * Gets the name of this command sender (usually username, but possibly "Rcon")
     */
    public String getCommandSenderName()
    {
        return this.commandSenderName;
    }

    /**
     * Sets the name of the command sender
     */
    public void setCommandSenderName(String par1Str)
    {
        this.commandSenderName = par1Str;
    }

    public void sendChatToPlayer(ChatMessageComponent par1ChatMessageComponent) {}

    /**
     * Returns true if the command sender is allowed to use the given command.
     */
    public boolean canCommandSenderUseCommand(int par1, String par2Str)
    {
        return par1 <= 2;
    }

    /**
     * Writes a tile entity to NBT.
     */
    public void writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.writeToNBT(par1NBTTagCompound);
        par1NBTTagCompound.setString("Command", this.command);
        par1NBTTagCompound.setInteger("SuccessCount", this.succesCount);
        par1NBTTagCompound.setString("CustomName", this.commandSenderName);
    }

    /**
     * Reads a tile entity from NBT.
     */
    public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readFromNBT(par1NBTTagCompound);
        this.command = par1NBTTagCompound.getString("Command");
        this.succesCount = par1NBTTagCompound.getInteger("SuccessCount");

        if (par1NBTTagCompound.hasKey("CustomName"))
        {
            this.commandSenderName = par1NBTTagCompound.getString("CustomName");
        }
    }

    /**
     * Return the position for this command sender.
     */
    public ChunkCoordinates getPlayerCoordinates()
    {
        return new ChunkCoordinates(this.xCoord, this.yCoord, this.zCoord);
    }

    public World getEntityWorld()
    {
        return this.getWorldObj();
    }

    /**
     * Overriden in a sign to provide the text.
     */
    public Packet getDescriptionPacket()
    {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        this.writeToNBT(nbttagcompound);
        return new Packet132TileEntityData(this.xCoord, this.yCoord, this.zCoord, 2, nbttagcompound);
    }

    public int getSignalStrength()
    {
        return this.succesCount;
    }

    public void setSignalStrength(int par1)
    {
        this.succesCount = par1;
    }

    // Cauldron start
    @Override
    public boolean canUpdate()
    {
        return false;
    }
    // Cauldron end
}