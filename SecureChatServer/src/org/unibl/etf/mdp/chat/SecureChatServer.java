package org.unibl.etf.mdp.chat;

import javax.net.ssl.*;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.config.ConfigLoader;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SecureChatServer {
    
    private static final Logger logger = AppLogger.getLogger();

    private static int PORT;
    private static String keystorePath;
    private static String keystorePassword;
    
   
    private static final Map<String, PrintWriter> connectedClients = new ConcurrentHashMap<>();

  
    private static final File CONVERSATIONS_DIR = new File(ConfigLoader.getProperty("chat.history.dir"));
    
    public static void main(String[] args) {
        loadConfiguration();
        configureSSL();

        
        if (!CONVERSATIONS_DIR.exists()) {
            CONVERSATIONS_DIR.mkdir();
        }

        try {
            SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) ssf.createServerSocket(PORT);

            logger.info("SSL Chat Server pokrenut na portu " + PORT);

            while (true) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (Exception e) {
            logger.severe("Greška na serveru: " + e.getMessage());
        }
    }

    private static void loadConfiguration() {
        try {
            PORT = Integer.parseInt(ConfigLoader.getProperty("chat.server.port"));
            keystorePath = ConfigLoader.getProperty("ssl.keystore");
            keystorePassword = ConfigLoader.getProperty("ssl.keystore.password");
        } catch (Exception e) {
            logger.severe("Greška pri učitavanju konfiguracije: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void configureSSL() {
        System.setProperty("javax.net.ssl.keyStore", keystorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
    }


    private static File getConversationFile(String user1, String user2) {
        String[] arr = new String[] { user1, user2 };
        Arrays.sort(arr);
        String fileName = arr[0] + "_" + arr[1] + ".chat";
        return new File(CONVERSATIONS_DIR, fileName);
    }


    private static void sendConversationHistory(String user1, String user2, PrintWriter out) {
        File convoFile = getConversationFile(user1, user2);
        if (!convoFile.exists() || convoFile.length() == 0) {
            out.println("NO_CONVERSATION_YET");
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(convoFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException e) {
            logger.warning("Greška pri čitanju istorije razgovora: " + e.getMessage());
        }
    }


    private static void appendMessageToConversation(String user1, String user2, String message) {
        File convoFile = getConversationFile(user1, user2);
        try (PrintWriter pw = new PrintWriter(new FileWriter(convoFile, true))) {
            pw.println(message);
        } catch (IOException e) {
            logger.warning("Neuspješno snimanje poruke: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private SSLSocket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private String chatWith;

        public ClientHandler(SSLSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String userLine = in.readLine();
                String chatWithLine = in.readLine();

                if (userLine == null || chatWithLine == null 
                        || !userLine.startsWith("USER:") 
                        || !chatWithLine.startsWith("CHAT_WITH:")) {
                    logger.warning("Neispravan handshake podataka! Klijent nije poslao validne podatke.");
                    socket.close();
                    return;
                }

                username = userLine.replace("USER:", "").trim();
                chatWith = chatWithLine.replace("CHAT_WITH:", "").trim();

                connectedClients.put(username, out);
                logger.info(username + " povezan, razgovara sa " + chatWith);

                sendConversationHistory(username, chatWith, out);

                String msg;
                while ((msg = in.readLine()) != null) {
                    String formattedMsg = "[" + username + "]: " + msg;

                    appendMessageToConversation(username, chatWith, formattedMsg);

                    PrintWriter recipientOut = connectedClients.get(chatWith);
                    if (recipientOut != null) {
                        recipientOut.println(formattedMsg);
                    } else {
                        logger.info("Sagovornik " + chatWith + " nije online.");
                    }

                    out.println(formattedMsg);
                }
            } catch (IOException e) {
                logger.warning("Konekcija sa " + username + " prekinuta: " + e.getMessage());
            } finally {
                try {
                    if (username != null) {
                        connectedClients.remove(username);
                    }
                    socket.close();
                } catch (IOException e) {
                    logger.warning("Greška pri zatvaranju konekcije: " + e.getMessage());
                }
            }
        }
    }
}
