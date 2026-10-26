package com.promatch.gui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Colours, fonts and small reusable Swing components shared by every screen,
 * so the look matches the old React version (maroon + white cards).
 */
final class Theme {

    private Theme() { }

    // ---- colours (taken from the React theme) ----
    static final Color MAROON = new Color(0x800020);
    static final Color MAROON_HOVER = new Color(0x680018);
    static final Color SIDEBAR_LINE = new Color(0x932641);
    static final Color BRAND_50 = new Color(0xFBEFF2);
    static final Color BRAND_100 = new Color(0xF3D6DD);
    static final Color TEXT_PRIMARY = new Color(0x1E293B);
    static final Color TEXT_SECONDARY = new Color(0x475569);
    static final Color TEXT_MUTED = new Color(0x64748B);
    static final Color BG_PAGE = new Color(0xF8FAFC);
    static final Color BORDER = new Color(0xE2E8F0);
    static final Color BORDER_STRONG = new Color(0xCBD5E1);
    static final Color MATCH_GREEN = new Color(0x16A34A);
    static final Color MATCH_BG = new Color(0xF0FDF4);
    static final Color NEUTRAL_BG = new Color(0xF1F5F9);
    static final Color WARNING = new Color(0xD97706);
    static final Color ERROR = new Color(0xDC2626);

    // ---- text shown in the UI (change these in one place) ----
    static final String ORG_NAME = "GEC Thrissur";
    static final String APP_SUBTITLE = ORG_NAME + " Portal";
    static final String[] DEPARTMENTS = {
            "Computer Science & Engineering",
            "Electronics & Communication Engineering",
            "Electrical & Electronics Engineering",
            "Mechanical Engineering",
            "Civil Engineering",
            "Architecture",
            "Chemical Engineering",
            "Production Engineering"
    };

    // ---- fonts ----
    private static final String FONT_FAMILY = pickFontFamily();

    private static String pickFontFamily() {
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String wanted : new String[]{"Inter", "Segoe UI", "Helvetica Neue", "SF Pro Text"}) {
            if (installed.contains(wanted)) {
                return wanted;
            }
        }
        return Font.SANS_SERIF;
    }

    static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    // ---- small text helpers ----

    static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        // lets long text shrink (with "...") instead of forcing the layout wider
        l.setMinimumSize(new Dimension(0, l.getPreferredSize().height));
        return l;
    }

    /** A label whose text wraps at a fixed pixel width. */
    static JLabel wrapLabel(String text, int widthPx, int style, int size, Color color) {
        String safe = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        JLabel l = new JLabel("<html><div style='width:" + widthPx + "px'>" + safe + "</div></html>");
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    /** "java, SQL , java" -> [java, SQL] (trimmed, no empty items, no duplicates). */
    static List<String> parseSkills(String text) {
        List<String> skills = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String part : text.split(",")) {
            String s = part.trim();
            if (!s.isEmpty() && seen.add(s.toLowerCase())) {
                skills.add(s);
            }
        }
        return skills;
    }

    // ---- layout helpers ----

    /** A vertical stack: add rows with addRow(). Rows keep their own height. */
    static JPanel column() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        return p;
    }

    static void addRow(JPanel column, Component comp, int gapBelow) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = column.getComponentCount();
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(0, 0, gapBelow, 0);
        column.add(comp, gc);
    }

    /** Pins a component to the top of its area so it doesn't stretch downwards. */
    static JPanel top(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    /** Small grey label above an input. */
    static JPanel labeled(String text, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(label(text, Font.BOLD, 13, TEXT_SECONDARY), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    static JPanel pageHeader(String title, String subtitle) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 4));
        p.setOpaque(false);
        p.add(label(title, Font.BOLD, 30, TEXT_PRIMARY));
        p.add(label(subtitle, Font.PLAIN, 15, TEXT_MUTED));
        return p;
    }

    private static JComponent separator() {
        JPanel sep = new JPanel();
        sep.setBackground(BORDER);
        sep.setPreferredSize(new Dimension(1, 1));
        return sep;
    }

    /** White rounded card with a title row (optional action on the right) and a divider. */
    static RoundedPanel card(String title, JComponent rightAction, JComponent body) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 16), 16, Color.WHITE, BORDER);
        card.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(label(title, Font.BOLD, 18, TEXT_PRIMARY), BorderLayout.WEST);
        if (rightAction != null) {
            head.add(rightAction, BorderLayout.EAST);
        }
        JPanel north = new JPanel(new BorderLayout(0, 14));
        north.setOpaque(false);
        north.add(head, BorderLayout.NORTH);
        north.add(separator(), BorderLayout.SOUTH);

        card.add(north, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    static PillButton link(String text, ActionListener action) {
        PillButton b = new PillButton(text, PillButton.Kind.LINK);
        b.addActionListener(action);
        return b;
    }

    // ---- input styling ----

    static void styleField(JTextComponent f) {
        f.setFont(font(Font.PLAIN, 14));
        f.setForeground(TEXT_PRIMARY);
        f.setBackground(Color.WHITE);
        f.setCaretColor(MAROON);
        f.setBorder(new RoundedBorder(BORDER_STRONG, 10, new Insets(10, 12, 10, 12)));
    }

    static void styleCombo(JComboBox<?> c) {
        c.setFont(font(Font.PLAIN, 14));
        c.setBackground(Color.WHITE);
        c.setForeground(TEXT_PRIMARY);
        c.setPreferredSize(new Dimension(c.getPreferredSize().width, 40));
    }

    static void styleCheck(JCheckBox c) {
        c.setFont(font(Font.PLAIN, 13));
        c.setForeground(TEXT_SECONDARY);
        c.setOpaque(false);
        c.setFocusPainted(false);
    }

    static void styleTable(JTable t) {
        t.setRowHeight(46);
        t.setFont(font(Font.PLAIN, 14));
        t.setForeground(TEXT_PRIMARY);
        t.setBackground(Color.WHITE);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(BORDER);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(BRAND_50);
        t.setSelectionForeground(TEXT_PRIMARY);
        t.setFillsViewportHeight(true);
        t.setDefaultRenderer(Object.class, new CellRenderer(null, false, SwingConstants.LEFT));

        JTableHeader header = t.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, false, false, row, column);
                setText(value == null ? "" : value.toString().toUpperCase());
                setBackground(NEUTRAL_BG);
                setForeground(TEXT_MUTED);
                setFont(font(Font.BOLD, 12));
                setBorder(new EmptyBorder(0, 14, 0, 14));
                // line the heading up with the column's cells
                javax.swing.table.TableCellRenderer cellRenderer = table.getColumnModel().getColumn(column).getCellRenderer();
                setHorizontalAlignment(cellRenderer instanceof CellRenderer
                        ? ((CellRenderer) cellRenderer).align : SwingConstants.LEFT);
                return this;
            }
        });
    }

    static Color matchColor(String text) {
        try {
            int v = Integer.parseInt(text.replace("%", "").trim());
            return v >= 70 ? MATCH_GREEN : v >= 40 ? WARNING : TEXT_MUTED;
        } catch (NumberFormatException e) {
            return TEXT_PRIMARY;
        }
    }

    static Color statusColor(String status) {
        switch (status) {
            case "Accepted": return MATCH_GREEN;
            case "Declined": return ERROR;
            case "Pending": return WARNING;
            default: return TEXT_PRIMARY;
        }
    }

    // =====================================================================
    //  Reusable components
    // =====================================================================

    /** Table cell with padding and an optional colour/bold rule based on the text. */
    static class CellRenderer extends DefaultTableCellRenderer {
        private final Function<String, Color> colorRule;
        private final boolean bold;
        private final int align;

        CellRenderer(Function<String, Color> colorRule, boolean bold, int align) {
            this.colorRule = colorRule;
            this.bold = bold;
            this.align = align;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            String text = value == null ? "" : value.toString();
            setBorder(new EmptyBorder(0, 14, 0, 14));
            setHorizontalAlignment(align);
            setFont(font(bold ? Font.BOLD : Font.PLAIN, 14));
            setForeground(colorRule == null ? TEXT_PRIMARY : colorRule.apply(text));
            return this;
        }
    }

    /** Rounded-corner panel (cards). */
    static class RoundedPanel extends JPanel {
        private final int arc;
        private Color fill;
        private final Color line;

        RoundedPanel(LayoutManager layout, int arc, Color fill, Color line) {
            super(layout);
            this.arc = arc;
            this.fill = fill;
            this.line = line;
            setOpaque(false);
        }

        void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc));
            if (line != null) {
                g2.setColor(line);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Thin rounded outline for text fields. */
    static class RoundedBorder implements Border {
        private final Color color;
        private final int arc;
        private final Insets insets;

        RoundedBorder(Color color, int arc, Insets insets) {
            this.color = color;
            this.arc = arc;
            this.insets = insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return insets;
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /** Rounded button used everywhere (primary, outlined, tab, or plain link style). */
    static class PillButton extends JButton {
        enum Kind { PRIMARY, SECONDARY, TAB_ON, TAB_OFF, LINK }

        private Kind kind;
        private boolean hover;

        PillButton(String text, Kind kind) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
            applyKind();
        }

        void setKind(Kind kind) {
            this.kind = kind;
            applyKind();
            repaint();
        }

        private void applyKind() {
            if (kind == Kind.LINK) {
                setFont(font(Font.BOLD, 13));
                setBorder(new EmptyBorder(4, 6, 4, 6));
                setForeground(MAROON);
            } else {
                setFont(font(Font.BOLD, 14));
                setBorder(new EmptyBorder(11, 22, 11, 22));
                switch (kind) {
                    case PRIMARY: setForeground(Color.WHITE); break;
                    case SECONDARY: setForeground(MAROON); break;
                    case TAB_ON: setForeground(MAROON); break;
                    default: setForeground(TEXT_MUTED); break;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = null;
            Color line = null;
            int arc = 12;
            switch (kind) {
                case PRIMARY:
                    fill = !isEnabled() ? BORDER_STRONG : hover ? MAROON_HOVER : MAROON;
                    break;
                case SECONDARY:
                    fill = hover ? BRAND_50 : Color.WHITE;
                    line = MAROON;
                    break;
                case TAB_ON:
                    fill = Color.WHITE;
                    arc = 10;
                    break;
                case TAB_OFF:
                    fill = hover ? BORDER : null;
                    arc = 10;
                    break;
                default:
                    break;
            }
            if (fill != null) {
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Round logo mark with "PM" (stands in for the college crest). */
    static class Crest extends JComponent {
        private final int size;
        private final boolean onDark;

        Crest(int size, boolean onDark) {
            this.size = size;
            this.onDark = onDark;
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(onDark ? Color.WHITE : MAROON);
            g2.fillOval(0, 0, size - 1, size - 1);
            g2.setColor(onDark ? MAROON : Color.WHITE);
            g2.setFont(font(Font.BOLD, size * 2 / 5));
            FontMetrics fm = g2.getFontMetrics();
            String text = "PM";
            g2.drawString(text, (size - fm.stringWidth(text)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Circle with the person's initials. */
    static class Avatar extends JComponent {
        private final String initials;
        private final int size;

        Avatar(String name, int size) {
            this.size = size;
            this.initials = initialsOf(name);
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        private static String initialsOf(String name) {
            String[] parts = name.trim().split("\\s+");
            String first = parts[0].substring(0, 1);
            String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
            return (first + last).toUpperCase();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(BRAND_100);
            g2.fillOval(0, 0, size - 1, size - 1);
            g2.setColor(MAROON);
            g2.setFont(font(Font.BOLD, Math.max(10, size * 2 / 5)));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initials, (size - fm.stringWidth(initials)) / 2,
                    (size - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Small rounded tag, e.g. a skill name. */
    static class Chip extends JLabel {
        private final Color bg;
        private final Color line;

        Chip(String text, Color fg, Color bg, Color line) {
            super(text);
            this.bg = bg;
            this.line = line;
            setFont(font(Font.BOLD, 12));
            setForeground(fg);
            setBorder(new EmptyBorder(3, 9, 3, 9));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(line);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static Chip skillChip(String text, boolean matched) {
        return matched
                ? new Chip(text, MATCH_GREEN, MATCH_BG, MATCH_GREEN)
                : new Chip(text, TEXT_SECONDARY, NEUTRAL_BG, BORDER);
    }

    /** A row of skill chips: matched skills first (green), at most 'max' shown, then "+n". */
    static JPanel chipRow(List<String> skills, List<String> matched, int max, int align) {
        JPanel row = new JPanel(new FlowLayout(align, 6, 0));
        row.setOpaque(false);
        List<String> ordered = new ArrayList<>(matched);
        for (String s : skills) {
            if (!ordered.contains(s)) {
                ordered.add(s);
            }
        }
        for (int i = 0; i < Math.min(max, ordered.size()); i++) {
            String s = ordered.get(i);
            row.add(skillChip(s, matched.contains(s)));
        }
        if (ordered.size() > max) {
            row.add(skillChip("+" + (ordered.size() - max), false));
        }
        return row;
    }

    /** "Team Progress   3/5 members" with a maroon bar. */
    static JPanel teamProgress(int filled, int total) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("Team Progress", Font.PLAIN, 12, TEXT_MUTED), BorderLayout.WEST);
        top.add(label(filled + "/" + total + " members", Font.BOLD, 12, TEXT_SECONDARY), BorderLayout.EAST);

        final double ratio = total <= 0 ? 0 : Math.min(1.0, (double) filled / total);
        JComponent bar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(NEUTRAL_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(MAROON);
                g2.fillRoundRect(0, 0, (int) (getWidth() * ratio), getHeight(), getHeight(), getHeight());
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(10, 8));

        p.add(top, BorderLayout.NORTH);
        p.add(bar, BorderLayout.CENTER);
        return p;
    }

    /** A page that is as wide as the window (no sideways scrolling) and scrolls up/down. */
    static class ScrollPage extends JPanel implements Scrollable {
        ScrollPage(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(40, visibleRect.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getViewport().setBackground(BG_PAGE);
        return sp;
    }
}