package org.unibl.etf.mdp.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;
import javax.swing.*;
import org.unibl.etf.mdp.data.ClanData;

public class UpdateMemberDialog extends JDialog {

    private JTextField txtUsername, txtFirstName, txtLastName, txtEmail, txtAddress, txtPassword;
    private boolean confirmed = false;
    private ClanData originalClan;

    public UpdateMemberDialog(Window parent, ClanData clan) {
        super(parent, "Ažuriraj podatke člana", ModalityType.APPLICATION_MODAL);
        this.originalClan = clan;

        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel centerPanel = new JPanel(new GridLayout(6, 2, 5, 5));

        centerPanel.add(new JLabel("Username:"));
        txtUsername = new JTextField(clan.getUsername());
        txtUsername.setEditable(false);
        centerPanel.add(txtUsername);

        centerPanel.add(new JLabel("First Name:"));
        txtFirstName = new JTextField(clan.getFirstName());
        centerPanel.add(txtFirstName);

        centerPanel.add(new JLabel("Last Name:"));
        txtLastName = new JTextField(clan.getLastName());
        centerPanel.add(txtLastName);

        centerPanel.add(new JLabel("Email:"));
        txtEmail = new JTextField(clan.getEmail());
        centerPanel.add(txtEmail);

        centerPanel.add(new JLabel("Address:"));
        txtAddress = new JTextField(clan.getAddress());
        centerPanel.add(txtAddress);

        centerPanel.add(new JLabel("Password:"));
        txtPassword = new JTextField(clan.getPassword());
        centerPanel.add(txtPassword);

        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Sačuvaj");
        JButton btnCancel = new JButton("Odustani");

        bottomPanel.add(btnOk);
        bottomPanel.add(btnCancel);
        add(bottomPanel, BorderLayout.SOUTH);

        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateFields()) {
                    confirmed = true;
                    setVisible(false);
                }
            }
        });

        btnCancel.addActionListener(e -> {
            confirmed = false;
            setVisible(false);
        });
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public ClanData getUpdatedClanData() {
        ClanData updated = new ClanData();
        updated.setUsername(originalClan.getUsername()); 

        updated.setFirstName(txtFirstName.getText().trim());
        updated.setLastName(txtLastName.getText().trim());
        updated.setEmail(txtEmail.getText().trim());
        updated.setAddress(txtAddress.getText().trim());
        updated.setPassword(txtPassword.getText().trim());

        return updated;
    }

    private boolean validateFields() {
        if (txtFirstName.getText().trim().isEmpty() ||
            txtLastName.getText().trim().isEmpty() ||
            txtEmail.getText().trim().isEmpty() ||
            txtAddress.getText().trim().isEmpty() ||
            txtPassword.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(this, "Sva polja su obavezna!", "Greška", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!isValidEmail(txtEmail.getText().trim())) {
            JOptionPane.showMessageDialog(this, "Neispravan format email adrese!", "Greška", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }
}
