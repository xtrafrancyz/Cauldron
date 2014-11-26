package net.minecraft.item.crafting;

// CraftBukkit start
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftShapedRecipe;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
// CraftBukkit end

public class ShapedRecipes implements IRecipe
{
    /** How many horizontal slots this recipe is wide. */
    public int recipeWidth;

    /** How many vertical slots this recipe uses. */
    public int recipeHeight;

    /** Is a array of ItemStack that composes the recipe. */
    public ItemStack[] recipeItems;

    /** Is the ItemStack that you get when craft the recipe. */
    public ItemStack recipeOutput; // Spigot

    /** Is the itemID of the output item that you get when craft the recipe. */
    public final int recipeOutputItemID;
    private boolean field_92101_f;

    public ShapedRecipes(int par1, int par2, ItemStack[] par3ArrayOfItemStack, ItemStack par4ItemStack)
    {
        this.recipeOutputItemID = par4ItemStack.itemID;
        this.recipeWidth = par1;
        this.recipeHeight = par2;
        this.recipeItems = par3ArrayOfItemStack;
        this.recipeOutput = par4ItemStack;
    }

    // CraftBukkit start
    public org.bukkit.inventory.ShapedRecipe toBukkitRecipe()
    {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.recipeOutput);
        CraftShapedRecipe recipe = new CraftShapedRecipe(result, this);

        switch (this.recipeHeight)
        {
            case 1:
                switch (this.recipeWidth)
                {
                    case 1:
                        recipe.shape("a");
                        break;
                    case 2:
                        recipe.shape("ab");
                        break;
                    case 3:
                        recipe.shape("abc");
                        break;
                }

                break;
            case 2:
                switch (this.recipeWidth)
                {
                    case 1:
                        recipe.shape("a", "b");
                        break;
                    case 2:
                        recipe.shape("ab", "cd");
                        break;
                    case 3:
                        recipe.shape("abc", "def");
                        break;
                }

                break;
            case 3:
                switch (this.recipeWidth)
                {
                    case 1:
                        recipe.shape("a", "b", "c");
                        break;
                    case 2:
                        recipe.shape("ab", "cd", "ef");
                        break;
                    case 3:
                        recipe.shape("abc", "def", "ghi");
                        break;
                }

                break;
        }

        char c = 'a';

        for (ItemStack stack : this.recipeItems)
        {
            if (stack != null)
            {
                recipe.setIngredient(c, org.bukkit.Material.getMaterial(stack.itemID), stack.getItemDamage());
            }

            c++;
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
        for (int i = 0; i <= 3 - this.recipeWidth; ++i)
        {
            for (int j = 0; j <= 3 - this.recipeHeight; ++j)
            {
                if (this.checkMatch(par1InventoryCrafting, i, j, true))
                {
                    return true;
                }

                if (this.checkMatch(par1InventoryCrafting, i, j, false))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if the region of a crafting inventory is match for the recipe.
     */
    private boolean checkMatch(InventoryCrafting par1InventoryCrafting, int par2, int par3, boolean par4)
    {
        for (int k = 0; k < 3; ++k)
        {
            for (int l = 0; l < 3; ++l)
            {
                int i1 = k - par2;
                int j1 = l - par3;
                ItemStack itemstack = null;

                if (i1 >= 0 && j1 >= 0 && i1 < this.recipeWidth && j1 < this.recipeHeight)
                {
                    if (par4)
                    {
                        itemstack = this.recipeItems[this.recipeWidth - i1 - 1 + j1 * this.recipeWidth];
                    }
                    else
                    {
                        itemstack = this.recipeItems[i1 + j1 * this.recipeWidth];
                    }
                }

                ItemStack itemstack1 = par1InventoryCrafting.getStackInRowAndColumn(k, l);

                if (itemstack1 != null || itemstack != null)
                {
                    if (itemstack1 == null && itemstack != null || itemstack1 != null && itemstack == null)
                    {
                        return false;
                    }

                    if (itemstack.itemID != itemstack1.itemID)
                    {
                        return false;
                    }

                    if (itemstack.getItemDamage() != 32767 && itemstack.getItemDamage() != itemstack1.getItemDamage())
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    public ItemStack getCraftingResult(InventoryCrafting par1InventoryCrafting)
    {
        ItemStack itemstack = this.getRecipeOutput().copy();

        if (this.field_92101_f)
        {
            for (int i = 0; i < par1InventoryCrafting.getSizeInventory(); ++i)
            {
                ItemStack itemstack1 = par1InventoryCrafting.getStackInSlot(i);

                if (itemstack1 != null && itemstack1.hasTagCompound())
                {
                    itemstack.setTagCompound((NBTTagCompound)itemstack1.stackTagCompound.copy());
                }
            }
        }

        return itemstack;
    }

    /**
     * Returns the size of the recipe area
     */
    public int getRecipeSize()
    {
        return this.recipeWidth * this.recipeHeight;
    }

    public ShapedRecipes func_92100_c()
    {
        this.field_92101_f = true;
        return this;
    }

    // Spigot start
    public java.util.List<ItemStack> getIngredients()
    {
        return java.util.Arrays.asList(recipeItems);
    }
    // Spigot end
}