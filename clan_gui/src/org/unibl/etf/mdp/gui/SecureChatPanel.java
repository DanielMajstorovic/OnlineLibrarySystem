package org.unibl.etf.mdp.gui;

import javax.net.ssl.*;
import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class SecureChatPanel extends JPanel {

    private static final Logger logger = AppLogger.getLogger();
    private static final String API_URL = ConfigLoader.getProperty("users.url");

    private JComboBox<String> comboRecipient; 
    private JTextArea txtChatHistory; 
    private JTextField txtMessage;   
    
    private String currentUser;
    private SSLSocket sslSocket;
    private PrintWriter out;
    private BufferedReader in;

    private static String truststorePath;
    private static String truststorePassword;
    private static String chatServerHost;
    private static int chatServerPort;

    private boolean noConversationYetShown = false;

    static {
        try {
            truststorePath = ConfigLoader.getProperty("ssl.keystore");
            truststorePassword = ConfigLoader.getProperty("ssl.keystore.password");
            chatServerHost = ConfigLoader.getProperty("chat.server.host");
            chatServerPort = Integer.parseInt(ConfigLoader.getProperty("chat.server.port"));

            System.setProperty("javax.net.ssl.trustStore", truststorePath);
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);

        } catch (Exception e) {
            logger.severe("Greška pri učitavanju SSL konfiguracije: " + e.getMessage());
        }
    }

    public SecureChatPanel(String currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout());


        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboRecipient = new JComboBox<>();
        JButton btnConnect = new JButton("Poveži se");

        topPanel.add(new JLabel("Sagovornik:"));
        topPanel.add(comboRecipient);
        topPanel.add(btnConnect);


        txtChatHistory = new JTextArea();
        txtChatHistory.setEditable(false);
        JScrollPane scrollChat = new JScrollPane(txtChatHistory);

        
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        txtMessage = new JTextField();
        JButton btnSend = new JButton("Pošalji");
        bottomPanel.add(txtMessage, BorderLayout.CENTER);
        bottomPanel.add(btnSend, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);


        loadAvailableUsers();

     
        btnConnect.addActionListener(e -> {
            String recipient = (String) comboRecipient.getSelectedItem();
            if (recipient != null && !recipient.isEmpty()) {
                connectToChatServer(recipient);
            }
        });

      
        btnSend.addActionListener(e -> sendMessage());
    }


    private void loadAvailableUsers() {
        new Thread(() -> {
            try {
                
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() != 200) {
                    throw new IOException("HTTP greška: " + conn.getResponseCode());
                }

                
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();
                conn.disconnect();

                
                JSONArray usersArray = new JSONArray(response.toString());

                
                SwingUtilities.invokeLater(() -> {
                    comboRecipient.removeAllItems();
                    boolean foundUsers = false;

                    for (int i = 0; i < usersArray.length(); i++) {
                        JSONObject user = usersArray.getJSONObject(i);
                        if (user.has("approved") && user.getBoolean("approved")) {
                            String username = user.getString("username");

                            
                            if (!username.equals(currentUser)) {
                                comboRecipient.addItem(username);
                                foundUsers = true;
                            }
                        }
                    }

                    if (!foundUsers) {
                        comboRecipient.addItem("Nema dostupnih korisnika");
                        comboRecipient.setEnabled(false);
                    } else {
                        comboRecipient.setEnabled(true);
                    }
                });

            } catch (Exception e) {
                logger.severe("Greška pri učitavanju korisnika: " + e.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Greška pri učitavanju liste korisnika!",
                        "Greška", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void connectToChatServer(String recipient) {
        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) factory.createSocket(chatServerHost, chatServerPort);

            out = new PrintWriter(sslSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));

            
            out.println("USER:" + currentUser);
            out.println("CHAT_WITH:" + recipient);

           
            txtChatHistory.setText("");
            noConversationYetShown = false;

            
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if ("NO_CONVERSATION_YET".equals(line)) {
                            txtChatHistory.setText("Nema prethodnih poruka sa ovim korisnikom.\n");
                            noConversationYetShown = true;
                        } else {
                            if (noConversationYetShown) {
                                txtChatHistory.setText("");
                                noConversationYetShown = false;
                            }
                            txtChatHistory.append(line + "\n");
                        }
                    }
                } catch (IOException ex) {
                    logger.severe("Greška u čitanju chat poruka: " + ex.getMessage());
                }
            }).start();

        } catch (Exception ex) {
            logger.severe("Neuspješno povezivanje na secure chat server: " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Greška prilikom konekcije na chat server.",
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        if (sslSocket == null || sslSocket.isClosed()) {
            JOptionPane.showMessageDialog(this,
                "Niste povezani na chat server!",
                "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String msg = txtMessage.getText().trim();
        if (!msg.isEmpty()) {
            out.println(msg); 
            txtMessage.setText("");
        }
    }
}
