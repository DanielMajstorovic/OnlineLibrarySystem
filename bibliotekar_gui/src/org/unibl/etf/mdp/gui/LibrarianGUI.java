package org.unibl.etf.mdp.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class LibrarianGUI extends JFrame {

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
            new LibrarianGUI().setVisible(true);
        });
    }

    public LibrarianGUI() {
        super("Online biblioteka - GUI bibliotekar");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        MembersPanel membersPanel = new MembersPanel();
        tabbedPane.addTab("Članovi", membersPanel);

        MulticastPanel multicastPanel = new MulticastPanel("Bibliotekar");
        tabbedPane.addTab("Multicast poruke", multicastPanel);
        
        SuppliersPanel suppliersPanel = new SuppliersPanel();
        tabbedPane.addTab("Dobavljaci", suppliersPanel);
        
        BooksPanel booksCrudPanel = new BooksPanel();
        tabbedPane.add("Upravljanje knjigama",booksCrudPanel);
        
        ReservationsPanel reservationsPanel = new ReservationsPanel();
        tabbedPane.add("Rezervacije knjiga",reservationsPanel);
        

        add(tabbedPane, BorderLayout.CENTER);
    }
}
