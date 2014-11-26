package net.minecraft.server.dedicated;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static net.minecraft.server.MinecraftServer.*; // Cauldron

class DedicatedServerCommandThread extends Thread
{
    final DedicatedServer server;

    DedicatedServerCommandThread(DedicatedServer par1DedicatedServer)
    {
        super("Command Reader");
        this.server = par1DedicatedServer;
    }

    public void run()
    {
        // CraftBukkit start
        if (!useConsole)
        {
            return;
        }

        // CraftBukkit end
        jline.console.ConsoleReader bufferedreader = this.server.reader; // CraftBukkit
        String s;

        try
        {
            // CraftBukkit start - JLine disabling compatibility
            while (!this.server.isServerStopped() && this.server.isServerRunning())
            {
                if (useJline)
                {
                    s = bufferedreader.readLine(">", null);
                }
                else
                {
                    s = bufferedreader.readLine();
                }

                if (s != null)
                {
                    this.server.addPendingCommand(s, this.server);
                }

                // CraftBukkit end
            }
        }
        catch (IOException ioexception)
        {
            // CraftBukkit
            java.util.logging.Logger.getLogger("").log(java.util.logging.Level.SEVERE, null, ioexception);
        }
    }
}