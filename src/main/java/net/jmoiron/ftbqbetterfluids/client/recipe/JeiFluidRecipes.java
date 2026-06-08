package net.jmoiron.ftbqbetterfluids.client.recipe;

import net.jmoiron.ftbqbetterfluids.FTBQBetterFluids;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class JeiFluidRecipes implements IModPlugin {
    private static final ResourceLocation UID = FTBQBetterFluids.id("jei");
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static boolean showRecipes(net.minecraftforge.fluids.FluidStack stack) {
        if (runtime == null) {
            return false;
        }

        runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, ForgeTypes.FLUID_STACK, stack));
        return true;
    }
}
