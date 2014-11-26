package net.minecraft.command;

import java.util.Iterator;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.ServerCommandScoreboard;
import net.minecraft.scoreboard.ServerCommandTestFor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.ChatMessageComponent;
import net.minecraft.util.EnumChatFormatting;

public class ServerCommandManager extends CommandHandler implements IAdminCommand
{
    // Cauldron start - moved commands to it's own method to be executed further in server startup + changed to registerVanillaCommand
    public ServerCommandManager()
    {
        CommandBase.setAdminCommander(this);
    }

    public void registerVanillaCommands()
    {
        // Cauldron - do not register vanilla commands replaced by Bukkit
        /*    
        this.registerCommand(new CommandTime());
        this.registerCommand(new CommandGameMode());
        this.registerCommand(new CommandDifficulty());
        this.registerCommand(new CommandDefaultGameMode());
        this.registerCommand(new CommandKill());
        this.registerCommand(new CommandToggleDownfall());
        this.registerCommand(new CommandWeather());
        this.registerCommand(new CommandXP());
        this.registerCommand(new CommandServerTp());
        this.registerCommand(new CommandGive());
        this.registerCommand(new CommandEffect());
        this.registerCommand(new CommandEnchant());
        this.registerCommand(new CommandServerEmote());
        this.registerCommand(new CommandShowSeed());
        this.registerCommand(new CommandHelp());
        */
        this.registerCommand("vanilla.command", new CommandDebug()); // Cauldron - add permission node
        /*
        this.registerCommand(new CommandServerMessage());
        this.registerCommand(new CommandServerSay());
        this.registerCommand(new CommandSetSpawnpoint());
        this.registerCommand(new CommandGameRule());
        this.registerCommand(new CommandClearInventory());
        this.registerCommand(new ServerCommandTestFor());
        */
        this.registerCommand("vanilla.command", new CommandSpreadPlayers()); // Cauldron - add permission node
        this.registerCommand("vanilla.command", new CommandPlaySound()); // Cauldron - add permission node
        this.registerCommand("vanilla.command", new ServerCommandScoreboard()); // Cauldron - add permission node // TODO: remove once Bukkit implements

        if (MinecraftServer.getServer().isDedicatedServer())
        {
            /*
            this.registerCommand(new CommandServerOp());
            this.registerCommand(new CommandServerDeop());
            this.registerCommand(new CommandServerStop());
            this.registerCommand(new CommandServerSaveAll());
            this.registerCommand(new CommandServerSaveOff());
            this.registerCommand(new CommandServerSaveOn());
            this.registerCommand(new CommandServerBanIp());
            this.registerCommand(new CommandServerPardonIp());
            this.registerCommand(new CommandServerBan());
            this.registerCommand(new CommandServerBanlist());
            this.registerCommand(new CommandServerPardon());
            this.registerCommand(new CommandServerKick());
            this.registerCommand(new CommandServerList());
            this.registerCommand(new CommandServerWhitelist());
            this.registerCommand(new CommandSetPlayerTimeout());
            */
        }
        else
        {
            this.registerCommand("vanilla.command", new CommandServerPublishLocal()); // Cauldron - add permission node
        }
    }
    // Cauldron end

    /**
     * Sends a message to the admins of the server from a given CommandSender with the given resource string and given
     * extra srings. If the int par2 is even or zero, the original sender is also notified.
     */
    public void notifyAdmins(ICommandSender par1ICommandSender, int par2, String par3Str, Object ... par4ArrayOfObj)
    {
        boolean flag = true;

        if (par1ICommandSender instanceof TileEntityCommandBlock && !MinecraftServer.getServer().worldServers[0].getGameRules().getGameRuleBooleanValue("commandBlockOutput"))
        {
            flag = false;
        }

        ChatMessageComponent chatmessagecomponent = ChatMessageComponent.createFromTranslationWithSubstitutions("chat.type.admin", new Object[] {par1ICommandSender.getCommandSenderName(), ChatMessageComponent.createFromTranslationWithSubstitutions(par3Str, par4ArrayOfObj)});
        chatmessagecomponent.setColor(EnumChatFormatting.GRAY);
        chatmessagecomponent.setItalic(Boolean.valueOf(true));

        if (flag)
        {
            Iterator iterator = MinecraftServer.getServer().getConfigurationManager().playerEntityList.iterator();

            while (iterator.hasNext())
            {
                EntityPlayerMP entityplayermp = (EntityPlayerMP)iterator.next();

                if (entityplayermp != par1ICommandSender && MinecraftServer.getServer().getConfigurationManager().isPlayerOpped(entityplayermp.getCommandSenderName()))
                {
                    entityplayermp.sendChatToPlayer(chatmessagecomponent);
                }
            }
        }

        if (par1ICommandSender != MinecraftServer.getServer())
        {
            MinecraftServer.getServer().sendChatToPlayer(chatmessagecomponent);
        }

        if ((par2 & 1) != 1)
        {
            par1ICommandSender.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(par3Str, par4ArrayOfObj));
        }
    }
}