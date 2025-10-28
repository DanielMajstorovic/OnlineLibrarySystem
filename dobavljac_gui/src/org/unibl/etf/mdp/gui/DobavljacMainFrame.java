package org.unibl.etf.mdp.gui;

import javax.swing.*;

import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.persistance.OfferPersistence;

import java.awt.*;
import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;

public class DobavljacMainFrame extends JFrame {

    private Socket socket;
    private String supplierName;

    private JTabbedPane tabbedPane;

    private AllBooksPanel allBooksPanel;
    private MyOfferPanel myOfferPanel;
    private SupplierOrdersPanel supplierOrdersPanel;

    private Set<String> myOffers;

    public DobavljacMainFrame(Socket socket, String supplierName) {
        super("Dobavljač GUI - " + supplierName);
        this.socket = socket;
        this.supplierName = supplierName;
        
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onLogout(); 
            }
        });
        
        myOffers = OfferPersistence.loadOffersForSupplier(supplierName);
        initGUI();
    }
    
    private void onLogout() {
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            out.writeBytes("END\n");
            out.flush();
            AppLogger.getLogger().info("END poslan serveru za korisnika: " + supplierName);
        } catch (IOException ex) {
            AppLogger.getLogger().warning("Greška prilikom slanja END komande: " + ex.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                AppLogger.getLogger().warning("Greška prilikom zatvaranja socket-a: " + ex.getMessage());
            }
            dispose(); 
            System.exit(0); 
        }
    }

    private void initGUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        tabbedPane = new JTabbedPane();

        allBooksPanel = new AllBooksPanel(socket, supplierName, myOffers);
        myOfferPanel = new MyOfferPanel(supplierName);
        
        supplierOrdersPanel = new SupplierOrdersPanel(supplierName);

        tabbedPane.addTab("All Books", allBooksPanel);
        tabbedPane.addTab("My Offer", myOfferPanel);
        tabbedPane.addTab("Orders", supplierOrdersPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    public void dispose() {
        OfferPersistence.saveOffersForSupplier(supplierName, myOffers);
        super.dispose();
    }
}
