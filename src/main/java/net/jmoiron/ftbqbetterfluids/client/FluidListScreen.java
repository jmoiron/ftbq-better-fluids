package net.jmoiron.ftbqbetterfluids.client;

import dev.architectury.fluid.FluidStack;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleTextButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;
import dev.ftb.mods.ftblibrary.ui.WidgetType;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.CompactGridLayout;
import dev.ftb.mods.ftblibrary.util.client.PositionedIngredient;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.FTBQuestsTheme;
import net.jmoiron.ftbqbetterfluids.FluidPlusTask;
import net.jmoiron.ftbqbetterfluids.client.recipe.ClientRecipeViewers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public class FluidListScreen extends BaseScreen {
    private final Component title;
    private final List<FluidStack> fluids;
    private final Panel fluidPanel;
    private final Button backButton;

    public FluidListScreen(FluidPlusTask task) {
        title = Component.translatable("ftbq_better_fluids.task.valid_fluids_for", task.getTitle());
        fluids = task.getFluids();

        fluidPanel = new Panel(this) {
            @Override
            public void addWidgets() {
                for (FluidStack fluid : fluids) {
                    add(new FluidButton(this, fluid));
                }
            }

            @Override
            public void alignWidgets() {
                align(new CompactGridLayout(36));
                setHeight(Math.min(160, getContentHeight()));
                parent.setHeight(height + 53);
                int off = (width - getContentWidth()) / 2;

                for (Widget widget : widgets) {
                    widget.setX(widget.posX + off);
                }

                fluidPanel.setX((parent.width - width) / 2);
                backButton.setPosAndSize(fluidPanel.posX + (width - 70) / 2, height + 28, 70, 20);
            }

            @Override
            public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
                theme.drawButton(graphics, x - 1, y - 1, w + 2, h + 2, WidgetType.NORMAL);
            }
        };

        fluidPanel.setPosAndSize(0, 22, 144, 0);

        backButton = new SimpleTextButton(this, Component.translatable("gui.back"), Color4I.empty()) {
            @Override
            public void onClicked(MouseButton button) {
                playClickSound();
                onBack();
            }

            @Override
            public boolean renderTitleInCenter() {
                return true;
            }
        };
    }

    @Override
    public void addWidgets() {
        setWidth(Math.max(156, getTheme().getStringWidth(title) + 12));
        add(fluidPanel);
        add(backButton);
    }

    @Override
    public Theme getTheme() {
        return FTBQuestsTheme.INSTANCE;
    }

    @Override
    public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        super.drawBackground(graphics, theme, x, y, w, h);
        theme.drawString(graphics, title, x + w / 2, y + 6, Color4I.WHITE, Theme.CENTERED);
    }

    @Override
    public boolean keyPressed(Key key) {
        if (super.keyPressed(key)) {
            return true;
        }
        if (key.esc()) {
            onBack();
            return true;
        }
        return false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return ClientQuestFile.exists() && ClientQuestFile.INSTANCE.isPauseGame();
    }

    @Override
    public boolean onClosedByKey(Key key) {
        if (super.onClosedByKey(key)) {
            onBack();
        }

        return false;
    }

    private static class FluidButton extends Button {
        private final FluidStack fluid;

        FluidButton(Panel panel, FluidStack fluid) {
            super(panel, fluid.getName(), FluidPlusTask.iconFor(fluid));
            this.fluid = fluid;
        }

        @Override
        public void onClicked(MouseButton button) {
            playClickSound();
            ClientRecipeViewers.showRecipes(FluidPlusTask.createForgeFluidStack(fluid));
        }

        @Override
        public Optional<PositionedIngredient> getIngredientUnderMouse() {
            return PositionedIngredient.of(fluid, this, true);
        }

        @Override
        public void draw(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
            if (isMouseOver()) {
                Color4I.WHITE.withAlpha(33).draw(graphics, x, y, w, h);
            }

            icon.draw(graphics, x + 2, y + 2, w - 4, h - 4);
        }
    }
}
