package org.unibl.etf.mdp.gui;

import javax.swing.*;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.config.ConfigLoader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import java.io.*;
import java.util.logging.Logger;

public class MulticastPanel extends JPanel {
    
    private static final Logger logger = AppLogger.getLogger();
    private JTextArea txtReceived;  
    private JTextField txtMessage;  
    
    private String currentUser;
    
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    
    public MulticastPanel(String currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout());

        txtReceived = new JTextArea();
        txtReceived.setEditable(false);
        JScrollPane scrollReceived = new JScrollPane(txtReceived);
        
        JPanel panelSend = new JPanel(new BorderLayout(5, 5));
        txtMessage = new JTextField();
        JButton btnSend = new JButton("Pošalji svima");
        panelSend.add(txtMessage, BorderLayout.CENTER);
        panelSend.add(btnSend, BorderLayout.EAST);
        
        add(scrollReceived, BorderLayout.CENTER);
        add(panelSend, BorderLayout.SOUTH);
        
        initMulticastSocket();
        
        startListeningThread();
        
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = txtMessage.getText().trim();
                if (!msg.isEmpty()) {
                    sendMulticastMessage(msg);
                    txtMessage.setText("");
                }
            }
        });
    }
    
    private void initMulticastSocket() {
        try {
            String multicastAddress = ConfigLoader.getProperty("multicast.address");
            port = Integer.parseInt(ConfigLoader.getProperty("multicast.port"));

            if (multicastAddress == null || multicastAddress.isEmpty()) {
                throw new IllegalArgumentException("Nedostaje multicast adresa u konfiguraciji!");
            }

            socket = new MulticastSocket(port);
            group = InetAddress.getByName(multicastAddress);
            socket.joinGroup(group);

            logger.info("Multicast socket pokrenut na adresi " + multicastAddress + " i portu " + port);
        } catch (Exception e) {
            logger.severe("Greška prilikom inicijalizacije multicast socketa: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Greška pri inicijalizaciji multicast-a. Provjerite konfiguraciju.",
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void startListeningThread() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    String receivedMsg = new String(packet.getData(), 0, packet.getLength());
                    
                    SwingUtilities.invokeLater(() -> {
                        txtReceived.append(receivedMsg + "\n");
                    });

                    logger.info("Primljena multicast poruka: " + receivedMsg);

                } catch (IOException e) {
                    logger.severe("Greška prilikom primanja multicast poruke: " + e.getMessage());
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
    
    private void sendMulticastMessage(String message) {
        try {
            String fullMsg = "[" + currentUser + "]: " + message;
            byte[] buffer = fullMsg.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
            socket.send(packet);
            
            logger.info("Poslana multicast poruka: " + fullMsg);
        } catch (IOException e) {
            logger.severe("Greška prilikom slanja multicast poruke: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Greška pri slanju poruke!",
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }
}
