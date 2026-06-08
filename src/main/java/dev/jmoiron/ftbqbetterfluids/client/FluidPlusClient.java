package dev.jmoiron.ftbqbetterfluids.client;

import dev.ftb.mods.ftblibrary.config.FluidConfig;
import dev.ftb.mods.ftblibrary.config.ui.SelectFluidScreen;
import dev.jmoiron.ftbqbetterfluids.FluidPlusTask;

public class FluidPlusClient {
    public static void init() {
        FluidPlusTask.TYPE.setGuiProvider((panel, quest, callback) -> {
            FluidConfig config = new FluidConfig(false);
            new SelectFluidScreen(config, accepted -> {
                panel.run();
                if (accepted) {
                    callback.accept(new FluidPlusTask(0L, quest).setFluid(config.getValue().getFluid()));
                }
            }).openGui();
        });
    }
}
