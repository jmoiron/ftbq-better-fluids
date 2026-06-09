package net.jmoiron.ftbqbetterfluids;

import dev.architectury.fluid.FluidStack;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.FluidConfig;
import dev.ftb.mods.ftblibrary.config.NBTConfig;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
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
import net.jmoiron.ftbqbetterfluids.client.FluidListScreen;
import net.jmoiron.ftbqbetterfluids.client.recipe.ClientRecipeViewers;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FluidPlusTask extends FluidTask {
    public static TaskType TYPE;
    private static final MethodHandle TASK_FILL_CONFIG_GROUP = findTaskFillConfigGroup();
    @Nullable
    private static final Field DETECTOR_CONSUME_FLUID_FIELD = findDetectorConsumeFluidField();
    private List<FluidStack> fluids = new ArrayList<>();

    public FluidPlusTask(long id, Quest quest) {
        super(id, quest);
        forceDetectorConsumeFluidFalse();
        setFluids(List.of(architecturyStack(Fluids.WATER, null)));
    }

    @Override
    public TaskType getType() {
        return TYPE;
    }

    @Override
    public FluidPlusTask setFluid(Fluid fluid) {
        setFluids(List.of(architecturyStack(fluid, null)));
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

        ListTag list = new ListTag();
        for (FluidStack stack : fluids) {
            list.add(stack.write(new CompoundTag()));
        }
        nbt.put("fluids", list);
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);
        if (nbt.contains("fluids", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("fluids", Tag.TAG_COMPOUND);
            List<FluidStack> readFluids = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                FluidStack stack = FluidStack.read(list.getCompound(i));
                if (!stack.isEmpty()) {
                    readFluids.add(normalizeStack(stack));
                }
            }
            setFluids(readFluids);
        } else {
            setFluids(List.of(architecturyStack(getFluid(), getFluidNBT())));
        }
        forceDetectorConsumeFluidFalse();
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        forceDetectorConsumeFluidFalse();
        super.writeNetData(buffer);
        buffer.writeVarInt(fluids.size());
        for (FluidStack stack : fluids) {
            stack.write(buffer);
        }
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        int size = buffer.readVarInt();
        List<FluidStack> readFluids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            FluidStack stack = FluidStack.read(buffer);
            if (!stack.isEmpty()) {
                readFluids.add(normalizeStack(stack));
            }
        }
        setFluids(readFluids);
        forceDetectorConsumeFluidFalse();
    }

    public FluidStack createFluidStack() {
        return getDisplayFluidStack();
    }

    public List<FluidStack> getFluids() {
        return List.copyOf(fluids);
    }

    public static FluidStack architecturyStack(Fluid fluid, @Nullable CompoundTag nbt) {
        return FluidStack.create(normalizeFluid(fluid), FluidStack.bucketAmount(), nbt);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public MutableComponent getAltTitle() {
        if (fluids.size() == 1) {
            return Component.literal("").append(fluids.get(0).getName());
        }

        return Component.translatable("ftbq_better_fluids.task.any_fluid", fluids.size());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Icon getAltIcon() {
        List<Icon> icons = new ArrayList<>();
        for (FluidStack stack : fluids) {
            icons.add(iconFor(stack));
        }
        return icons.size() == 1 ? icons.get(0) : IconAnimation.fromList(icons, false);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void fillConfigGroup(ConfigGroup config) {
        fillBaseTaskConfigGroup(config);
        config.addList("fluids", new ArrayList<>(fluids), new FluidConfig(false).showAmount(false), this::setFluids, architecturyStack(Fluids.WATER, null));
        config.add("fluid_nbt", new NBTConfig(), getFluidNBT(), v -> {
            setFluidNBT(v);
            clearCachedData();
        }, null);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Optional<PositionedIngredient> getIngredient(Widget widget) {
        return PositionedIngredient.of(getDisplayFluidStack(), widget);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onButtonClicked(Button button, boolean canClick) {
        button.playClickSound();
        if (fluids.size() > 1) {
            new FluidListScreen(this).openGui();
        } else {
            ClientRecipeViewers.showRecipes(createForgeFluidStack(getDisplayFluidStack()));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addMouseOverText(TooltipList list, TeamData teamData) {
        if (fluids.size() > 1) {
            list.blankLine();
            list.add(Component.translatable("ftbq_better_fluids.task.any_of"));
            for (FluidStack fluid : fluids) {
                list.add(Component.literal("- ").withStyle(ChatFormatting.YELLOW).append(Component.literal("").append(fluid.getName()).withStyle(ChatFormatting.WHITE)));
            }

            list.blankLine();
            list.add(Component.translatable("ftbq_better_fluids.task.click_valid_fluids").withStyle(ChatFormatting.YELLOW, ChatFormatting.UNDERLINE));
        } else if (ClientRecipeViewers.isRecipeViewerLoaded()) {
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
        if (contained.isEmpty()) {
            return false;
        }

        for (FluidStack configured : fluids) {
            if (contained.getFluid() == configured.getFluid() && (!configured.hasTag() || Objects.equals(configured.getTag(), contained.getTag()))) {
                return true;
            }
        }

        return false;
    }

    public static net.minecraftforge.fluids.FluidStack createForgeFluidStack(FluidStack stack) {
        return new net.minecraftforge.fluids.FluidStack(stack.getFluid(), 1000, stack.getTag());
    }

    private void setFluidNBT(@Nullable CompoundTag fluidNBT) {
        List<FluidStack> updated = new ArrayList<>(fluids.size());
        for (FluidStack stack : fluids) {
            updated.add(architecturyStack(stack.getFluid(), fluidNBT == null ? null : fluidNBT.copy()));
        }
        setFluids(updated);
    }

    private void setFluids(List<FluidStack> stacks) {
        fluids = new ArrayList<>();
        for (FluidStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                fluids.add(normalizeStack(stack));
            }
        }
        if (fluids.isEmpty()) {
            fluids.add(architecturyStack(Fluids.WATER, null));
        }

        FluidStack first = fluids.get(0);
        super.setFluid(first.getFluid());
        setSuperFluidNBT(first.getTag());
        clearCachedData();
    }

    private void setSuperFluidNBT(@Nullable CompoundTag fluidNBT) {
        CompoundTag nbt = new CompoundTag();
        super.writeData(nbt);
        if (fluidNBT == null) {
            nbt.remove("nbt");
        } else {
            nbt.put("nbt", fluidNBT.copy());
        }
        super.readData(nbt);
    }

    private FluidStack getDisplayFluidStack() {
        return fluids.get((int) (System.currentTimeMillis() / 1000L % fluids.size()));
    }

    private static FluidStack normalizeStack(FluidStack stack) {
        CompoundTag tag = stack.hasTag() ? stack.getTag().copy() : null;
        return architecturyStack(stack.getFluid(), tag);
    }

    private static Fluid normalizeFluid(@Nullable Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY ? Fluids.WATER : fluid;
    }

    @OnlyIn(Dist.CLIENT)
    public static Icon iconFor(FluidStack stack) {
        return Icon.getIcon(ClientUtils.getStillTexture(stack)).withTint(Color4I.rgb(ClientUtils.getFluidColor(stack)));
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
