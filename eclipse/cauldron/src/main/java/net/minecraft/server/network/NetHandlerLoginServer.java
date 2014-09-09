package net.minecraft.server.network;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import io.netty.util.concurrent.GenericFutureListener;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.SecretKey;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.INetHandlerLoginServer;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.login.server.S01PacketEncryptionRequest;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.CryptManager;
import net.minecraft.util.IChatComponent;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// CraftBukkit start
import net.minecraft.entity.player.EntityPlayerMP;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
// CraftBukkit end
import com.mojang.authlib.properties.Property;

public class NetHandlerLoginServer implements INetHandlerLoginServer
{
    private static final AtomicInteger field_147331_b = new AtomicInteger(0);
    private static final Logger logger = LogManager.getLogger();
    private static final Random field_147329_d = new Random();
    private final byte[] field_147330_e = new byte[4];
    private final MinecraftServer field_147327_f;
    public final NetworkManager field_147333_a;
    private LoginState field_147328_g;
    private int field_147336_h;
    private GameProfile field_147337_i;
    private String field_147334_j;
    private SecretKey field_147335_k;
    public String hostname = ""; // CraftBukkit - add field
    private static final String __OBFID = "CL_00001458";

    public NetHandlerLoginServer(MinecraftServer p_i45298_1_, NetworkManager p_i45298_2_)
    {
        this.field_147328_g = LoginState.HELLO;
        this.field_147334_j = "";
        this.field_147327_f = p_i45298_1_;
        this.field_147333_a = p_i45298_2_;
        field_147329_d.nextBytes(this.field_147330_e);
    }

    public void onNetworkTick()
    {
        if (this.field_147328_g == LoginState.READY_TO_ACCEPT)
        {
            this.func_147326_c();
        }

        if (this.field_147336_h++ == FMLNetworkHandler.LOGIN_TIMEOUT)
        {
            this.func_147322_a("Took too long to log in");
        }
    }

    public void func_147322_a(String p_147322_1_)
    {
        try
        {
            logger.info("Disconnecting " + this.func_147317_d() + ": " + p_147322_1_);
            ChatComponentText chatcomponenttext = new ChatComponentText(p_147322_1_);
            this.field_147333_a.scheduleOutboundPacket(new S00PacketDisconnect(chatcomponenttext), new GenericFutureListener[0]);
            this.field_147333_a.closeChannel(chatcomponenttext);
        }
        catch (Exception exception)
        {
            logger.error("Error whilst disconnecting player", exception);
        }
    }

    // Spigot start
    public void initUUID()
    {
        UUID uuid;
        if ( field_147333_a.spoofedUUID != null )
        {
            uuid = field_147333_a.spoofedUUID;
        } else
        {
            uuid = UUID.nameUUIDFromBytes( ( "OfflinePlayer:" + this.field_147337_i.getName() ).getBytes( Charsets.UTF_8 ) );
        }

        this.field_147337_i = new GameProfile( uuid, this.field_147337_i.getName() );

        if (field_147333_a.spoofedProfile != null)
        {
            for ( Property property : field_147333_a.spoofedProfile )
            {
                this.field_147337_i.getProperties().put( property.getName(), property );
            }
        }
    }
    // Spigot end

    public void func_147326_c()
    {
        // Spigot start - Moved to initUUID
        /*
        if (!this.field_147337_i.isComplete())
        {
            this.field_147337_i = this.func_152506_a(this.field_147337_i);
        }
        */
        // Spigot end

        // CraftBukkit start - fire PlayerLoginEvent
        EntityPlayerMP s = this.field_147327_f.getConfigurationManager().attemptLogin(this, this.field_147337_i, this.hostname);

        if (s == null)
        {
            // this.func_147322_a(s);
            // CraftBukkit end
        }
        else
        {
            this.field_147328_g = NetHandlerLoginServer.LoginState.ACCEPTED;
            this.field_147333_a.scheduleOutboundPacket(new S02PacketLoginSuccess(this.field_147337_i), new GenericFutureListener[0]);
            FMLNetworkHandler.fmlServerHandshake(this.field_147327_f.getConfigurationManager(), this.field_147333_a, this.field_147327_f.getConfigurationManager().processLogin(this.field_147337_i, s)); // CraftBukkit - add player reference
        }
    }

    public void onDisconnect(IChatComponent p_147231_1_)
    {
        logger.info(this.func_147317_d() + " lost connection: " + p_147231_1_.getUnformattedText());
    }

    public String func_147317_d()
    {
        return this.field_147337_i != null ? this.field_147337_i.toString() + " (" + this.field_147333_a.getSocketAddress().toString() + ")" : String.valueOf(this.field_147333_a.getSocketAddress());
    }

    public void onConnectionStateTransition(EnumConnectionState p_147232_1_, EnumConnectionState p_147232_2_)
    {
        Validate.validState(this.field_147328_g == LoginState.ACCEPTED || this.field_147328_g == LoginState.HELLO, "Unexpected change in protocol", new Object[0]);
        Validate.validState(p_147232_2_ == EnumConnectionState.PLAY || p_147232_2_ == EnumConnectionState.LOGIN, "Unexpected protocol " + p_147232_2_, new Object[0]);
    }

    public void processLoginStart(C00PacketLoginStart p_147316_1_)
    {
        Validate.validState(this.field_147328_g == LoginState.HELLO, "Unexpected hello packet", new Object[0]);
        this.field_147337_i = p_147316_1_.func_149304_c();

        if (this.field_147327_f.isServerInOnlineMode() && !this.field_147333_a.isLocalChannel())
        {
            this.field_147328_g = LoginState.KEY;
            this.field_147333_a.scheduleOutboundPacket(new S01PacketEncryptionRequest(this.field_147334_j, this.field_147327_f.getKeyPair().getPublic(), this.field_147330_e), new GenericFutureListener[0]);
        }
        else
        {
            (new ThreadPlayerLookupUUID(this, "User Authenticator #" + field_147331_b.incrementAndGet())).start(); // Spigot
        }
    }

    public void processEncryptionResponse(C01PacketEncryptionResponse p_147315_1_)
    {
        Validate.validState(this.field_147328_g == LoginState.KEY, "Unexpected key packet", new Object[0]);
        PrivateKey privatekey = this.field_147327_f.getKeyPair().getPrivate();

        if (!Arrays.equals(this.field_147330_e, p_147315_1_.func_149299_b(privatekey)))
        {
            throw new IllegalStateException("Invalid nonce!");
        }
        else
        {
            this.field_147335_k = p_147315_1_.func_149300_a(privatekey);
            this.field_147328_g = NetHandlerLoginServer.LoginState.AUTHENTICATING;
            this.field_147333_a.enableEncryption(this.field_147335_k);
            (new ThreadPlayerLookupUUID(this, "User Authenticator #" + field_147331_b.incrementAndGet())).start();
        }
    }

    protected GameProfile func_152506_a(GameProfile p_152506_1_)
    {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + p_152506_1_.getName()).getBytes(Charsets.UTF_8));
        return new GameProfile(uuid, p_152506_1_.getName());
    }

    // Cauldron start - access methods for ThreadPlayerLookupUUID
    static String getLoginServerId(NetHandlerLoginServer loginServer) {
        return loginServer.field_147334_j;
    }

    static MinecraftServer getMinecraftServer(NetHandlerLoginServer loginServer) {
        return loginServer.field_147327_f;
    }

    static SecretKey getSecretKey(NetHandlerLoginServer loginServer) {
        return loginServer.field_147335_k;
    }

    static GameProfile processPlayerLoginGameProfile(NetHandlerLoginServer loginServer, GameProfile gameprofile) {
        return loginServer.field_147337_i = gameprofile;
    }

    static GameProfile getGameProfile(NetHandlerLoginServer loginServer) {
        return loginServer.field_147337_i;
    }

    static Logger getLogger() {
        return logger;
    }

    static void setLoginState(NetHandlerLoginServer loginServer, LoginState state)
    {
        loginServer.field_147328_g = state;
    }
    // Cauldron end

    static enum LoginState
    {
        HELLO,
        KEY,
        AUTHENTICATING,
        READY_TO_ACCEPT,
        ACCEPTED;

        private static final String __OBFID = "CL_00001463";
    }
}