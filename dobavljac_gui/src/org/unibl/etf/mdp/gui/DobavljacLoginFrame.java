package org.unibl.etf.mdp.gui;

import javax.swing.*;

import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.logging.Level;

public class DobavljacLoginFrame extends JFrame {
    private JTextField tfUsername;
    private JButton btnLogin;

    public DobavljacLoginFrame() {
        super("Dobavljač - Login");
        initGUI();
    }

    private void initGUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(300, 150);
        setLocationRelativeTo(null);

        tfUsername = new JTextField(15);
        btnLogin = new JButton("Login");

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 1));
        panel.add(new JLabel("Unesite svoje ime:"));
        panel.add(tfUsername);

        add(panel, BorderLayout.CENTER);
        add(btnLogin, BorderLayout.SOUTH);

        btnLogin.addActionListener(e -> onLogin());
    }

    private void onLogin() {
        String username = tfUsername.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username ne može biti prazan!");
            return;
        }

        try {
            String host = ConfigLoader.getProperty("server.host");
            int port = Integer.parseInt(ConfigLoader.getProperty("server.port"));
            Socket socket = new Socket(host, port);

                AppLogger.getLogger().info("Dobavljač [" + username + "] logovan.");

                DobavljacMainFrame mainFrame = new DobavljacMainFrame(socket, username);
                mainFrame.setVisible(true);
                this.dispose(); 

        } catch (IOException ex) {
            AppLogger.getLogger().log(Level.WARNING, "Greška pri loginu: ", ex);
            JOptionPane.showMessageDialog(this, "Greška pri povezivanju na server.");
        }
    }
}

