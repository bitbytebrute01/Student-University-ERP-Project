package com.university.erp.gui.theme;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.university.db.DatabaseManager;
import com.university.erp.security.SessionManager;
import com.university.erp.security.User;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ThemeManager {
    private static boolean isDarkMode = false;
    private static final String THEME_ROLE = "erp.theme.role";
    private static final String THEMED_ICON = "erp.theme.icon";
    private static final String THEMED_ICON_SIZE = "erp.theme.icon.size";
    private static final String ROLE_PAGE = "page";
    private static final String ROLE_CARD = "card";
    private static final String ROLE_HEADER = "header";
    private static final String ROLE_SIDEBAR = "sidebar";
    
    public static final Color ACCENT_BLUE = new Color(0, 115, 177);
    public static final Color SUCCESS_GREEN = new Color(46, 204, 113);
    public static final Color WARNING_ORANGE = new Color(230, 126, 34);
    public static final Color DANGER_RED = new Color(231, 76, 60);
    public static final Color BACKGROUND_LIGHT = new Color(243, 242, 239);
    public static final Color CARD_BORDER = new Color(224, 224, 224);

    private static final Color BACKGROUND_DARK = new Color(17, 21, 26);
    private static final Color SURFACE_DARK = new Color(29, 34, 41);
    private static final Color SURFACE_ALT_LIGHT = new Color(244, 247, 250);
    private static final Color SURFACE_ALT_DARK = new Color(38, 44, 52);
    private static final Color BORDER_DARK = new Color(62, 70, 82);
    private static final Color TEXT_PRIMARY_LIGHT = new Color(31, 38, 46);
    private static final Color TEXT_PRIMARY_DARK = new Color(232, 236, 241);
    private static final Color TEXT_SECONDARY_LIGHT = new Color(100, 108, 116);
    private static final Color TEXT_SECONDARY_DARK = new Color(166, 174, 184);

    public static void initialize() {
        applyInitialTheme();
    }

    private static void applyUiDefaults() {
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("CheckBox.arc", 12);
        UIManager.put("ProgressBar.arc", 12);
        UIManager.put("defaultFont", new Font("Inter", Font.PLAIN, 13));
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
        UIManager.put("TabbedPane.selectedBackground", surface());
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 12));
        UIManager.put("Table.rowHeight", 35);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.gridColor", border());
        UIManager.put("Panel.background", background());
        UIManager.put("Viewport.background", background());
        UIManager.put("ScrollPane.background", background());
        UIManager.put("Label.foreground", textPrimary());
        UIManager.put("Table.background", surface());
        UIManager.put("Table.foreground", textPrimary());
        UIManager.put("TableHeader.background", surfaceAlt());
        UIManager.put("TableHeader.foreground", textPrimary());
        UIManager.put("TextField.background", surface());
        UIManager.put("TextField.foreground", textPrimary());
        UIManager.put("TextArea.background", surface());
        UIManager.put("TextArea.foreground", textPrimary());
    }

    public static void loadUserTheme() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String sql = "SELECT theme FROM user_preferences WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                isDarkMode = "DARK".equalsIgnoreCase(rs.getString("theme"));
                applyInitialTheme();
                refreshOpenWindows();
            }
        } catch (SQLException e) {
            System.err.println("Error loading user theme: " + e.getMessage());
        }
    }

    private static void applyInitialTheme() {
        try {
            if (isDarkMode) {
                UIManager.setLookAndFeel(new FlatDarculaLaf());
            } else {
                UIManager.setLookAndFeel(new FlatIntelliJLaf());
            }
            applyUiDefaults();
        } catch (Exception e) {
            System.err.println("Theme Initialization Error: " + e.getMessage());
        }
    }

    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
        saveThemePreference();
        
        if (!GraphicsEnvironment.isHeadless()) {
            FlatAnimatedLafChange.showSnapshot();
        }
        applyInitialTheme();
        refreshOpenWindows();

        if (!GraphicsEnvironment.isHeadless()) {
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
        }
    }

    private static void saveThemePreference() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String theme = isDarkMode ? "DARK" : "LIGHT";
        String sql = "INSERT INTO user_preferences (username, theme) VALUES (?, ?) " +
                     "ON CONFLICT(username) DO UPDATE SET theme = excluded.theme";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, theme);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving user theme: " + e.getMessage());
        }
    }

    public static boolean isDarkMode() { return isDarkMode; }

    public static Color background() {
        return isDarkMode ? BACKGROUND_DARK : BACKGROUND_LIGHT;
    }

    public static Color surface() {
        return isDarkMode ? SURFACE_DARK : Color.WHITE;
    }

    public static Color surfaceAlt() {
        return isDarkMode ? SURFACE_ALT_DARK : SURFACE_ALT_LIGHT;
    }

    public static Color border() {
        return isDarkMode ? BORDER_DARK : CARD_BORDER;
    }

    public static Color textPrimary() {
        return isDarkMode ? TEXT_PRIMARY_DARK : TEXT_PRIMARY_LIGHT;
    }

    public static Color textSecondary() {
        return isDarkMode ? TEXT_SECONDARY_DARK : TEXT_SECONDARY_LIGHT;
    }

    public static Color sidebarBackground() {
        return isDarkMode ? new Color(12, 16, 21) : new Color(28, 32, 36);
    }

    public static void stylePage(JComponent component) {
        component.putClientProperty(THEME_ROLE, ROLE_PAGE);
        component.setOpaque(true);
        component.setBackground(background());
    }

    public static void styleCard(JComponent component) {
        component.putClientProperty(THEME_ROLE, ROLE_CARD);
        component.setOpaque(true);
        component.setBackground(surface());
        component.setBorder(cardBorder());
    }

    public static void styleHeader(JComponent component) {
        component.putClientProperty(THEME_ROLE, ROLE_HEADER);
        component.setOpaque(true);
        component.setBackground(surface());
        component.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border()));
    }

    public static void styleSidebar(JComponent component) {
        component.putClientProperty(THEME_ROLE, ROLE_SIDEBAR);
        component.setOpaque(true);
        component.setBackground(sidebarBackground());
    }

    public static void styleIconButton(AbstractButton button, Ikon icon, int size) {
        button.putClientProperty(THEMED_ICON, icon);
        button.putClientProperty(THEMED_ICON_SIZE, size);
        button.setForeground(textSecondary());
        button.setIcon(FontIcon.of(icon, size, textSecondary()));
    }
    
    public static JPanel createGlassCard() {
        JPanel card = new JPanel();
        styleCard(card);
        return card;
    }

    public static void refreshOpenWindows() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            refreshComponentTree(window);
            window.invalidate();
            window.validate();
            window.repaint();
        }
        FlatLaf.updateUI();
    }

    public static void refreshComponentTree(Component component) {
        applyTheme(component);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                refreshComponentTree(child);
            }
        }
    }

    private static void applyTheme(Component component) {
        if (component instanceof JScrollPane scrollPane) {
            scrollPane.setBackground(background());
            scrollPane.getViewport().setBackground(background());
        }

        if (component instanceof JViewport viewport) {
            Component view = viewport.getView();
            viewport.setBackground(view instanceof JTable ? surface() : background());
        }

        if (component instanceof JTable table) {
            table.setBackground(surface());
            table.setForeground(textPrimary());
            table.setGridColor(border());
            table.setSelectionBackground(isDarkMode ? new Color(20, 92, 143) : new Color(218, 235, 249));
            table.setSelectionForeground(isDarkMode ? Color.WHITE : textPrimary());
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                header.setBackground(surfaceAlt());
                header.setForeground(textPrimary());
                header.setBorder(BorderFactory.createLineBorder(border()));
            }
        }

        if (!(component instanceof JComponent jComponent)) {
            return;
        }

        Object role = jComponent.getClientProperty(THEME_ROLE);
        if (ROLE_PAGE.equals(role)) {
            jComponent.setOpaque(true);
            jComponent.setBackground(background());
        } else if (ROLE_CARD.equals(role)) {
            jComponent.setOpaque(true);
            jComponent.setBackground(surface());
            jComponent.setBorder(cardBorder());
        } else if (ROLE_HEADER.equals(role)) {
            jComponent.setOpaque(true);
            jComponent.setBackground(surface());
            jComponent.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border()));
        } else if (ROLE_SIDEBAR.equals(role)) {
            jComponent.setOpaque(true);
            jComponent.setBackground(sidebarBackground());
        } else if (component instanceof JPanel panel && panel.isOpaque()) {
            Color current = panel.getBackground();
            if (isThemedBackground(current)) {
                panel.setBackground(background());
            } else if (isThemedSurface(current)) {
                panel.setBackground(surface());
            }
        }

        if (component instanceof JLabel label) {
            applyLabelTheme(label);
        } else if (component instanceof JTextArea textArea) {
            textArea.setBackground(surface());
            textArea.setForeground(textPrimary());
            textArea.setCaretColor(textPrimary());
        } else if (component instanceof JTextField textField) {
            textField.setBackground(surface());
            textField.setForeground(textPrimary());
            textField.setCaretColor(textPrimary());
        } else if (component instanceof AbstractButton button) {
            applyButtonTheme(button);
        }
    }

    private static void applyLabelTheme(JLabel label) {
        if (label.isOpaque() && !isThemedSurface(label.getBackground()) && !isThemedBackground(label.getBackground())) {
            return;
        }

        Color foreground = label.getForeground();
        if (isPrimaryTextColor(foreground)) {
            label.setForeground(textPrimary());
        } else if (isSecondaryTextColor(foreground)) {
            label.setForeground(textSecondary());
        }
    }

    private static void applyButtonTheme(AbstractButton button) {
        Object icon = button.getClientProperty(THEMED_ICON);
        Object size = button.getClientProperty(THEMED_ICON_SIZE);
        if (icon instanceof Ikon ikon && size instanceof Integer iconSize) {
            button.setForeground(textSecondary());
            button.setIcon(FontIcon.of(ikon, iconSize, textSecondary()));
        }
    }

    private static javax.swing.border.Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border(), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        );
    }

    private static boolean isThemedBackground(Color color) {
        return sameColor(color, BACKGROUND_LIGHT) || sameColor(color, BACKGROUND_DARK);
    }

    private static boolean isThemedSurface(Color color) {
        return sameColor(color, Color.WHITE) || sameColor(color, SURFACE_DARK)
                || sameColor(color, SURFACE_ALT_LIGHT) || sameColor(color, SURFACE_ALT_DARK);
    }

    private static boolean isPrimaryTextColor(Color color) {
        return sameColor(color, Color.BLACK)
                || sameColor(color, TEXT_PRIMARY_LIGHT)
                || sameColor(color, TEXT_PRIMARY_DARK)
                || sameColor(color, new Color(38, 46, 56))
                || sameColor(color, new Color(31, 38, 46));
    }

    private static boolean isSecondaryTextColor(Color color) {
        if (color == null || sameColor(color, Color.WHITE) || isStatusOrAccentColor(color)) {
            return false;
        }
        if (sameColor(color, TEXT_SECONDARY_LIGHT) || sameColor(color, TEXT_SECONDARY_DARK)) {
            return true;
        }
        int max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
        int min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
        int average = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
        return max - min <= 40 && average >= 70 && average <= 205;
    }

    private static boolean isStatusOrAccentColor(Color color) {
        return sameColor(color, ACCENT_BLUE)
                || sameColor(color, SUCCESS_GREEN)
                || sameColor(color, WARNING_ORANGE)
                || sameColor(color, DANGER_RED);
    }

    private static boolean sameColor(Color a, Color b) {
        return a != null && b != null
                && a.getRed() == b.getRed()
                && a.getGreen() == b.getGreen()
                && a.getBlue() == b.getBlue();
    }
}
