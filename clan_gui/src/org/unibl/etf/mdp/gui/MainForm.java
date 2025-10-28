package org.unibl.etf.mdp.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Logger;

public class MainForm extends JFrame {
    
    private static final Logger logger = Logger.getLogger(MainForm.class.getName());

    private MulticastPanel multicastPanel;
    private SecureChatPanel chatPanel;
    
    public MainForm(String loggedInUser) {
        setTitle("Online Biblioteka - GUI clan (" + loggedInUser + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        
        multicastPanel = new MulticastPanel(loggedInUser);
        chatPanel = new SecureChatPanel(loggedInUser);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Multicast poruke", multicastPanel);
        tabbedPane.addTab("Privatni chat", chatPanel);
        
        MemberBooksPanel memberBooksPanel = new MemberBooksPanel(loggedInUser);
        tabbedPane.add("Knjige",memberBooksPanel);

        
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }
    
}
