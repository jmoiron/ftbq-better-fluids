package net.jmoiron.ftbqbetterfluids.client.recipe;

import net.minecraftforge.fml.ModList;

public class ClientRecipeViewers {
    public static boolean isRecipeViewerLoaded() {
        return ModList.get().isLoaded("jei") || ModList.get().isLoaded("roughlyenoughitems");
    }

    public static void showRecipes(net.minecraftforge.fluids.FluidStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        if (ModList.get().isLoaded("jei") && JeiFluidRecipes.showRecipes(stack)) {
            return;
        }

        if (ModList.get().isLoaded("roughlyenoughitems")) {
            ReiFluidRecipes.showRecipes(stack);
        }
    }
}
