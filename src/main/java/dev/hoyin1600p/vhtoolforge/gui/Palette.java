package dev.hoyin1600p.vhtoolforge.gui;

/** Every colour the planner screen draws with, as ARGB ints. The first block is the Vault Hunters / vanilla container palette. */
final class Palette {
    // Container palette
    static final int PANEL = 0xFFC6C6C6, PANEL_HI = 0xFFFFFFFF, PANEL_LO = 0xFF555555, EDGE = 0xFF000000;
    static final int TITLE = 0xFF404040, SLOT = 0xFF8B8B8B, SLOT_DARK = 0xFF373737;
    static final int INSET = 0xFF2B2B2B, ROW = 0xFF333333, ROW_HOVER = 0xFF3D3D3D, TILE = 0xFF1E1E1E;
    static final int TEXT = 0xFFE0E0E0, DIM = 0xFF9A9A9A, GOOD = 0xFF55FF55, PART = 0xFFFFAA00, BAD = 0xFFFF5555;
    static final int UNIQUE = 0xFFFF9A1F, CRAFT = 0xFFFFD84A;

    // Plain
    static final int WHITE = 0xFFFFFFFF, BLACK = 0xFF000000, GOLD = 0xFFFFD84A;

    // Translucent shades laid over things
    static final int SHADE_LIGHT = 0x55000000, SHADE_HEAVY = 0x99000000, SHADE_COLLECTED = 0x88000000;

    // Top bar
    static final int HINT_LOCKED = 0xFF8A5A00, HINT_LIVE = 0xFF5C5C5C;

    // Tabs and capacity bar
    static final int TAB_IDLE = 0xFF3A3A3A, CAPACITY_FILL = 0xFFE09A2A;

    // Cards, tiles and outlines
    static final int CARD_EDGE = 0xFF444444, TILE_ON = 0xFF2A2A2A, TRACK = 0xFF151515;

    // Secondary text
    static final int NOTE = 0xFF7A7A7A, DISABLED = 0xFF555555, RESULT_IDLE = 0xFF666666, FLAG_OFF = 0xFF8A8A8A;
    static final int FOOTNOTE = 0xFF6E6E6E, HOVER_HINT = 0xFF6A6A6A, SIZE_FIXED = 0xFFDDDDDD, UNIQUE_OFF = 0xFF8A6A40;

    // Priority button and dropdown
    static final int RANK_EDGE_SET = 0xFF8A7420;
    static final int DROPDOWN_BG = 0xFF101010, DROPDOWN_EDGE = 0xFFAAAAAA, DROPDOWN_HOVER = 0xFF3A3A6A;

    // Backpack choice and scrollbar
    static final int BACKPACK_SELECTED = 0xFF2F3A2F, SCROLL_THUMB = 0xFF8A8A8A;

    // Buttons
    static final int BUTTON = 0xFF6F6F6F, BUTTON_HOVER = 0xFF7E88B8, BUTTON_SHADE = 0xFF4A4A4A, BUTTON_SHADE_HOVER = 0xFF4A5280;

    // Slots and checkboxes
    static final int SLOT_SELECTED = 0xFFE8E8E8, CHECK_EDGE = 0xFFA0A0A0, CHECK_ON = 0xFF55AA55;

    private Palette() {}
}
