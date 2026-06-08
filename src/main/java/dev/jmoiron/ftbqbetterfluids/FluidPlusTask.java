package dev.jmoiron.ftbqbetterfluids;

import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.registries.RegistrarManager;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.FluidConfig;
import dev.ftb.mods.ftblibrary.config.NBTConfig;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftblibrary.util.client.ClientUtils;
import dev.ftb.mods.ftblibrary.util.client.PositionedIngredient;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.jmoiron.ftbqbetterfluids.client.recipe.ClientRecipeViewers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public class FluidPlusTask extends Task {
    public static TaskType TYPE;

    private Fluid fluid = Fluids.WATER;
    @Nullable
    private CompoundTag fluidNBT;
    @Nullable
    private FluidStack cachedFluidStack;

    public FluidPlusTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return TYPE;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public FluidPlusTask setFluid(Fluid fluid) {
        this.fluid = normalizeFluid(fluid);
        clearCachedData();
        return this;
    }

    @Nullable
    public CompoundTag getFluidNBT() {
        return fluidNBT;
    }

    @Override
    public boolean consumesResources() {
        return false;
    }

    @Override
    public boolean submitItemsOnInventoryChange() {
        return true;
    }

    @Override
    public boolean checkOnLogin() {
        return true;
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);
        nbt.putString("fluid", RegistrarManager.getId(fluid, Registries.FLUID).toString());
        if (fluidNBT != null) {
            nbt.put("nbt", fluidNBT);
        }
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        fluid = normalizeFluid(BuiltInRegistries.FLUID.get(new ResourceLocation(nbt.getString("fluid"))));
        fluidNBT = nbt.get("nbt") instanceof CompoundTag tag ? tag : null;
        clearCachedData();
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(RegistrarManager.getId(fluid, Registries.FLUID));
        buffer.writeNbt(fluidNBT);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        fluid = normalizeFluid(BuiltInRegistries.FLUID.get(buffer.readResourceLocation()));
        fluidNBT = buffer.readNbt();
        clearCachedData();
    }

    @Override
    public void clearCachedData() {
        super.clearCachedData();
        cachedFluidStack = null;
    }

    public FluidStack createFluidStack() {
        if (cachedFluidStack == null) {
            cachedFluidStack = architecturyStack(fluid, fluidNBT);
        }
        return cachedFluidStack;
    }

    public static FluidStack architecturyStack(Fluid fluid, @Nullable CompoundTag nbt) {
        return FluidStack.create(normalizeFluid(fluid), FluidStack.bucketAmount(), nbt);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public MutableComponent getAltTitle() {
        return Component.literal("").append(createFluidStack().getName());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Icon getAltIcon() {
        FluidStack stack = createFluidStack();
        return Icon.getIcon(ClientUtils.getStillTexture(stack)).withTint(Color4I.rgb(ClientUtils.getFluidColor(stack)));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        super.fillConfigGroup(config);
        config.add("fluid", new FluidConfig(false), createFluidStack(), v -> {
            fluid = normalizeFluid(v.getFluid());
            clearCachedData();
        }, architecturyStack(Fluids.WATER, null));
        config.add("fluid_nbt", new NBTConfig(), fluidNBT, v -> {
            fluidNBT = v;
            clearCachedData();
        }, null);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Optional<PositionedIngredient> getIngredient(Widget widget) {
        return PositionedIngredient.of(createFluidStack(), widget);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onButtonClicked(Button button, boolean canClick) {
        button.playClickSound();
        ClientRecipeViewers.showRecipes(createForgeFluidStack());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        if (ClientRecipeViewers.isRecipeViewerLoaded()) {
            list.blankLine();
            list.add(Component.translatable("ftbq_better_fluids.task.click_recipe").withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE));
        }
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
        if (!checkTaskSequence(teamData) || teamData.isCompleted(this)) {
            return;
        }

        for (ItemStack stack : player.getInventory().items) {
            if (stackContainsMatchingFluid(stack)) {
                teamData.setProgress(this, getMaxProgress());
                return;
            }
        }
    }

    private boolean stackContainsMatchingFluid(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return FluidUtil.getFluidHandler(stack)
                .map(handler -> {
                    for (int tank = 0; tank < handler.getTanks(); tank++) {
                        net.minecraftforge.fluids.FluidStack contained = handler.getFluidInTank(tank);
                        if (matches(contained)) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    private boolean matches(net.minecraftforge.fluids.FluidStack contained) {
        if (contained.isEmpty() || contained.getFluid() != fluid) {
            return false;
        }
        return fluidNBT == null || Objects.equals(fluidNBT, contained.getTag());
    }

    private net.minecraftforge.fluids.FluidStack createForgeFluidStack() {
        return new net.minecraftforge.fluids.FluidStack(fluid, 1000, fluidNBT);
    }

    private static Fluid normalizeFluid(@Nullable Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY ? Fluids.WATER : fluid;
    }
}
