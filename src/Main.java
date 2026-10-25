import com.promatch.gui.LoginFrame;
import com.promatch.service.DatabaseManager;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Entry point: sets up the database, then opens the login window. */
public class Main {

    public static void main(String[] args) {
        try {
            // Same look on Windows, macOS and Linux
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // keep the default look if this fails
        }

        // Any unexpected error (for example a database problem) is shown instead of silently ignored
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            error.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Something went wrong:\n" + error.getMessage(),
                    "ProMatch", JOptionPane.ERROR_MESSAGE));
        });

        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Connect to the shared MySQL database, create the tables, add sample data if it is empty
                DatabaseManager.initializeDatabase();
                // 2. Open the app
                new LoginFrame().setVisible(true);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to the database.\n" + e.getMessage()
                                + "\n\nCheck: db.properties exists and is correct, you are online,"
                                + "\nand the MySQL driver jar is in the lib folder.",
                        "ProMatch", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
