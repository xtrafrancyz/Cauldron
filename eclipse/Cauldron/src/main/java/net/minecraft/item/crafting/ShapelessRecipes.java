package net.minecraft.item.crafting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftShapelessRecipe;
// CraftBukkit end

public class ShapelessRecipes implements IRecipe
{
    /** Is the ItemStack that you get when craft the recipe. */
    public final ItemStack recipeOutput; // Spigot

    /** Is a List of ItemStack that composes the recipe. */
    public final List recipeItems;

    public ShapelessRecipes(ItemStack par1ItemStack, List par2List)
    {
        this.recipeOutput = par1ItemStack;
        this.recipeItems = par2List;
    }

    // CraftBukkit start
    @SuppressWarnings("unchecked")
    public org.bukkit.inventory.ShapelessRecipe toBukkitRecipe()
    {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.recipeOutput);
        CraftShapelessRecipe recipe = new CraftShapelessRecipe(result, this);

        for (ItemStack stack : (List<ItemStack>) this.recipeItems)
        {
            if (stack != null)
            {
                recipe.addIngredient(org.bukkit.Material.getMaterial(stack.itemID), stack.getItemDamage());
            }
        }

        return recipe;
    }
    // CraftBukkit end

    public ItemStack getRecipeOutput()
    {
        return this.recipeOutput;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    public boolean matches(InventoryCrafting par1InventoryCrafting, World par2World)
    {
        ArrayList arraylist = new ArrayList(this.recipeItems);

        for (int i = 0; i < 3; ++i)
        {
            for (int j = 0; j < 3; ++j)
            {
                ItemStack itemstack = par1InventoryCrafting.getStackInRowAndColumn(j, i);

                if (itemstack != null)
                {
                    boolean flag = false;
                    Iterator iterator = arraylist.iterator();

                    while (iterator.hasNext())
                    {
                        ItemStack itemstack1 = (ItemStack)iterator.next();

                        if (itemstack.itemID == itemstack1.itemID && (itemstack1.getItemDamage() == 32767 || itemstack.getItemDamage() == itemstack1.getItemDamage()))
                        {
                            flag = true;
                            arraylist.remove(itemstack1);
                            break;
                        }
                    }

                    if (!flag)
                    {
                        return false;
                    }
                }
            }
        }

        return arraylist.isEmpty();
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(InventoryCrafting par1InventoryCrafting)
    {
        return this.recipeOutput.copy();
    }

    /**
     * Returns the size of the recipe area
     */
    public int getRecipeSize()
    {
        return this.recipeItems.size();
    }

    // Spigot start
    public java.util.List<ItemStack> getIngredients()
    {
        return java.util.Collections.unmodifiableList(recipeItems);
    }
    // Spigot end
}