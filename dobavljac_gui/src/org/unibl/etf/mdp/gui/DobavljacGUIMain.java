package org.unibl.etf.mdp.gui;

import javax.swing.*;

public class DobavljacGUIMain {
    public static void main(String[] args) {
    	
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

    	
        SwingUtilities.invokeLater(() -> {
            DobavljacLoginFrame login = new DobavljacLoginFrame();
            login.setVisible(true);
        });
    }
}
