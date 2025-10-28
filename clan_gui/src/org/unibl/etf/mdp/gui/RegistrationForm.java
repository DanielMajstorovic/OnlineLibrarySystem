package org.unibl.etf.mdp.gui;

import javax.swing.*;
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
import org.unibl.etf.mdp.config.ConfigLoader;

public class RegistrationForm extends JFrame {

    private static final Logger logger = Logger.getLogger(RegistrationForm.class.getName());

    private JTextField txtFirstName, txtLastName, txtAddress, txtEmail, txtUsername;
    private JPasswordField txtPassword, txtPasswordConfirm;
    private JButton btnSubmit;

    public RegistrationForm() {
        setTitle("Registracija");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initComponents();
    }

    private void initComponents() {
        JPanel panel = new JPanel(new GridLayout(8, 2));

        panel.add(new JLabel("Ime:"));
        txtFirstName = new JTextField();
        panel.add(txtFirstName);

        panel.add(new JLabel("Prezime:"));
        txtLastName = new JTextField();
        panel.add(txtLastName);

        panel.add(new JLabel("Adresa:"));
        txtAddress = new JTextField();
        panel.add(txtAddress);

        panel.add(new JLabel("Email:"));
        txtEmail = new JTextField();
        panel.add(txtEmail);

        panel.add(new JLabel("Korisničko ime:"));
        txtUsername = new JTextField();
        panel.add(txtUsername);

        panel.add(new JLabel("Lozinka:"));
        txtPassword = new JPasswordField();
        panel.add(txtPassword);

        panel.add(new JLabel("Ponovi lozinku:"));
        txtPasswordConfirm = new JPasswordField();
        panel.add(txtPasswordConfirm);

        btnSubmit = new JButton("Registruj se");
        panel.add(btnSubmit);
        panel.add(new JLabel(""));

        getContentPane().add(panel);
        initActions();
    }

    private void initActions() {
        btnSubmit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    registerUser();
                } catch (Exception ex) {
                    logger.severe("Greška prilikom registracije: " + ex.getMessage());
                    JOptionPane.showMessageDialog(RegistrationForm.this,
                            "Došlo je do greške prilikom registracije.",
                            "Greška", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void registerUser() throws Exception {
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String address = txtAddress.getText().trim();
        String email = txtEmail.getText().trim();
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());
        String passwordConfirm = new String(txtPasswordConfirm.getPassword());

        if (firstName.isEmpty() || lastName.isEmpty() || address.isEmpty() ||
            email.isEmpty() || username.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Molimo popunite sva polja.",
                    "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            JOptionPane.showMessageDialog(this,
                    "Email nije u ispravnom formatu.",
                    "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!password.equals(passwordConfirm)) {
            JOptionPane.showMessageDialog(this,
                    "Lozinke se ne podudaraju!",
                    "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String json = "{\"firstName\":\"" + firstName + "\","
                    + "\"lastName\":\"" + lastName + "\","
                    + "\"address\":\"" + address + "\","
                    + "\"email\":\"" + email + "\","
                    + "\"username\":\"" + username + "\","
                    + "\"password\":\"" + password + "\"}";

        String registerUrl = ConfigLoader.getProperty("register.url");

        if (registerUrl == null || registerUrl.isEmpty()) {
            throw new Exception("Konfiguracija 'register.url' nije pronađena.");
        }

        HttpURLConnection conn = (HttpURLConnection) new java.net.URL(registerUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int status = conn.getResponseCode();

        if (status == 200) {
            JOptionPane.showMessageDialog(this,
                    "<html><b>Registracija uspješna!</b><br><i>Možete se ulogovati kada Vam se odobri nalog!</i></html>",
                    "Informacija", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else if (status == 409) {
            JOptionPane.showMessageDialog(this,
                    "<html><b>Registracija nije uspjela.</b><br><i>Korisničko ime već postoji.</i></html>",
                    "Greška", JOptionPane.ERROR_MESSAGE);
        } else {
            StringBuilder sb = new StringBuilder();
            try (InputStream es = conn.getErrorStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line.trim());
                }
            }
            String responseMsg = sb.toString();

            JOptionPane.showMessageDialog(this,
                    "<html><b>Došlo je do greške.</b><br><i>" + responseMsg + "</i></html>",
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }
}
