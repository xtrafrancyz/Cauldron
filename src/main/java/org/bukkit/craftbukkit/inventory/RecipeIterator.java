package org.bukkit.craftbukkit.inventory;

import java.util.Iterator;

import net.minecraftforge.cauldron.potion.CustomModRecipe;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;



public class RecipeIterator implements Iterator<Recipe> {
    private final Iterator<net.minecraft.item.crafting.IRecipe> recipes;
    private final Iterator<Integer> smelting;
    private Iterator<?> removeFrom = null;

    public RecipeIterator() {
        this.recipes = net.minecraft.item.crafting.CraftingManager.getInstance().getRecipeList().iterator();
        this.smelting = net.minecraft.item.crafting.FurnaceRecipes.smelting().getSmeltingList().keySet().iterator();
    }

    public boolean hasNext() {
        if (recipes.hasNext()) {
            return true;
        } else {
            return smelting.hasNext();
        }
    }

    public Recipe next() {
        if (recipes.hasNext()) {
            removeFrom = recipes;
            // Cauldron start - handle custom recipe classes without Bukkit API equivalents
            net.minecraft.item.crafting.IRecipe iRecipe = recipes.next();
            try {
                return iRecipe.toBukkitRecipe();
            } catch (AbstractMethodError ex) {
                // No Bukkit wrapper provided
                return new CustomModRecipe(iRecipe);
            }
            // Cauldron end
        } else {
            removeFrom = smelting;
            int id = smelting.next();
            CraftItemStack stack = CraftItemStack.asCraftMirror(net.minecraft.item.crafting.FurnaceRecipes.smelting().getSmeltingResult(id)); // Cauldron - TODO: use metadata-aware smelting result
            return new CraftFurnaceRecipe(stack, new ItemStack(id, 1, (short) -1));
        }
    }

    public void remove() {
        if (removeFrom == null) {
            throw new IllegalStateException();
        }
        removeFrom.remove();
    }
}
