package org.unibl.etf.mdp.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.unibl.etf.mdp.bookutls.BookLoader;
import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.model.BookData;

public class SupplierServer {

    private static final int PORT = Integer.parseInt(ConfigLoader.getProperty("SERVER_PORT"));

    public static List<BookData> loadBooks() {
        return BookLoader.loadAllBooks();
    }

    public static void main(String[] args) {
        AppLogger.getLogger().log(Level.INFO, "SupplierServerMain starting on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            AppLogger.getLogger().info("Dobavljač server je startao. Čeka klijente na portu: " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                AppLogger.getLogger().info("Novi klijent se povezao: " + client);
                new SupplierClientHandler(client).start();
            }
        } catch (Exception e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška prilikom rada servera: ", e);
        }
    }
}
