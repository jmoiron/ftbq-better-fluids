package net.jmoiron.ftbqbetterfluids;

import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.client.ClientUtils;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.jmoiron.ftbqbetterfluids.client.FluidPlusClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(FTBQBetterFluids.MOD_ID)
public class FTBQBetterFluids {
    public static final String MOD_ID = "ftbq_better_fluids";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FTBQBetterFluids() {
        FluidPlusTask.TYPE = TaskTypes.register(id("fluid_plus"), FluidPlusTask::new, () -> Icon.getIcon(Optional.ofNullable(ClientUtils.getStillTexture(FluidPlusTask.architecturyStack(Fluids.WATER, null)))
                        .map(ResourceLocation::toString)
                        .orElse("missingno"))
                .withTint(Color4I.rgb(0x8080FF))
                .combineWith(Icon.getIcon(FluidTask.TANK_TEXTURE.toString())));

        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> FluidPlusClient::init);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
