package org.unibl.etf.mdp.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;
import javax.swing.*;
import org.unibl.etf.mdp.data.ClanData;

public class AddMemberDialog extends JDialog {

    private JTextField txtUsername, txtFirstName, txtLastName, txtEmail, txtAddress, txtPassword;
    private JCheckBox chkApproved;
    private boolean confirmed = false;

    public AddMemberDialog(Window parent) {
        super(parent, "Dodaj novog člana", ModalityType.APPLICATION_MODAL);
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout(10, 10));

        JPanel centerPanel = new JPanel(new GridLayout(7, 2, 5, 5));
        centerPanel.add(new JLabel("Username:"));
        txtUsername = new JTextField();
        centerPanel.add(txtUsername);

        centerPanel.add(new JLabel("First Name:"));
        txtFirstName = new JTextField();
        centerPanel.add(txtFirstName);

        centerPanel.add(new JLabel("Last Name:"));
        txtLastName = new JTextField();
        centerPanel.add(txtLastName);

        centerPanel.add(new JLabel("Email:"));
        txtEmail = new JTextField();
        centerPanel.add(txtEmail);

        centerPanel.add(new JLabel("Address:"));
        txtAddress = new JTextField();
        centerPanel.add(txtAddress);

        centerPanel.add(new JLabel("Password:"));
        txtPassword = new JTextField();
        centerPanel.add(txtPassword);

        centerPanel.add(new JLabel("Approved:"));
        chkApproved = new JCheckBox();
        centerPanel.add(chkApproved);

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

    public ClanData getNewClanData() {
        ClanData clan = new ClanData();
        clan.setUsername(txtUsername.getText().trim());
        clan.setFirstName(txtFirstName.getText().trim());
        clan.setLastName(txtLastName.getText().trim());
        clan.setEmail(txtEmail.getText().trim());
        clan.setAddress(txtAddress.getText().trim());
        clan.setPassword(txtPassword.getText().trim());
        clan.setApproved(chkApproved.isSelected());
        return clan;
    }

    private boolean validateFields() {
        if (txtUsername.getText().trim().isEmpty() ||
            txtFirstName.getText().trim().isEmpty() ||
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
