package net.minecraft.command.server;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityCommandBlockListener;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

// CraftBukkit start
import java.util.ArrayList;
import org.apache.logging.log4j.Level;
import org.bukkit.craftbukkit.command.VanillaCommandWrapper;
import com.google.common.base.Joiner;
import net.minecraft.command.PlayerSelector;
import net.minecraft.entity.EntityMinecartCommandBlockListener;
import net.minecraft.entity.player.EntityPlayerMP;
// CraftBukkit end

public abstract class CommandBlockLogic implements ICommandSender
{
    private static final SimpleDateFormat field_145766_a = new SimpleDateFormat("HH:mm:ss");
    private int field_145764_b;
    private boolean field_145765_c = true;
    private IChatComponent field_145762_d = null;
    public String field_145763_e = ""; // CraftBukkit - private -> public
    private String field_145761_f = "@";
    protected org.bukkit.command.CommandSender sender; // CraftBukkit - add sender;
    private static final String __OBFID = "CL_00000128";

    public int func_145760_g()
    {
        return this.field_145764_b;
    }

    public IChatComponent func_145749_h()
    {
        return this.field_145762_d;
    }

    public void func_145758_a(NBTTagCompound p_145758_1_)
    {
        p_145758_1_.setString("Command", this.field_145763_e);
        p_145758_1_.setInteger("SuccessCount", this.field_145764_b);
        p_145758_1_.setString("CustomName", this.field_145761_f);

        if (this.field_145762_d != null)
        {
            p_145758_1_.setString("LastOutput", IChatComponent.Serializer.func_150696_a(this.field_145762_d));
        }

        p_145758_1_.setBoolean("TrackOutput", this.field_145765_c);
    }

    public void func_145759_b(NBTTagCompound p_145759_1_)
    {
        this.field_145763_e = p_145759_1_.getString("Command");
        this.field_145764_b = p_145759_1_.getInteger("SuccessCount");

        if (p_145759_1_.hasKey("CustomName", 8))
        {
            this.field_145761_f = p_145759_1_.getString("CustomName");
        }

        if (p_145759_1_.hasKey("LastOutput", 8))
        {
            this.field_145762_d = IChatComponent.Serializer.func_150699_a(p_145759_1_.getString("LastOutput"));
        }

        if (p_145759_1_.hasKey("TrackOutput", 1))
        {
            this.field_145765_c = p_145759_1_.getBoolean("TrackOutput");
        }
    }

    public boolean canCommandSenderUseCommand(int p_70003_1_, String p_70003_2_)
    {
        return p_70003_1_ <= 2;
    }

    public void func_145752_a(String p_145752_1_)
    {
        this.field_145763_e = p_145752_1_;
    }

    public String func_145753_i()
    {
        return this.field_145763_e;
    }

    public void func_145755_a(World p_145755_1_)
    {
        if (p_145755_1_.isRemote)
        {
            this.field_145764_b = 0;
        }

        MinecraftServer minecraftserver = MinecraftServer.getServer();

        if (minecraftserver != null && minecraftserver.isCommandBlockEnabled())
        {
            // CraftBukkit start - Handle command block commands using Bukkit dispatcher
            org.bukkit.command.SimpleCommandMap commandMap = minecraftserver.server.getCommandMap();
            Joiner joiner = Joiner.on(" ");
            String command = this.field_145763_e;

            if (this.field_145763_e.startsWith("/"))
            {
                command = this.field_145763_e.substring(1);
            }

            String[] args = command.split(" ");
            ArrayList<String[]> commands = new ArrayList<String[]>();

            // Block disallowed commands
            if (args[0].equalsIgnoreCase("stop") || args[0].equalsIgnoreCase("kick") || args[0].equalsIgnoreCase("op") ||
                    args[0].equalsIgnoreCase("deop") || args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("ban-ip") ||
                    args[0].equalsIgnoreCase("pardon") || args[0].equalsIgnoreCase("pardon-ip") || args[0].equalsIgnoreCase("reload"))
            {
                this.field_145764_b = 0;
                return;
            }

            // If the world has no players don't run
            if (this.getEntityWorld().playerEntities.isEmpty())
            {
                this.field_145764_b = 0;
                return;
            }

            // Handle vanilla commands;
            if (minecraftserver.server.getCommandBlockOverride(args[0]))
            {
                org.bukkit.command.Command commandBlockCommand = commandMap.getCommand("minecraft:" + args[0]);

                if (commandBlockCommand instanceof VanillaCommandWrapper)
                {
                    this.field_145764_b = ((VanillaCommandWrapper) commandBlockCommand).dispatchVanillaCommandBlock(this, this.field_145763_e);
                    return;
                }
            }

            // Make sure this is a valid command
            if (commandMap.getCommand(args[0]) == null)
            {
                // Cauldron start - execute command using the vanilla command manager if it isn't in the bukkit command map
                net.minecraft.command.ICommandManager icommandmanager = minecraftserver.getCommandManager();
                icommandmanager.executeCommand(this, this.field_145763_e);
                return;
                // Cauldron end
            }

            // testfor command requires special handling
            if (args[0].equalsIgnoreCase("testfor"))
            {
                if (args.length < 2)
                {
                    this.field_145764_b = 0;
                    return;
                }

                EntityPlayerMP[] players = PlayerSelector.matchPlayers(this, args[1]);

                if (players != null && players.length > 0)
                {
                    this.field_145764_b = players.length;
                    return;
                }
                else
                {
                    EntityPlayerMP player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(args[1]);

                    if (player == null)
                    {
                        this.field_145764_b = 0;
                        return;
                    }
                    else
                    {
                        this.field_145764_b = 1;
                        return;
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
                    if (this instanceof TileEntityCommandBlockListener)
                    {
                        TileEntityCommandBlockListener listener = (TileEntityCommandBlockListener) this;
                        MinecraftServer.getLogger().log(Level.WARN, String.format("CommandBlock at (%d,%d,%d) failed to handle command", listener.getPlayerCoordinates().posX, listener.getPlayerCoordinates().posY, listener.getPlayerCoordinates().posZ), exception);
                    }
                    else if (this instanceof EntityMinecartCommandBlockListener)
                    {
                        EntityMinecartCommandBlockListener listener = (EntityMinecartCommandBlockListener) this;
                        MinecraftServer.getLogger().log(Level.WARN, String.format("MinecartCommandBlock at (%d,%d,%d) failed to handle command", listener.getPlayerCoordinates().posX, listener.getPlayerCoordinates().posY, listener.getPlayerCoordinates().posZ), exception);
                    }
                    else
                    {
                        MinecraftServer.getLogger().log(Level.WARN, String.format("Unknown CommandBlock failed to handle command"), exception);
                    }
                }
            }

            this.field_145764_b = completed;
            // CraftBukkit end
        }
        else
        {
            this.field_145764_b = 0;
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
                if (player.worldObj != this.getEntityWorld())
                {
                    continue;
                }

                String[] command = args.clone();
                command[pos] = player.getCommandSenderName();
                commands.add(command);
            }
        }

        return commands;
    }
    // CraftBukkit end

    public String getCommandSenderName()
    {
        return this.field_145761_f;
    }

    public IChatComponent func_145748_c_()
    {
        return new ChatComponentText(this.getCommandSenderName());
    }

    public void func_145754_b(String p_145754_1_)
    {
        this.field_145761_f = p_145754_1_;
    }

    public void addChatMessage(IChatComponent p_145747_1_)
    {
        if (this.field_145765_c && this.getEntityWorld() != null && !this.getEntityWorld().isRemote)
        {
            this.field_145762_d = (new ChatComponentText("[" + field_145766_a.format(new Date()) + "] ")).appendSibling(p_145747_1_);
            this.func_145756_e();
        }
    }

    public abstract void func_145756_e();

    @SideOnly(Side.CLIENT)
    public abstract int func_145751_f();

    @SideOnly(Side.CLIENT)
    public abstract void func_145757_a(ByteBuf p_145757_1_);

    public void func_145750_b(IChatComponent p_145750_1_)
    {
        this.field_145762_d = p_145750_1_;
    }
}