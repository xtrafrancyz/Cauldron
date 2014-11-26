package net.minecraft.server;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.MinecraftException;

@SideOnly(Side.SERVER)
public final class ThreadDedicatedServer extends Thread
{
    final DedicatedServer field_96244_a;

    public ThreadDedicatedServer(DedicatedServer par1DedicatedServer)
    {
        this.field_96244_a = par1DedicatedServer;
    }

    public void run()
    {
        try
        {
            this.field_96244_a.stopServer();
        }
        catch (MinecraftException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}