package net.minecraft.profiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// CraftBukkit start - Strip down to empty methods, performance cost
public class Profiler
{
    /** Flag profiling enabled */
    public boolean profilingEnabled = false;

    /**
     * Clear profiling.
     */
    public final void clearProfiling() { }

    /**
     * Start section
     */
    public final void startSection(String par1Str) { }

    /**
     * End section
     */
    public final void endSection() { }

    /**
     * Get profiling data
     */
    public final List getProfilingData(String par1Str)
    {
        return null;
    }

    /**
     * End current section and start a new section
     */
    public final void endStartSection(String par1Str) { }
    public final String getNameOfLastSection()
    {
        return null;
    }
}
// CraftBukkit end