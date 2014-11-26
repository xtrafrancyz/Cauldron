package net.minecraft.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLEncoder;
import net.minecraft.util.CryptManager;

// CraftBukkit start
import net.minecraft.network.packet.Packet255KickDisconnect;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
// CraftBukkit end

class ThreadLoginVerifier extends Thread
{
    /** The login handler that spawned this thread. */
    final NetLoginHandler loginHandler;

    // CraftBukkit start
    CraftServer server;

    // Cauldron start - vanilla compatibility
    ThreadLoginVerifier(NetLoginHandler pendingconnection)
    {
        this(pendingconnection, (CraftServer) org.bukkit.Bukkit.getServer());
    }
    // Cauldron end

    ThreadLoginVerifier(NetLoginHandler pendingconnection, CraftServer server)
    {
        super("Login Verifier - " + pendingconnection.getUsernameAndAddress());
        this.server = server;
        // CraftBukkit end
        this.loginHandler = pendingconnection;
    }

    private boolean auth() throws java.io.IOException
    {
        String s = (new BigInteger(CryptManager.getServerIdHash(NetLoginHandler.getServerId(this.loginHandler), NetLoginHandler.getLoginMinecraftServer(this.loginHandler).getKeyPair().getPublic(), NetLoginHandler.getSharedKey(this.loginHandler)))).toString(16);
        URL url = new URL("http://mc.vimeworld.ru/checkserver.php?user=" + URLEncoder.encode(NetLoginHandler.getClientUsername(this.loginHandler), "UTF-8") + "&server=" + URLEncoder.encode(s, "UTF-8"));
        BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(url.openConnection(NetLoginHandler.getLoginMinecraftServer(this.loginHandler).getServerProxy()).getInputStream()));
        String s1 = bufferedreader.readLine();
        bufferedreader.close();

        if (!"YES".equals(s1))
        {
            this.loginHandler.raiseErrorAndDisconnect("Failed to verify username! "+s1);
            return false;
        }

        // CraftBukkit start
        if (this.loginHandler.getSocket() == null)
        {
            return false;
        }
        return true;
    }

    public void run() {
        try {
            if (org.spigotmc.SpamHaus.filterIp(loginHandler)) return; // Spigot
            if (server.getOnlineMode() && !auth()) return; // Spigot

            AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(NetLoginHandler.getClientUsername(this.loginHandler), this.loginHandler.getSocket().getInetAddress());
            this.server.getPluginManager().callEvent(asyncEvent);

            if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0)
            {
                final PlayerPreLoginEvent event = new PlayerPreLoginEvent(NetLoginHandler.getClientUsername(this.loginHandler), this.loginHandler.getSocket().getInetAddress());

                if (asyncEvent.getResult() != PlayerPreLoginEvent.Result.ALLOWED)
                {
                    event.disallow(asyncEvent.getResult(), asyncEvent.getKickMessage());
                }

                Waitable<PlayerPreLoginEvent.Result> waitable = new Waitable<PlayerPreLoginEvent.Result>()
                {
                    @Override
                    protected PlayerPreLoginEvent.Result evaluate()
                    {
                        ThreadLoginVerifier.this.server.getPluginManager().callEvent(event);
                        return event.getResult();
                    }
                };
                NetLoginHandler.getLoginMinecraftServer(this.loginHandler).processQueue.add(waitable);

                if (waitable.get() != PlayerPreLoginEvent.Result.ALLOWED)
                {
                    this.loginHandler.raiseErrorAndDisconnect(event.getKickMessage());
                    return;
                }
            }
            else
            {
                if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
                {
                    this.loginHandler.raiseErrorAndDisconnect(asyncEvent.getKickMessage());
                    return;
                }
            }

            // CraftBukkit end
            NetLoginHandler.func_72531_a(this.loginHandler, true);
            // CraftBukkit start
        }
        catch (java.io.IOException exception)
        {
            this.loginHandler.raiseErrorAndDisconnect("Failed to verify username, session authentication server unavailable!");
        }
        catch (Exception exception)
        {
            this.loginHandler.raiseErrorAndDisconnect("Failed to verify username!");
            server.getLogger().log(java.util.logging.Level.WARNING, "Exception verifying " + NetLoginHandler.getClientUsername(this.loginHandler), exception);
            // CraftBukkit end
        }
    }
}