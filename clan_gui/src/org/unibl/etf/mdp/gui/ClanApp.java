package org.unibl.etf.mdp.gui;

public class ClanApp {
	
    public static void main(String[] args) {

        javax.swing.SwingUtilities.invokeLater(() -> {
            LoginForm loginForm = new LoginForm();
            loginForm.setVisible(true);
        });
    }
}
