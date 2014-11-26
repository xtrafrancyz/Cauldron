/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 cpw.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     cpw - implementation
 */

package cpw.mods.fml.relauncher;

/**
 * Copied from ConsoleLogFormatter for shared use on the client
 *
 */
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public final class FMLLogFormatter extends Formatter // Cauldron - public for MinecraftServer
{
    static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Cauldron - static for setFormat (TODO)
    
    // Cauldron start
    public static void setFormat(boolean nojline, SimpleDateFormat date_format)
    {
        if (date_format != null)
            dateFormat = date_format;
        else if (nojline)
            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
    // Cauldron end

    public String format(LogRecord record)
    {
        StringBuilder msg = new StringBuilder();
        msg.append(this.dateFormat.format(Long.valueOf(record.getMillis())));
        Level lvl = record.getLevel();

        String name = lvl.getLocalizedName();
        if ( name == null )
        {
            name = lvl.getName();
        }

        if ( ( name != null ) && ( name.length() > 0 ) )
        {
            msg.append(" [" + name + "] ");
        }
        else
        {
            msg.append(" ");
        }

        if (record.getLoggerName() != null)
        {
            msg.append("["+record.getLoggerName()+"] ");
        }
        else
        {
            msg.append("[] ");
        }
        msg.append(formatMessage(record));
        msg.append(LINE_SEPARATOR);
        Throwable thr = record.getThrown();

        if (thr != null)
        {
            StringWriter thrDump = new StringWriter();
            thr.printStackTrace(new PrintWriter(thrDump));
            msg.append(thrDump.toString());
        }

        return msg.toString();
    }
}