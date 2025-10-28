package org.unibl.etf.mdp.gui;

import com.google.gson.Gson;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.model.Book;
import org.unibl.etf.mdp.persistance.OfferPersistence;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.logging.Level;

public class AllBooksPanel extends JPanel {

    private Socket socket;
    private String supplierName;
    private Set<String> myOffers; 

    private JPanel booksContainer;
    private JScrollPane scrollPane;

    public AllBooksPanel(Socket socket, String supplierName, Set<String> myOffers) {
        this.socket = socket;
        this.supplierName = supplierName;
        this.myOffers = myOffers;

        setLayout(new BorderLayout());
        booksContainer = new JPanel();
        booksContainer.setLayout(new BoxLayout(booksContainer, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(booksContainer);
        add(scrollPane, BorderLayout.CENTER);

        loadBooksFromServer();
    }

    private void loadBooksFromServer() {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF("GET_BOOKS\n");

            DataInputStream din = new DataInputStream(socket.getInputStream());

            while (true) {

                String json = din.readUTF();
                if ("END_LIST".equals(json)) {
                    break; 
                }
                Book book = new Gson().fromJson(json, Book.class);

                int length = din.readInt();
                byte[] imgBytes = new byte[length];
                din.readFully(imgBytes);

                if (length > 0) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(imgBytes);
                    book.setCoverImage(ImageIO.read(bais));
                }

                addBookComponent(book);
            }


        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška pri učitavanju knjiga sa servera: ", e);
        }
    }

    private void addBookComponent(Book book) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.setLayout(new BorderLayout());

        if (book.getCoverImage() != null) {
            JLabel coverLabel = new JLabel(new ImageIcon(book.getCoverImage()));
            panel.add(coverLabel, BorderLayout.WEST);
        }

        JTextArea infoArea = new JTextArea(5, 30);
        infoArea.setEditable(false);
        infoArea.setText(
                "Title: " + book.getTitle() + "\n" +
                "Author: " + book.getAuthor() + "\n" +
                "Publish Date: " + book.getPublishDate() + "\n" +
                "Language: " + book.getLanguage() + "\n"
        );
        panel.add(infoArea, BorderLayout.CENTER);

        JButton btnAdd = new JButton("Add to Offer");
        if (myOffers.contains(book.getTitle())) {
            btnAdd.setEnabled(false);
        }
        btnAdd.addActionListener(e -> {
            myOffers.add(book.getTitle()); 
            btnAdd.setEnabled(false); 
            OfferPersistence.saveOffersForSupplier(supplierName, myOffers);
        });
        panel.add(btnAdd, BorderLayout.EAST);

        booksContainer.add(panel);
        booksContainer.add(Box.createVerticalStrut(10));
        booksContainer.revalidate();
    }
}
