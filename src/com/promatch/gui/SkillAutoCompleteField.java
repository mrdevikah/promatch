package com.promatch.gui;

import com.promatch.service.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A text field for comma-separated skills ("Java, SQL, Figma") that suggests skills from the
 * database while you type. Only the word you are currently typing (the part between two commas)
 * is matched.
 *
 *   - Type "fig"          -> a list drops down with matching skills, e.g. "Figma"
 *   - Skill not in list   -> the last row says: + Add "fig" as a new skill (it is saved when you
 *                            save the form, and then everybody gets it as a suggestion)
 *   - Up / Down + Enter, or a mouse click, picks a suggestion. Esc closes the list.
 *
 * The matching rules are in the two static methods at the bottom so they are easy to read and test.
 */
class SkillAutoCompleteField extends JTextField {

    private static final int MAX_SHOWN = 6;      // suggestions from the database (plus 1 "add new" row)
    private static final int ROW_HEIGHT = 30;

    /** One row of the drop-down: the text shown, and the text that goes into the field. */
    static final class Suggestion {
        final String label;
        final String value;
        final boolean isNew;

        Suggestion(String label, String value, boolean isNew) {
            this.label = label;
            this.value = value;
            this.isNew = isNew;
        }
    }

    /** The field's new text and caret position after a suggestion was picked. */
    static final class Edit {
        final String text;
        final int caret;

        Edit(String text, int caret) {
            this.text = text;
            this.caret = caret;
        }
    }

    private final JPopupMenu popup = new JPopupMenu();
    private final DefaultListModel<Suggestion> model = new DefaultListModel<>();
    private final JList<Suggestion> list = new JList<>(model);
    private List<String> allSkills = new ArrayList<>();
    private boolean updating;      // true while we change the text ourselves

    SkillAutoCompleteField() {
        // the popup must never take keyboard focus, or you could not keep typing
        popup.setFocusable(false);
        list.setFocusable(false);
        list.setFixedCellHeight(ROW_HEIGHT);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                Suggestion s = (Suggestion) value;
                JLabel label = (JLabel) super.getListCellRendererComponent(l, s.label, index, isSelected, false);
                label.setFont(Theme.font(s.isNew ? Font.BOLD : Font.PLAIN, 14));
                label.setForeground(s.isNew ? Theme.MAROON : Theme.TEXT_PRIMARY);
                label.setBackground(isSelected ? Theme.BRAND_50 : Color.WHITE);
                label.setBorder(new EmptyBorder(0, 12, 0, 12));
                label.setOpaque(true);
                return label;
            }
        });
        popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER_STRONG));
        popup.add(list);

        // typing -> refresh suggestions (after the edit has finished, hence invokeLater)
        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> updateSuggestions()); }
            @Override public void removeUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> updateSuggestions()); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });

        // click on the field -> load the newest skills; leave the field -> close the list
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                reloadSkills();
            }

            @Override
            public void focusLost(FocusEvent e) {
                hidePopup();
            }
        });

        // keyboard
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!popup.isVisible()) {
                    return;
                }
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        moveSelection(1);
                        e.consume();
                        break;
                    case KeyEvent.VK_UP:
                        moveSelection(-1);
                        e.consume();
                        break;
                    case KeyEvent.VK_ENTER:
                        if (list.getSelectedIndex() >= 0) {      // only if a row is highlighted
                            accept(list.getSelectedIndex());
                            e.consume();
                        }
                        break;
                    case KeyEvent.VK_ESCAPE:
                        hidePopup();
                        e.consume();
                        break;
                    default:
                        break;
                }
            }
        });

        // mouse: hover highlights, click picks
        list.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int i = list.locationToIndex(e.getPoint());
                if (i >= 0 && list.getCellBounds(i, i).contains(e.getPoint())) {
                    list.setSelectedIndex(i);
                }
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = list.locationToIndex(e.getPoint());
                if (i >= 0 && list.getCellBounds(i, i).contains(e.getPoint())) {
                    accept(i);
                }
            }
        });
    }

    // ------------------------------------------------------------ behaviour

    private void reloadSkills() {
        try {
            allSkills = DatabaseManager.getAllSkills();
        } catch (RuntimeException ignored) {
            // no connection right now: keep whatever list we already have
        }
    }

    private void updateSuggestions() {
        if (updating || !isFocusOwner()) {
            hidePopup();
            return;
        }
        List<Suggestion> found = computeSuggestions(getText(), getCaretPosition(), allSkills);
        model.clear();
        for (Suggestion s : found) {
            model.addElement(s);
        }
        if (found.isEmpty()) {
            hidePopup();
            return;
        }
        popup.setPopupSize(Math.max(getWidth(), 240), found.size() * ROW_HEIGHT + 2);
        if (!popup.isVisible()) {
            popup.show(this, 0, getHeight());
        }
        popup.revalidate();
        popup.repaint();
    }

    private void hidePopup() {
        if (popup.isVisible()) {
            popup.setVisible(false);
        }
    }

    private void moveSelection(int delta) {
        int n = model.size();
        if (n == 0) {
            return;
        }
        int i = list.getSelectedIndex() + delta;
        if (i < 0) {
            i = n - 1;
        } else if (i >= n) {
            i = 0;
        }
        list.setSelectedIndex(i);
    }

    private void accept(int index) {
        if (index < 0 || index >= model.size()) {
            return;
        }
        Edit edit = applySuggestion(getText(), getCaretPosition(), model.get(index).value);
        updating = true;
        try {
            setText(edit.text);
            setCaretPosition(edit.caret);
        } finally {
            updating = false;
        }
        hidePopup();
    }

    // ------------------------------------------------------------ matching rules (pure logic)

    /**
     * Suggestions for the word at the caret. "text" is the whole field, e.g. "Java, fig, SQL".
     * Skills already in the field are left out. Prefix matches come before "contains" matches.
     */
    static List<Suggestion> computeSuggestions(String text, int caret, List<String> allSkills) {
        List<Suggestion> out = new ArrayList<>();
        caret = Math.max(0, Math.min(caret, text.length()));
        int start = text.lastIndexOf(',', caret - 1) + 1;     // first char after the previous comma
        int end = text.indexOf(',', caret);                    // next comma (or end of text)
        if (end < 0) {
            end = text.length();
        }
        String token = text.substring(start, end).trim();
        if (token.isEmpty()) {
            return out;
        }

        // skills typed elsewhere in the field
        Set<String> already = new HashSet<>();
        for (String part : (text.substring(0, start) + "," + text.substring(end)).split(",")) {
            if (!part.trim().isEmpty()) {
                already.add(part.trim().toLowerCase());
            }
        }

        String q = token.toLowerCase();
        boolean exists = false;
        List<String> prefix = new ArrayList<>();
        List<String> inside = new ArrayList<>();
        for (String skill : allSkills) {
            String lower = skill.toLowerCase();
            if (lower.equals(q)) {
                exists = true;
            }
            if (already.contains(lower)) {
                continue;
            }
            if (lower.startsWith(q)) {
                prefix.add(skill);
            } else if (lower.contains(q)) {
                inside.add(skill);
            }
        }
        prefix.addAll(inside);
        for (int i = 0; i < Math.min(MAX_SHOWN, prefix.size()); i++) {
            out.add(new Suggestion(prefix.get(i), prefix.get(i), false));
        }
        if (!exists && !already.contains(q) && token.length() <= DatabaseManager.MAX_SKILL) {
            out.add(new Suggestion("+ Add \"" + token + "\" as a new skill", token, true));
        }
        return out;
    }

    /** Replaces the word at the caret with the chosen skill and adds ", " ready for the next one. */
    static Edit applySuggestion(String text, int caret, String value) {
        caret = Math.max(0, Math.min(caret, text.length()));
        int start = text.lastIndexOf(',', caret - 1) + 1;
        int end = text.indexOf(',', caret);
        if (end < 0) {
            end = text.length();
        }
        String before = text.substring(0, start);
        String after = text.substring(end);
        String head = before.isEmpty() ? "" : before + " ";
        if (after.isEmpty()) {
            return new Edit(head + value + ", ", (head + value).length() + 2);
        }
        return new Edit(head + value + after, (head + value).length());
    }
}