package org.unibl.etf.mdp.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.json.JSONObject; // Dodan import za JSON parsiranje

public class LoginForm extends JFrame {
    private static final long serialVersionUID = -8592228909150380982L;
    private static final Logger logger = AppLogger.getLogger();

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnRegister;

    public LoginForm() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Ako Nimbus nije dostupan, zadrži defaultni Look & Feel
        }

        setTitle("Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout(0, 20));
        contentPane.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(contentPane);

        JLabel lblTitle = new JLabel("Prijava");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        contentPane.add(lblTitle, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Korisničko ime:"), gbc);

        gbc.gridx = 1;
        txtUsername = new JTextField(15);
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Lozinka:"), gbc);

        gbc.gridx = 1;
        txtPassword = new JPasswordField(15);
        formPanel.add(txtPassword, gbc);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnLogin = new JButton("Prijava");
        btnRegister = new JButton("Registracija");
        buttonsPanel.add(btnLogin);
        buttonsPanel.add(btnRegister);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(formPanel);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(buttonsPanel);

        contentPane.add(centerPanel, BorderLayout.CENTER);

        initActions();
    }

    private void initActions() {
        btnLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    login();
                } catch (Exception ex) {
                    logger.severe("Greška prilikom prijave: " + ex.getMessage());
                    JOptionPane.showMessageDialog(LoginForm.this, 
                        "Desila se greška pri prijavi. Detalji u logu.",
                        "Greška", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RegistrationForm regForm = new RegistrationForm();
                regForm.setVisible(true);
            }
        });
    }

    private void login() throws Exception {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        String jsonRequest = "{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}";

        HttpURLConnection conn = (HttpURLConnection) new java.net.URL(ConfigLoader.getProperty("login.url")).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int status = conn.getResponseCode();
        String responseMsg = null;

        if (status == 200) {
            StringBuilder sb = new StringBuilder();
            try (InputStream is = conn.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line.trim());
                }
            }
            String responseJson = sb.toString();
            logger.info("Uspješna prijava! Odgovor servera: " + responseJson);

            JSONObject jsonObj = new JSONObject(responseJson);
            responseMsg = jsonObj.optString("message", "Prijava je uspješna.");

             MainForm mainForm = new MainForm(username);
             mainForm.setVisible(true);
            this.dispose();
        } else if (status == 401) {
            StringBuilder sb = new StringBuilder();
            try (InputStream es = conn.getErrorStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line.trim());
                }
            }
            String responseJson = sb.toString();

            JSONObject jsonObj = new JSONObject(responseJson);
            responseMsg = jsonObj.optString("message", "Neispravni kredencijali.");
            JOptionPane.showMessageDialog(this, responseMsg, "Greška", JOptionPane.ERROR_MESSAGE);
        } else if (status == 403) {
        	responseMsg = "<html><b>Korisnik je \"disabled\"!</b><br><i>Nalog je tek kreiran (nije odobren), ili je blokiran!.</i></html>";
            //logger.warning("Neuspješna prijava. Status: " + status + ". " + responseMsg);
            JOptionPane.showMessageDialog(this, responseMsg, "Greška", JOptionPane.ERROR_MESSAGE);
        } else {
            StringBuilder sb = new StringBuilder();
            try (InputStream es = conn.getErrorStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line.trim());
                }
            }
            String responseJson = sb.toString();
            //logger.warning("Neuspješna prijava. Status: " + status + ". Poruka: " + responseJson);
            JOptionPane.showMessageDialog(this, "Došlo je do greške: " + responseJson, "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

}
