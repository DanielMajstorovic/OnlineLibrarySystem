package org.unibl.etf.mdp.gui;

import javax.swing.*;
import javax.swing.event.*;

import org.unibl.etf.mdp.persistance.OfferPersistence;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

public class MyOfferPanel extends JPanel {

    private JList<String> offerList;
    private DefaultListModel<String> listModel;
    private JButton refreshButton;

    private String supplierName;
    
    public MyOfferPanel(String supplierName) {
        this.supplierName = supplierName;
        initGUI();
        
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                refreshList(); 
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {}

            @Override
            public void ancestorMoved(AncestorEvent event) {}
        });
    }

    private void initGUI() {
        setLayout(new BorderLayout());

        listModel = new DefaultListModel<>();
        offerList = new JList<>(listModel);
        add(new JScrollPane(offerList), BorderLayout.CENTER);


        refreshList();
    }

    private void refreshList() {
        listModel.clear();
        for (String title : OfferPersistence.loadOffersForSupplier(supplierName)) {
            listModel.addElement(title);
        }
    }
}
