package net.minecraft.server.dedicated;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.logging.ILogAgent;

import joptsimple.OptionSet; // CraftBukkit

public class PropertyManager
{
    public final Properties properties = new Properties(); // CraftBukkit - private -> public

    /** Reference to the logger. */
    private final ILogAgent logger;
    private final File associatedFile;

    public PropertyManager(File par1File, ILogAgent par2ILogAgent)
    {
        this.associatedFile = par1File;
        this.logger = par2ILogAgent;

        if (par1File.exists())
        {
            FileInputStream fileinputstream = null;

            try
            {
                fileinputstream = new FileInputStream(par1File);
                this.properties.load(fileinputstream);
            }
            catch (Exception exception)
            {
                par2ILogAgent.logWarningException("Failed to load " + par1File, exception);
                this.logMessageAndSave();
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
            par2ILogAgent.logWarning(par1File + " does not exist");
            this.logMessageAndSave();
        }
    }

    // CraftBukkit start
    private OptionSet options = null;

    public PropertyManager(final OptionSet options, ILogAgent ilogagent)
    {
        this((File) options.valueOf("config"), ilogagent);
        this.options = options;
    }

    private <T> T getOverride(String name, T value)
    {
        if ((this.options != null) && (this.options.has(name)) && !name.equals("online-mode"))    // Spigot
        {
            return (T) this.options.valueOf(name);
        }

        return value;
    }
    // CraftBukkit end

    /**
     * logs an info message then calls saveSettingsToFile Yes this appears to be a potential stack overflow - these 2
     * functions call each other repeatdly if an exception occurs.
     */
    public void logMessageAndSave()
    {
        this.logger.logInfo("Generating new properties file");
        this.saveProperties();
    }

    /**
     * Writes the properties to the properties file.
     */
    public void saveProperties()
    {
        FileOutputStream fileoutputstream = null;

        try
        {
            // CraftBukkit start - Don't attempt writing to file if it's read only
            if (this.associatedFile.exists() && !this.associatedFile.canWrite())
            {
                return;
            }

            // CraftBukkit end
            fileoutputstream = new FileOutputStream(this.associatedFile);
            this.properties.store(fileoutputstream, "Minecraft server properties");
        }
        catch (Exception exception)
        {
            this.logger.logWarningException("Failed to save " + this.associatedFile, exception);
            this.logMessageAndSave();
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

    /**
     * Returns this PropertyManager's file object used for property saving.
     */
    public File getPropertiesFile()
    {
        return this.associatedFile;
    }

    /**
     * Gets a property. If it does not exist, set it to the specified value.
     */
    public String getProperty(String par1Str, String par2Str)
    {
        if (!this.properties.containsKey(par1Str))
        {
            this.properties.setProperty(par1Str, par2Str);
            this.saveProperties();
        }

        return this.getOverride(par1Str, this.properties.getProperty(par1Str, par2Str)); // CraftBukkit
    }

    /**
     * Gets an integer property. If it does not exist, set it to the specified value.
     */
    public int getIntProperty(String par1Str, int par2)
    {
        try
        {
            return this.getOverride(par1Str, Integer.parseInt(this.getProperty(par1Str, "" + par2))); // CraftBukkit
        }
        catch (Exception exception)
        {
            this.properties.setProperty(par1Str, "" + par2);
            return this.getOverride(par1Str, par2); // CraftBukkit
        }
    }

    /**
     * Gets a boolean property. If it does not exist, set it to the specified value.
     */
    public boolean getBooleanProperty(String par1Str, boolean par2)
    {
        try
        {
            return this.getOverride(par1Str, Boolean.parseBoolean(this.getProperty(par1Str, "" + par2))); // CraftBukkit
        }
        catch (Exception exception)
        {
            this.properties.setProperty(par1Str, "" + par2);
            return this.getOverride(par1Str, par2); // CraftBukkit
        }
    }

    /**
     * Saves an Object with the given property name.
     */
    public void setProperty(String par1Str, Object par2Obj)
    {
        this.properties.setProperty(par1Str, "" + par2Obj);
    }
}