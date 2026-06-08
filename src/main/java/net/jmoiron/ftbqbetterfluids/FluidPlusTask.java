package net.jmoiron.ftbqbetterfluids;

import dev.architectury.fluid.FluidStack;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.FluidConfig;
import dev.ftb.mods.ftblibrary.config.NBTConfig;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftblibrary.util.client.ClientUtils;
import dev.ftb.mods.ftblibrary.util.client.PositionedIngredient;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.jmoiron.ftbqbetterfluids.client.recipe.ClientRecipeViewers;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

public class FluidPlusTask extends FluidTask {
    public static TaskType TYPE;
    private static final MethodHandle TASK_FILL_CONFIG_GROUP = findTaskFillConfigGroup();
    @Nullable
    private static final Field DETECTOR_CONSUME_FLUID_FIELD = findDetectorConsumeFluidField();

    public FluidPlusTask(long id, Quest quest) {
        super(id, quest);
        forceDetectorConsumeFluidFalse();
    }

    @Override
    public TaskType getType() {
        return TYPE;
    }

    @Override
    public FluidPlusTask setFluid(Fluid fluid) {
        super.setFluid(normalizeFluid(fluid));
        clearCachedData();
        return this;
    }

    @Override
    public long getMaxProgress() {
        return 1L;
    }

    @Override
    public boolean consumesResources() {
        return false;
    }

    @Override
    public boolean canInsertItem() {
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
        forceDetectorConsumeFluidFalse();
        super.writeData(nbt);
        nbt.remove("amount");
        nbt.putString("consume_fluid", Tristate.FALSE.name());
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        forceDetectorConsumeFluidFalse();
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        forceDetectorConsumeFluidFalse();
        super.writeNetData(buffer);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        forceDetectorConsumeFluidFalse();
    }

    public FluidStack createFluidStack() {
        return architecturyStack(getFluid(), getFluidNBT());
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
        fillBaseTaskConfigGroup(config);
        config.add("fluid", new FluidConfig(false).showAmount(false), createFluidStack(), v -> {
            setFluid(v.getFluid());
            clearCachedData();
        }, architecturyStack(Fluids.WATER, null));
        config.add("fluid_nbt", new NBTConfig(), getFluidNBT(), v -> {
            setFluidNBT(v);
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
        if (contained.isEmpty() || contained.getFluid() != getFluid()) {
            return false;
        }
        return getFluidNBT() == null || Objects.equals(getFluidNBT(), contained.getTag());
    }

    private net.minecraftforge.fluids.FluidStack createForgeFluidStack() {
        return new net.minecraftforge.fluids.FluidStack(getFluid(), 1000, getFluidNBT());
    }

    private void setFluidNBT(@Nullable CompoundTag fluidNBT) {
        CompoundTag nbt = new CompoundTag();
        writeData(nbt);
        if (fluidNBT == null) {
            nbt.remove("nbt");
        } else {
            nbt.put("nbt", fluidNBT);
        }
        readData(nbt);
    }

    private static Fluid normalizeFluid(@Nullable Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY ? Fluids.WATER : fluid;
    }

    private void fillBaseTaskConfigGroup(ConfigGroup config) {
        try {
            TASK_FILL_CONFIG_GROUP.invoke(this, config);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to open Fluid+ task config", e);
        }
    }

    private static MethodHandle findTaskFillConfigGroup() {
        try {
            return MethodHandles.lookup().findSpecial(Task.class, "fillConfigGroup", MethodType.methodType(void.class, ConfigGroup.class), FluidPlusTask.class);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private void forceDetectorConsumeFluidFalse() {
        if (DETECTOR_CONSUME_FLUID_FIELD == null) {
            return;
        }

        try {
            DETECTOR_CONSUME_FLUID_FIELD.set(this, Tristate.FALSE);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set Fluid+ detector consume mode", e);
        }
    }

    @Nullable
    private static Field findDetectorConsumeFluidField() {
        try {
            Field field = FluidTask.class.getDeclaredField("consumeFluid");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
