package org.unibl.etf.mdp.socket;

import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.persistance.OfferPersistence;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

public class OfferSocket {
    public static void main(String[] args) {
        int port = Integer.parseInt(ConfigLoader.getProperty("socket.port"));
        String offersFolder = ConfigLoader.getProperty("offers.folder");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            AppLogger.getLogger().info("DobavljacServer pokrenut na portu: " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                AppLogger.getLogger().info("Klijent povezan: " + clientSocket);

                new Thread(() -> handleClient(clientSocket, offersFolder)).start();
            }
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.SEVERE, "Greška prilikom rada ServerSocket-a: ", e);
        }
    }

    private static void handleClient(Socket clientSocket, String offersFolder) {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            String request = in.readUTF(); 
            if ("GET_SUPPLIERS".equals(request)) {
                sendSuppliersData(out, offersFolder);
            } else {
                out.writeUTF("INVALID_REQUEST");
            }

        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška u komunikaciji sa klijentom: ", e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                AppLogger.getLogger().log(Level.WARNING, "Greška prilikom zatvaranja klijenta: ", e);
            }
        }
    }

    private static void sendSuppliersData(DataOutputStream out, String offersFolder) throws IOException {
        File folder = new File(offersFolder);

        if (!folder.exists() || !folder.isDirectory()) {
            out.writeUTF("NO_OFFERS");
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.startsWith("offers_") && name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            out.writeUTF("NO_OFFERS");
            return;
        }

        out.writeUTF("SUPPLIERS_LIST"); 
        for (File file : files) {
            String supplierName = file.getName().replace("offers_", "").replace(".txt", "");
            out.writeUTF(supplierName);

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.writeUTF(line); 
                }
            }
            out.writeUTF("END_BOOKS"); 
        }
        out.writeUTF("END_SUPPLIERS");
    }
}
