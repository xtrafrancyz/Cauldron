package net.minecraft.server.dedicated;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import joptsimple.OptionSet; // CraftBukkit

@SideOnly(Side.SERVER)
public class PropertyManager
{
    private static final Logger field_164440_a = LogManager.getLogger();
    public final Properties serverProperties = new Properties(); // CraftBukkit - private -> public
    private final File serverPropertiesFile;
    private static final String __OBFID = "CL_00001782";

    public PropertyManager(File p_i45278_1_)
    {
        this.serverPropertiesFile = p_i45278_1_;

        if (p_i45278_1_.exists())
        {
            FileInputStream fileinputstream = null;

            try
            {
                fileinputstream = new FileInputStream(p_i45278_1_);
                this.serverProperties.load(fileinputstream);
            }
            catch (Exception exception)
            {
                field_164440_a.warn("Failed to load " + p_i45278_1_, exception);
                this.generateNewProperties();
            }
            finally
            {
                if (fileinputstream != null)
                {
                    try
                    {
                        fileinputstream.close();
                    }
                    catch (IOException ioexception)
                    {
                        ;
                    }
                }
            }
        }
        else
        {
            field_164440_a.warn(p_i45278_1_ + " does not exist");
            this.generateNewProperties();
        }
    }

    // CraftBukkit start
    private OptionSet options = null;

    public PropertyManager(final OptionSet options)
    {
        this((File) options.valueOf("config"));
        this.options = options;
    }

    private <T> T getOverride(String name, T value)
    {
        if ((this.options != null) && (this.options.has(name)))
        {
            return (T) this.options.valueOf(name);
        }

        return value;
    }
    // CraftBukkit end

    public void generateNewProperties()
    {
        field_164440_a.info("Generating new properties file");
        this.saveProperties();
    }

    public void saveProperties()
    {
        FileOutputStream fileoutputstream = null;

        try
        {
            // CraftBukkit start - Don't attempt writing to file if it's read only
            if (this.serverPropertiesFile.exists() && !this.serverPropertiesFile.canWrite())
            {
                return;
            }

            // CraftBukkit end
            fileoutputstream = new FileOutputStream(this.serverPropertiesFile);
            this.serverProperties.store(fileoutputstream, "Minecraft server properties");
        }
        catch (Exception exception)
        {
            field_164440_a.warn("Failed to save " + this.serverPropertiesFile, exception);
            this.generateNewProperties();
        }
        finally
        {
            if (fileoutputstream != null)
            {
                try
                {
                    fileoutputstream.close();
                }
                catch (IOException ioexception)
                {
                    ;
                }
            }
        }
    }

    public File getPropertiesFile()
    {
        return this.serverPropertiesFile;
    }

    public String getStringProperty(String p_73671_1_, String p_73671_2_)
    {
        if (!this.serverProperties.containsKey(p_73671_1_))
        {
            this.serverProperties.setProperty(p_73671_1_, p_73671_2_);
            this.saveProperties();
            this.saveProperties();
        }

        return this.getOverride(p_73671_1_, this.serverProperties.getProperty(p_73671_1_, p_73671_2_)); // CraftBukkit
    }

    public int getIntProperty(String p_73669_1_, int p_73669_2_)
    {
        try
        {
            return this.getOverride(p_73669_1_, Integer.parseInt(this.getStringProperty(p_73669_1_, "" + p_73669_2_))); // CraftBukkit
        }
        catch (Exception exception)
        {
            this.serverProperties.setProperty(p_73669_1_, "" + p_73669_2_);
            this.saveProperties();
            return this.getOverride(p_73669_1_, p_73669_2_); // CraftBukkit
        }
    }

    public boolean getBooleanProperty(String p_73670_1_, boolean p_73670_2_)
    {
        try
        {
            return this.getOverride(p_73670_1_, Boolean.parseBoolean(this.getStringProperty(p_73670_1_, "" + p_73670_2_))); // CraftBukkit
        }
        catch (Exception exception)
        {
            this.serverProperties.setProperty(p_73670_1_, "" + p_73670_2_);
            this.saveProperties();
            return this.getOverride(p_73670_1_, p_73670_2_); // CraftBukkit
        }
    }

    public void setProperty(String p_73667_1_, Object p_73667_2_)
    {
        this.serverProperties.setProperty(p_73667_1_, "" + p_73667_2_);
    }
}