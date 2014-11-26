package net.minecraft.server;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.server.dedicated.DedicatedServer;

@SideOnly(Side.SERVER)
public final class ThreadDedicatedServer extends Thread
{
    final DedicatedServer connectedDedicatedServer;

    public ThreadDedicatedServer(DedicatedServer par1DedicatedServer)
    {
        this.connectedDedicatedServer = par1DedicatedServer;
    }

    public void run()
    {
        this.connectedDedicatedServer.stopServer();
    }
}
