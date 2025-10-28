package org.unibl.etf.mdp.server;

import com.google.gson.Gson;

import javax.imageio.ImageIO;

import org.unibl.etf.mdp.bookutls.BookFileSender;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.model.BookData;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import org.unibl.etf.mdp.model.*;

public class SupplierClientHandler extends Thread {

    private final Socket clientSocket;
    private BufferedReader reader;
    private DataOutputStream writer;

    private final List<BookData> availableBooks;
    private String currentSupplierName = null;

    public SupplierClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.availableBooks = SupplierServer.loadBooks();
        
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writer = new DataOutputStream(clientSocket.getOutputStream());

            String command;
            while ((command = reader.readLine()) != null && !"END".equalsIgnoreCase(command)) {
                AppLogger.getLogger().info("Primljena komanda: " + command);

                switch (command) {
                    case "GET_BOOKS":
                        sendBookList();
                        break;
                    case "REQUEST_FILE":
                        handleBookRequest();
                        break;
                    default:
                        AppLogger.getLogger().warning("Nepoznata komanda: " + command);
                        break;
                }
            }

            clientSocket.close();

        } catch (Exception e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška u klijentskoj niti: ", e);
        }
    }

    private void sendBookList() {
        try {
            Gson gson = new Gson();
            for (BookData book : availableBooks) {
                String bookJson = gson.toJson(book);
                writer.writeUTF(bookJson);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (book.getCoverImage() != null) {
                    ImageIO.write(book.getCoverImage(), "jpg", baos);
                }
                byte[] imageBytes = baos.toByteArray();
                writer.writeInt(imageBytes.length);
                writer.write(imageBytes);
            }
            writer.writeUTF("END_LIST");
        } catch (Exception e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška pri slanju liste knjiga: ", e);
        }
    }

    private void handleBookRequest() {
        try {
            String requestedTitle = reader.readLine();
            AppLogger.getLogger().info("Klijent traži knjigu: " + requestedTitle);

            boolean found = false;
            
            for (BookData book : availableBooks) {
                if (book.getTitle() != null && book.getTitle().equals(requestedTitle)) {
                    found = true;

                    Gson gson = new Gson();
                    String jsonBook = gson.toJson(book);
                    writer.writeUTF(jsonBook);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (book.getCoverImage() != null) {
                        ImageIO.write(book.getCoverImage(), "jpg", baos);
                    }
                    byte[] coverBytes = baos.toByteArray();
                    writer.writeInt(coverBytes.length);
                    writer.write(coverBytes);

                    BookFileSender.sendBookFile(book.getTitle(), writer);

                    break; 
                }
            }
            
            if(!found) {
                writer.writeUTF("BOOK_NOT_FOUND");
            }

        } catch (Exception e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška pri REQUEST_FILE: ", e);
        }
    }
}
