package corgitaco.blockswap.client;

import corgitaco.blockswap.config.BlockSwapConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.Map.Entry;

public class BlockIndexScreen extends Screen {
    private final Screen parent;
    private BlockList list;
    private EditBox searchBox;
    private String filterText = "";

    // All block IDs for validation/autocomplete
    private List<String> allBlockIds;

    private Button doneBtn;

    public BlockIndexScreen(Screen parent) {
        super(Component.literal("Block Swap: Block Mapping"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int searchWidth = Math.min(300, this.width - 60);
        int searchY = 24;
        int top = searchY + 26; // below search box
        int bottom = this.height - 32;

        if (this.allBlockIds == null) {
            this.allBlockIds = new ArrayList<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                if (id != null) this.allBlockIds.add(id.toString());
            }
            Collections.sort(this.allBlockIds);
        }

        this.list = new BlockList(this.minecraft, this.width, this.height, top, bottom, 24, this.allBlockIds);
        this.addRenderableWidget(this.list);

        this.searchBox = new EditBox(this.font, (this.width - searchWidth) / 2, searchY, searchWidth, 20, Component.literal("Search"));
        this.searchBox.setMaxLength(256);
        this.searchBox.setValue(this.filterText);
        this.searchBox.setSuggestion(this.filterText.isEmpty() ? "Filter blocks..." : "");
        this.searchBox.setResponder(s -> {
            this.filterText = s;
            this.searchBox.setSuggestion(s.isEmpty() ? "Filter blocks..." : "");
            this.rebuildList();
        });
        this.addRenderableWidget(this.searchBox);

        this.rebuildList();

        int buttonWidth = 120;
        int y = bottom + 4;

        this.doneBtn = this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            // Persist current config to disk
            BlockSwapConfig.save(BlockSwapConfig.getConfig(false));
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - buttonWidth / 2, y, buttonWidth, 20).build());
    }

    public void rebuildList() {
        this.list.clearAll();
        String needle = this.filterText.trim().toLowerCase(Locale.ROOT);
        
        // Build a list of entries with their data
        class EntryData {
            String id;
            String target;
            boolean retro;
            boolean player;
            boolean hasModifications;
            
            EntryData(String id, String target, boolean retro, boolean player) {
                this.id = id;
                this.target = target;
                this.retro = retro;
                this.player = player;
                // Entry has modifications if it has a replacement, retro is on, or player is on
                this.hasModifications = (target != null && !target.isEmpty()) || retro || player;
            }
        }
        
        List<EntryData> entries = new ArrayList<>();
        BlockSwapConfig cfg = BlockSwapConfig.getConfig(false);
        Map<Block, Block> map = cfg.blockBlockMap();
        
        for (Block block : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null) continue;
            String idStr = id.toString();
            
            if (needle.isEmpty() || idStr.toLowerCase(Locale.ROOT).contains(needle)) {
                String target = null;
                boolean retro = false;
                boolean player = false;
                
                Block newBlock = map.get(block);
                if (newBlock != null) {
                    ResourceLocation nid = BuiltInRegistries.BLOCK.getKey(newBlock);
                    if (nid != null) target = nid.toString();
                }
                
                BlockSwapConfig.RuleFlags flags = cfg.rules().get(block);
                if (flags != null) {
                    retro = flags.retroGen();
                    player = flags.replacePlayerPlaced();
                }
                
                entries.add(new EntryData(idStr, target, retro, player));
            }
        }
        
        // Sort: modified entries first, then alphabetically by ID
        entries.sort((a, b) -> {
            // If one has modifications and the other doesn't, modified goes first
            if (a.hasModifications != b.hasModifications) {
                return a.hasModifications ? -1 : 1;
            }
            // Both have same modification status, sort alphabetically
            return a.id.compareTo(b.id);
        });
        
        // Add entries to the list
        for (EntryData data : entries) {
            this.list.addBlockEntry(new BlockEntry(data.id, data.target, data.retro, data.player, this.allBlockIds));
        }
    }

    @Override
    public void tick() {
        if (this.searchBox != null) this.searchBox.tick();
        if (this.list != null) {
            for (BlockEntry e : this.list.children()) {
                e.tick();
            }
        }
        super.tick();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Clear any open dropdowns before processing the click
        if (this.list != null) {
            for (BlockEntry e : this.list.children()) {
                if (e.targetBox.isFocused()) {
                    // Check if click is on the dropdown area
                    int boxX = e.targetBox.getX();
                    int boxY = e.targetBox.getY();
                    int dropY = boxY + 20;
                    int max = Math.min(e.filtered.size(), 8);
                    int itemH = 12;
                    
                    // If click is not on the text box or dropdown, clear it
                    boolean clickOnBox = mouseX >= boxX && mouseX <= boxX + e.boxW && mouseY >= boxY && mouseY <= boxY + 20;
                    boolean clickOnDropdown = !e.filtered.isEmpty() && 
                        mouseX >= boxX && mouseX <= boxX + e.boxW && 
                        mouseY >= dropY && mouseY <= dropY + max * itemH;
                    
                    if (!clickOnBox && !clickOnDropdown) {
                        e.filtered = Collections.emptyList();
                        e.highlight = -1;
                        e.targetBox.setFocused(false);
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);
        this.list.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private static class BlockList extends ObjectSelectionList<BlockEntry> {
        private final List<String> allBlockIds;

        public BlockList(Minecraft mc, int width, int screenHeight, int top, int bottom, int itemHeight, List<String> allBlockIds) {
            super(mc, width, screenHeight, top, bottom, itemHeight);
            this.allBlockIds = allBlockIds;
        }

        public void addBlockEntry(BlockEntry entry) {
            super.addEntry(entry);
        }

        public void clearAll() {
            this.clearEntries();
        }

        @Override
        public int getRowWidth() {
            return this.width - 30;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width - 6;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            for (BlockEntry e : this.children()) {
                if (e.keyPressed(keyCode, scanCode, modifiers)) return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            for (BlockEntry e : this.children()) {
                if (e.charTyped(codePoint, modifiers)) return true;
            }
            return super.charTyped(codePoint, modifiers);
        }
    }

    private static class BlockEntry extends ObjectSelectionList.Entry<BlockEntry> {
        private final String oldId;
        private final EditBox targetBox;
        private boolean valid;
        private final List<String> allBlockIds;
        private final CycleButton<Boolean> retroToggle;
        private final CycleButton<Boolean> playerToggle;
        private List<String> filtered = Collections.emptyList();
        private int highlight = -1;
        private int boxW = 140; // 70% of previous 200 to avoid cutting off labels

        public BlockEntry(String oldId, String initialTarget, boolean initialRetro, boolean initialPlayer, List<String> allBlockIds) {
            this.oldId = oldId;
            this.allBlockIds = allBlockIds;
            this.targetBox = new EditBox(Minecraft.getInstance().font, 0, 0, 140, 20, Component.literal("Replace with"));
            this.targetBox.setMaxLength(256);
            this.targetBox.setValue(initialTarget == null ? "" : initialTarget);
            this.targetBox.setSuggestion(initialTarget == null || initialTarget.isEmpty() ? "e.g. minecraft:stone" : "");
            this.valid = initialTarget != null && isValidId(initialTarget);
            this.targetBox.setResponder(s -> {
                this.valid = s.isEmpty() || isValidId(s);
                this.targetBox.setTextColor(this.valid || s.isEmpty() ? 0xE0E0E0 : 0xFF5555);
                this.targetBox.setSuggestion(s.isEmpty() ? "e.g. minecraft:stone" : "");
                updateFiltered(s);
                applyMapping();
            });

            // Toggles with custom display text
            this.retroToggle = CycleButton.onOffBuilder()
                .withInitialValue(initialRetro)
                .displayOnlyValue()
                .create(0, 0, 60, 20, Component.literal("Retro"), (b, v) -> {
                    b.setMessage(Component.literal(v ? "Retro: ON" : "Retro: OFF").withStyle(v ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.WHITE));
                    applyFlags(v, null);
                });
            this.retroToggle.setMessage(Component.literal(initialRetro ? "Retro: ON" : "Retro: OFF").withStyle(initialRetro ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.WHITE));
            this.retroToggle.setTooltip(Tooltip.create(Component.literal("If ON, also replace this block in existing chunks.")));
            
            this.playerToggle = CycleButton.onOffBuilder()
                .withInitialValue(initialPlayer)
                .displayOnlyValue()
                .create(0, 0, 60, 20, Component.literal("Player"), (b, v) -> {
                    b.setMessage(Component.literal(v ? "Player: ON" : "Player: OFF").withStyle(v ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.WHITE));
                    applyFlags(null, v);
                });
            this.playerToggle.setMessage(Component.literal(initialPlayer ? "Player: ON" : "Player: OFF").withStyle(initialPlayer ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.WHITE));
            this.playerToggle.setTooltip(Tooltip.create(Component.literal("If ON, replace when a player places the old block.")));
        }

        private boolean isValidId(String s) {
            if (!s.contains(":")) return false;
            ResourceLocation rl;
            try {
                rl = new ResourceLocation(s);
            } catch (Exception ex) {
                return false;
            }
            return BuiltInRegistries.BLOCK.containsKey(rl);
        }

        private void updateFiltered(String needle) {
            String n = needle.toLowerCase(Locale.ROOT).trim();
            if (n.isEmpty()) {
                this.filtered = Collections.emptyList();
                this.highlight = -1;
                return;
            }
            List<String> starts = new ArrayList<>();
            List<String> contains = new ArrayList<>();
            for (String id : this.allBlockIds) {
                String low = id.toLowerCase(Locale.ROOT);
                if (low.startsWith(n)) {
                    starts.add(id);
                } else if (low.contains(n)) {
                    contains.add(id);
                }
                if (starts.size() >= 50 && contains.size() >= 50) break; // cap work
            }
            List<String> combined = new ArrayList<>(starts);
            for (String c : contains) {
                if (combined.size() >= 50) break;
                combined.add(c);
            }
            this.filtered = combined;
            this.highlight = this.filtered.isEmpty() ? -1 : 0;
        }

        private void applyMapping() {
            BlockSwapConfig cfg = BlockSwapConfig.getConfig(false);
            // Work on mutable copies to avoid ImmutableMap issues from codec
            Map<Block, Block> newBlockMap = new java.util.IdentityHashMap<>(cfg.blockBlockMap());
            Map<BlockState, BlockState> newStateMap = new java.util.IdentityHashMap<>(cfg.blockStateBlockStateMap());
            Map<Block, BlockSwapConfig.RuleFlags> rules = new java.util.IdentityHashMap<>(cfg.rules());

            Block oldBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation(this.oldId));
            String s = this.targetBox.getValue().trim();
            if (oldBlock != null) {
                if (s.isEmpty()) {
                    newBlockMap.remove(oldBlock);
                } else if (isValidId(s)) {
                    Block newBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation(s));
                    if (newBlock != null) newBlockMap.put(oldBlock, newBlock);
                }
                // Ensure rules entry exists
                rules.putIfAbsent(oldBlock, BlockSwapConfig.RuleFlags.DEFAULT);
                // Push updated config to runtime (save happens on Done)
                BlockSwapConfig.getConfig(new BlockSwapConfig(newBlockMap, newStateMap, cfg.retroGen(), cfg.generateAllKnownStates(), rules));
            }
        }

        @Override
        public void render(GuiGraphics gfx, int index, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean hovered, float partialTick) {
            // Layout: [label] [Retro] [Player] [ReplaceWith box] [x]
            int y = top + (rowHeight - 20) / 2;

            // Vanilla-style simple scaling: keep control widths fixed and trim label.
            // Let the text box scale modestly with row width within a safe clamp.
            int clearW = 20; // space for the clear 'x'
            this.boxW = Math.max(100, Math.min(150, rowWidth / 4));

            int retroX = left + rowWidth - (this.boxW + clearW) - 60 - 60 - 8; // space for both toggles and gap
            int playerX = retroX + 60 + 4;
            this.retroToggle.setX(retroX);
            this.retroToggle.setY(y);
            this.playerToggle.setX(playerX);
            this.playerToggle.setY(y);

            // Draw label trimmed so it doesn't render under controls
            int labelX = left + 4;
            int labelAvail = Math.max(0, retroX - (labelX + 4));
            String labelStr = this.oldId;
            if (labelAvail > 0) {
                Font font = Minecraft.getInstance().font;
                if (font.width(labelStr) > labelAvail) {
                    int ellipsesW = font.width("...");
                    String cut = font.plainSubstrByWidth(labelStr, Math.max(0, labelAvail - ellipsesW));
                    if (cut.length() < labelStr.length()) labelStr = cut + "...";
                }
                gfx.drawString(font, labelStr, labelX, top + (rowHeight - 9) / 2, 0xFFFFFF, false);
            }

            // Draw border around enabled toggles
            if (this.retroToggle.getValue()) {
                gfx.renderOutline(retroX - 1, y - 1, 62, 22, 0xFF00FF00); // Green border
            }
            if (this.playerToggle.getValue()) {
                gfx.renderOutline(playerX - 1, y - 1, 62, 22, 0xFF00FF00); // Green border
            }

            this.retroToggle.render(gfx, mouseX, mouseY, partialTick);
            this.playerToggle.render(gfx, mouseX, mouseY, partialTick);

            int boxX = left + rowWidth - (this.boxW + clearW);
            int boxY = y;
            this.targetBox.setX(boxX);
            this.targetBox.setY(boxY);
            this.targetBox.setWidth(this.boxW);
            this.targetBox.render(gfx, mouseX, mouseY, partialTick);

            // Clear button (x) - visual only, click handling is in mouseClicked
            int clearX = left + rowWidth - 16;
            int clearY = top + (rowHeight - 12) / 2;
            boolean clearHovered = mouseX >= clearX && mouseX <= clearX + 12 && mouseY >= clearY && mouseY <= clearY + 12;
            gfx.drawString(Minecraft.getInstance().font, Component.literal("x"), clearX + 3, clearY + 2, clearHovered ? 0xFFFFFF : 0xAAAAAA, false);

            // Dropdown suggestions (drawn here with raised Z so it appears above following rows)
            if (this.targetBox.isFocused() && !this.filtered.isEmpty()) {
                int max = Math.min(this.filtered.size(), 8);
                int itemH = 12;
                int dropX = boxX;
                int dropY = boxY + 20;
                int dropW = this.boxW;
                gfx.pose().pushPose();
                gfx.pose().translate(0, 0, 400);
                int bg = 0xAA000000; // translucent black
                gfx.fill(dropX, dropY, dropX + dropW, dropY + max * itemH, bg);
                for (int i = 0; i < max; i++) {
                    int y2 = dropY + i * itemH;
                    if (i == this.highlight) {
                        gfx.fill(dropX, y2, dropX + dropW, y2 + itemH, 0xAA444444);
                    }
                    String text = this.filtered.get(i);
                    gfx.drawString(Minecraft.getInstance().font, text, dropX + 4, y2 + 2, 0xE0E0E0, false);
                }
                gfx.pose().popPose();
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            // Check clear button first
            int clearX = this.targetBox.getX() + this.boxW + 4;
            int clearY = this.targetBox.getY() + 4;
            if (button == 0 && mouseX >= clearX && mouseX <= clearX + 12 && mouseY >= clearY && mouseY <= clearY + 12) {
                // Clear the text but keep the dropdown open if it was open
                this.targetBox.setValue("");
                this.targetBox.setSuggestion("e.g. minecraft:stone");
                updateFiltered("");
                applyMapping();
                // Keep focus on the text box
                this.targetBox.setFocused(true);
                return true;
            }
            
            boolean t1 = this.retroToggle.mouseClicked(mouseX, mouseY, button);
            boolean t2 = this.playerToggle.mouseClicked(mouseX, mouseY, button);

            boolean inside = this.targetBox.mouseClicked(mouseX, mouseY, button);
            this.targetBox.setFocused(inside);
            if (inside) updateFiltered(this.targetBox.getValue());

            // Click on dropdown
            if (this.targetBox.isFocused() && !this.filtered.isEmpty()) {
                int boxX = this.targetBox.getX();
                int boxY = this.targetBox.getY();
                int dropX = boxX;
                int dropY = boxY + 20;
                int itemH = 12;
                int max = Math.min(this.filtered.size(), 8);
                if (mouseX >= dropX && mouseX <= dropX + this.boxW && mouseY >= dropY && mouseY <= dropY + max * itemH) {
                    int idx = (int) ((mouseY - dropY) / itemH);
                    if (idx >= 0 && idx < max) {
                        String pick = this.filtered.get(idx);
                        this.targetBox.setValue(pick);
                        this.targetBox.setSuggestion("");
                        updateFiltered(pick);
                        applyMapping();
                        return true;
                    }
                }
            }
            // Don't clear dropdown here - let the parent screen handle it
            return t1 || t2 || inside;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            boolean r1 = this.retroToggle.mouseReleased(mouseX, mouseY, button);
            boolean r2 = this.playerToggle.mouseReleased(mouseX, mouseY, button);
            return this.targetBox.mouseReleased(mouseX, mouseY, button) || r1 || r2;
        }

        public void tick() {
            this.targetBox.tick();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            // Let toggles handle space/enter
            if (this.retroToggle.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (this.playerToggle.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (this.targetBox.isFocused() && !this.filtered.isEmpty()) {
                if (keyCode == GLFW.GLFW_KEY_DOWN) {
                    this.highlight = (this.highlight + 1) % Math.min(this.filtered.size(), 8);
                    return true;
                } else if (keyCode == GLFW.GLFW_KEY_UP) {
                    int max = Math.min(this.filtered.size(), 8);
                    this.highlight = (this.highlight - 1 + max) % max;
                    return true;
                } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                    if (this.highlight >= 0 && this.highlight < Math.min(this.filtered.size(), 8)) {
                        String pick = this.filtered.get(this.highlight);
                        this.targetBox.setValue(pick);
                        this.targetBox.setSuggestion("");
                        updateFiltered(pick);
                        applyMapping();
                        return true;
                    }
                }
            }
            return this.targetBox.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            return this.targetBox.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
        }

        private void applyFlags(Boolean retro, Boolean player) {
            BlockSwapConfig cfg = BlockSwapConfig.getConfig(false);
            Map<Block, Block> newBlockMap = new java.util.IdentityHashMap<>(cfg.blockBlockMap());
            Map<BlockState, BlockState> newStateMap = new java.util.IdentityHashMap<>(cfg.blockStateBlockStateMap());
            Map<Block, BlockSwapConfig.RuleFlags> rules = new java.util.IdentityHashMap<>(cfg.rules());
            Block oldBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation(this.oldId));
            if (oldBlock == null) return;
            BlockSwapConfig.RuleFlags cur = rules.getOrDefault(oldBlock, BlockSwapConfig.RuleFlags.DEFAULT);
            boolean r = retro == null ? cur.retroGen() : retro;
            boolean p = player == null ? cur.replacePlayerPlaced() : player;
            rules.put(oldBlock, new BlockSwapConfig.RuleFlags(r, p));
            BlockSwapConfig.getConfig(new BlockSwapConfig(newBlockMap, newStateMap, cfg.retroGen(), cfg.generateAllKnownStates(), rules));
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.oldId + ": replace with -> " + this.targetBox.getValue());
        }
    }
}
