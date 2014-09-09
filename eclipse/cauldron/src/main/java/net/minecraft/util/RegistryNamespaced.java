package net.minecraft.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.Iterator;
import java.util.Map;

// Cauldron start
import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.Block;
// Cauldron end

public class RegistryNamespaced extends RegistrySimple implements IObjectIntIterable
{
    protected ObjectIntIdentityMap underlyingIntegerMap = new ObjectIntIdentityMap();
    protected final Map field_148758_b;
    private static final String __OBFID = "CL_00001206";

    public RegistryNamespaced()
    {
        this.field_148758_b = ((BiMap)this.registryObjects).inverse();
    }

    public void addObject(int p_148756_1_, String p_148756_2_, Object p_148756_3_)
    {
        // Cauldron start - register item/block materials for Bukkit
        boolean isForgeBlock = p_148756_3_ instanceof Block && (p_148756_3_.getClass().getName().length() > 3 && !p_148756_3_.getClass().getName().startsWith("net.minecraft.block")) ? true : false;
        org.bukkit.Material material = org.bukkit.Material.addMaterial(p_148756_1_, p_148756_2_, isForgeBlock);
        if (material != null)
        {
            if (isForgeBlock)
            {
                FMLLog.info("Injected new Forge block material %s with ID %d.", material.name(), material.getId());
            }
            else
            {
                FMLLog.info("Injected new Forge item material %s with ID %d.", material.name(), material.getId());
            }
        }
        // Cauldron end
        this.underlyingIntegerMap.func_148746_a(p_148756_3_, p_148756_1_);
        this.putObject(ensureNamespaced(p_148756_2_), p_148756_3_);
    }

    protected Map createUnderlyingMap()
    {
        return HashBiMap.create();
    }

    public Object getObject(String p_82594_1_)
    {
        return super.getObject(ensureNamespaced(p_82594_1_));
    }

    public String getNameForObject(Object p_148750_1_)
    {
        return (String)this.field_148758_b.get(p_148750_1_);
    }

    public boolean containsKey(String p_148741_1_)
    {
        return super.containsKey(ensureNamespaced(p_148741_1_));
    }

    public int getIDForObject(Object p_148757_1_)
    {
        return this.underlyingIntegerMap.func_148747_b(p_148757_1_);
    }

    public Object getObjectById(int p_148754_1_)
    {
        return this.underlyingIntegerMap.func_148745_a(p_148754_1_);
    }

    public Iterator iterator()
    {
        return this.underlyingIntegerMap.iterator();
    }

    public boolean containsId(int p_148753_1_)
    {
        return this.underlyingIntegerMap.func_148744_b(p_148753_1_);
    }

    protected static String ensureNamespaced(String p_148755_0_)
    {
        return p_148755_0_.indexOf(58) == -1 ? "minecraft:" + p_148755_0_ : p_148755_0_;
    }

    public boolean containsKey(Object p_148741_1_)
    {
        return this.containsKey((String)p_148741_1_);
    }

    public Object getObject(Object p_82594_1_)
    {
        return this.getObject((String)p_82594_1_);
    }
}