import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

public class DigitalLockerApp extends JFrame {
    private JTextField folderField;
    private JPasswordField passwordField;
    private JButton selectFolderButton, lockButton, unlockButton;
    private File selectedFolder;
    private final String passwordFile = "locker_password.hash";

    public DigitalLockerApp() {
        setTitle("Digital Locker");
        setSize(500, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1));

        JPanel passPanel = new JPanel();
        passPanel.add(new JLabel("Enter Password: "));
        passwordField = new JPasswordField(20);
        passPanel.add(passwordField);
        add(passPanel);

        JPanel folderPanel = new JPanel();
        folderField = new JTextField(25);
        folderField.setEditable(false);
        selectFolderButton = new JButton("Select Folder");
        selectFolderButton.addActionListener(e -> chooseFolder());
        folderPanel.add(folderField);
        folderPanel.add(selectFolderButton);
        add(folderPanel);

        JPanel actionPanel = new JPanel();
        lockButton = new JButton("Lock Folder");
        unlockButton = new JButton("Unlock Folder");

        lockButton.addActionListener(e -> lockFolder());
        unlockButton.addActionListener(e -> unlockFolder());

        actionPanel.add(lockButton);
        actionPanel.add(unlockButton);
        add(actionPanel);
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            selectedFolder = chooser.getSelectedFile();
            folderField.setText(selectedFolder.getAbsolutePath());
        }
    }

    private void lockFolder() {
        if (!validateInput()) return;

        String password = new String(passwordField.getPassword());

        try {
            savePasswordHash(password);
            byte[] key = getAESKeyFromPassword(password);

            File[] files = selectedFolder.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.isFile()) {
                    encryptFile(file, key);
                }
            }

            JOptionPane.showMessageDialog(this, "Folder locked (encrypted) successfully.");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Encryption failed: " + ex.getMessage());
        }
    }

    private void unlockFolder() {
        if (!validateInput()) return;

        String password = new String(passwordField.getPassword());

        try {
            if (!checkPassword(password)) {
                JOptionPane.showMessageDialog(this, "Incorrect password.");
                return;
            }

            byte[] key = getAESKeyFromPassword(password);

            File[] files = selectedFolder.listFiles();
            if (files == null) return;

            for (File file : files) {
                if (file.isFile()) {
                    decryptFile(file, key);
                }
            }

            JOptionPane.showMessageDialog(this, "Folder unlocked (decrypted) successfully.");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Decryption failed: " + ex.getMessage());
        }
    }

    private boolean validateInput() {
        if (selectedFolder == null) {
            JOptionPane.showMessageDialog(this, "Please select a folder.");
            return false;
        }

        if (passwordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this, "Please enter a password.");
            return false;
        }

        return true;
    }

    private void encryptFile(File inputFile, byte[] key) throws Exception {
        processFile(Cipher.ENCRYPT_MODE, inputFile, key);
    }

    private void decryptFile(File inputFile, byte[] key) throws Exception {
        processFile(Cipher.DECRYPT_MODE, inputFile, key);
    }

    private void processFile(int mode, File inputFile, byte[] key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(mode, secretKey);

        byte[] inputBytes = Files.readAllBytes(inputFile.toPath());
        byte[] outputBytes = cipher.doFinal(inputBytes);

        Files.write(inputFile.toPath(), outputBytes);
    }

    private byte[] getAESKeyFromPassword(String password) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = sha.digest(password.getBytes("UTF-8"));
        return Arrays.copyOf(key, 16); // AES key = 16 bytes
    }

    private void savePasswordHash(String password) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(password.getBytes("UTF-8"));
        Files.write(Paths.get(passwordFile), hash);
    }

    private boolean checkPassword(String input) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hashInput = sha.digest(input.getBytes("UTF-8"));
        byte[] storedHash = Files.readAllBytes(Paths.get(passwordFile));
        return Arrays.equals(hashInput, storedHash);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DigitalLockerApp app = new DigitalLockerApp();
            app.setVisible(true);
        });
    }
}
