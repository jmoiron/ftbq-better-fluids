package dev.jmoiron.ftbqbetterfluids.client.recipe;

import dev.architectury.fluid.FluidStack;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.util.EntryStacks;

public class ReiFluidRecipes {
    static void showRecipes(net.minecraftforge.fluids.FluidStack stack) {
        ViewSearchBuilder.builder()
                .addRecipesFor(EntryStacks.of(FluidStack.create(stack.getFluid(), stack.getAmount(), stack.getTag())))
                .open();
    }
}
