package net.wieldyourpower.client.cloth;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * A helper row that composes a filter entry and appends it to a list, so pack authors do not have to type
 * commas by hand. An optional cycle button picks the rule list (black/white); another picks the type
 * (e.g. item/tag/mod/source or tag/type/uuid).
 */
public final class QuickAddEntry extends AbstractConfigListEntry<List<String>> {

    private final List<String> target;
    private final String[] lists;
    private final String[] types;

    private final EditBox valueBox;
    private final Button listButton;
    private final Button typeButton;
    private final Button addButton;

    private int listIndex;
    private int typeIndex;
    private net.minecraft.client.gui.components.events.GuiEventListener focusedListener;
    private boolean dragging;

    public QuickAddEntry(Component fieldName, List<String> target, String[] lists, String[] types) {
        super(fieldName, false);
        this.target = target;
        this.lists = lists;
        this.types = types;

        this.valueBox = new EditBox(Minecraft.getInstance().font, 0, 0, 120, 18,
                Component.translatable("wieldyourpower.field.filter_value"));
        this.valueBox.setMaxLength(256);
        this.valueBox.setHint(Component.translatable("wieldyourpower.field.filter_value"));

        this.listButton = lists.length > 0
                ? Button.builder(Component.literal(lists[0]), b -> {
                    listIndex = (listIndex + 1) % lists.length;
                    b.setMessage(Component.literal(lists[listIndex]));
                }).bounds(0, 0, 54, 18).build()
                : null;

        this.typeButton = Button.builder(Component.literal(types[0]), b -> {
            typeIndex = (typeIndex + 1) % types.length;
            b.setMessage(Component.literal(types[typeIndex]));
        }).bounds(0, 0, 76, 18).build();

        this.addButton = Button.builder(Component.translatable("wieldyourpower.button.add"), b -> add())
                .bounds(0, 0, 40, 18).build();
    }

    @Override
    public List<String> getValue() {
        return target;
    }

    @Override
    public Optional<List<String>> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        graphics.drawString(Minecraft.getInstance().font, getFieldName(), x, y + 6, 0xFFFFFF);

        int cursor = x + entryWidth - 40 - 120 - 76 - (listButton != null ? 54 : 0) - 8;
        if (cursor < x + 4) {
            cursor = x + 4;
        }
        int buttonY = y + 1;

        if (listButton != null) {
            listButton.setX(cursor);
            listButton.setY(buttonY);
            listButton.render(graphics, mouseX, mouseY, delta);
            cursor += 54;
        }
        typeButton.setX(cursor);
        typeButton.setY(buttonY);
        typeButton.render(graphics, mouseX, mouseY, delta);
        cursor += 76;

        valueBox.setX(cursor);
        valueBox.setY(buttonY);
        valueBox.setWidth(120);
        valueBox.setHeight(18);
        valueBox.render(graphics, mouseX, mouseY, delta);
        cursor += 120;

        addButton.setX(cursor);
        addButton.setY(buttonY);
        addButton.render(graphics, mouseX, mouseY, delta);
    }

    private void add() {
        String value = valueBox.getValue().trim();
        if (value.isEmpty()) {
            return;
        }
        String entry = listButton != null
                ? lists[listIndex] + "," + types[typeIndex] + "," + value
                : types[typeIndex] + "," + value;
        if (!target.contains(entry)) {
            target.add(entry);
        }
        valueBox.setValue("");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return click(listButton, mouseX, mouseY, button)
                | click(typeButton, mouseX, mouseY, button)
                | click(addButton, mouseX, mouseY, button)
                | valueBox.mouseClicked(mouseX, mouseY, button)
                | super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return release(listButton, mouseX, mouseY, button)
                | release(typeButton, mouseX, mouseY, button)
                | release(addButton, mouseX, mouseY, button)
                | valueBox.mouseReleased(mouseX, mouseY, button)
                | super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return valueBox.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return valueBox.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public java.util.List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
        return java.util.List.of(valueBox);
    }

    // The SRG Cloth class leaves the (official-named) container focus methods abstract under dev mappings.
    @Override
    public void setFocused(net.minecraft.client.gui.components.events.GuiEventListener focused) {
        this.focusedListener = focused;
    }

    @Override
    public net.minecraft.client.gui.components.events.GuiEventListener getFocused() {
        return focusedListener;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    @Override
    public java.util.List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        java.util.List<net.minecraft.client.gui.components.events.GuiEventListener> list = new java.util.ArrayList<>();
        if (listButton != null) {
            list.add(listButton);
        }
        list.add(typeButton);
        list.add(valueBox);
        list.add(addButton);
        return list;
    }

    @Override
    public net.minecraft.client.gui.narration.NarratableEntry.NarrationPriority narrationPriority() {
        return net.minecraft.client.gui.narration.NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
    }

    @Override
    public boolean isActive() {
        return isEditable();
    }

    private static boolean click(AbstractWidget widget, double mouseX, double mouseY, int button) {
        return widget != null && widget.isMouseOver(mouseX, mouseY) && widget.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean release(AbstractWidget widget, double mouseX, double mouseY, int button) {
        return widget != null && widget.mouseReleased(mouseX, mouseY, button);
    }
}
