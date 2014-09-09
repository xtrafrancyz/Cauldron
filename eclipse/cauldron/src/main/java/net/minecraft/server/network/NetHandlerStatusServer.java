package net.minecraft.server.network;


import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.status.INetHandlerStatusServer;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;

// CraftBukkit start
import java.net.InetSocketAddress;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.util.ChatComponentText;
import org.bukkit.craftbukkit.util.CraftIconCache;
// CraftBukkit end

public class NetHandlerStatusServer implements INetHandlerStatusServer
{
    private final MinecraftServer field_147314_a;
    private final NetworkManager field_147313_b;
    private static final String __OBFID = "CL_00001464";

    public NetHandlerStatusServer(MinecraftServer p_i45299_1_, NetworkManager p_i45299_2_)
    {
        this.field_147314_a = p_i45299_1_;
        this.field_147313_b = p_i45299_2_;
    }

    public void onDisconnect(IChatComponent p_147231_1_) {}

    public void onConnectionStateTransition(EnumConnectionState p_147232_1_, EnumConnectionState p_147232_2_)
    {
        if (p_147232_2_ != EnumConnectionState.STATUS)
        {
            throw new UnsupportedOperationException("Unexpected change in protocol to " + p_147232_2_);
        }
    }

    public void onNetworkTick() {}

    public void processServerQuery(C00PacketServerQuery p_147312_1_)
    {
        // CraftBukkit start - fire ping event
        class ServerListPingEvent extends org.bukkit.event.server.ServerListPingEvent
        {
            CraftIconCache icon = field_147314_a.server.getServerIcon();

            ServerListPingEvent()
            {
                super(((InetSocketAddress) field_147313_b.getSocketAddress()).getAddress(), field_147314_a.getMOTD(), field_147314_a.getConfigurationManager().getCurrentPlayerCount(), field_147314_a.getConfigurationManager().getMaxPlayers());
            }

            @Override
            public void setServerIcon(org.bukkit.util.CachedServerIcon icon)
            {
                if (!(icon instanceof CraftIconCache))
                {
                    throw new IllegalArgumentException(icon + " was not created by " + org.bukkit.craftbukkit.CraftServer.class);
                }

                this.icon = (CraftIconCache) icon;
            }
        }
        ServerListPingEvent event = new ServerListPingEvent();
        this.field_147314_a.server.getPluginManager().callEvent(event);
        ServerStatusResponse ping = new ServerStatusResponse();
        ping.func_151320_a(event.icon.value);
        ping.func_151315_a(new ChatComponentText(event.getMotd()));
        ping.func_151319_a(new ServerStatusResponse.PlayerCountData(event.getMaxPlayers(), field_147314_a.getConfigurationManager().getCurrentPlayerCount()));
        ping.func_151321_a(new ServerStatusResponse.MinecraftProtocolVersionIdentifier(field_147314_a.getServerModName() + " " + field_147314_a.getMinecraftVersion(), 5)); // TODO: Update when protocol changes
        this.field_147313_b.scheduleOutboundPacket(new S00PacketServerInfo(ping), new GenericFutureListener[0]);
        // CraftBukkit end
    }

    public void processPing(C01PacketPing p_147311_1_)
    {
        this.field_147313_b.scheduleOutboundPacket(new S01PacketPong(p_147311_1_.func_149289_c()), new GenericFutureListener[0]);
    }
}