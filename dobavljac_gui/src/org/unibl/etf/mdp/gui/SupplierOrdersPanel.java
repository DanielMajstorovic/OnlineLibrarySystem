package org.unibl.etf.mdp.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import org.json.JSONArray;
import org.json.JSONObject;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.rabbitmq.RabbitMQHelper;
import org.unibl.etf.mdp.rmi.IAccountingService;

import com.rabbitmq.client.GetResponse;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.Random;

import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.model.*;

public class SupplierOrdersPanel extends JPanel {
    
    private String supplierName;       
    private RabbitMQHelper rabbitMQ;    
    private Channel channel;            
    private String queueName;         

    private IAccountingService accountingService; 

    private JTextArea booksTextArea;
    private JButton getNextOrderButton;
    private JButton confirmOrderButton;
    private JButton rejectOrderButton;
    
    private GetResponse currentOrder;
    
    public SupplierOrdersPanel(String supplierName) {
        this.supplierName = supplierName;
        initGUI();
        initRabbitMQ();
        initRmiClient(); 
    }
    

    private void initGUI() {
        setLayout(new BorderLayout());
        
        booksTextArea = new JTextArea(10, 30);
        booksTextArea.setEditable(false);
        
        getNextOrderButton = new JButton("Preuzmi sljedeću narudžbu");
        confirmOrderButton = new JButton("Potvrdi narudžbu");
        rejectOrderButton = new JButton("Odbij narudžbu");
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(getNextOrderButton);
        buttonPanel.add(confirmOrderButton);
        buttonPanel.add(rejectOrderButton);
        
        add(new JScrollPane(booksTextArea), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        getNextOrderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchNextOrder();
            }
        });
        
        confirmOrderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmOrder();
            }
        });
        
        rejectOrderButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rejectOrder();
            }
        });
    }
    

    private void initRabbitMQ() {
        try {
            rabbitMQ = new RabbitMQHelper();
            channel = rabbitMQ.getChannel();
            
            queueName = supplierName + "_queue";
            
            channel.queueDeclare(queueName, true, false, false, null);
            
            channel.queueBind(queueName, rabbitMQ.getExchangeName(), supplierName);
            
            AppLogger.getLogger().info("Dobavljač '" + supplierName + "' povezan na RabbitMQ, queue: " + queueName);
            
        } catch (IOException | TimeoutException e) {
            AppLogger.getLogger().severe("Greška prilikom inicijalizacije RabbitMQ za dobavljača " 
                + supplierName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
  
    private void initRmiClient() {
        try {
            
            String host = ConfigLoader.getProperty("rmi.host");  
            int port = Integer.parseInt(ConfigLoader.getProperty("rmi.port")); 
            String serviceName = ConfigLoader.getProperty("rmi.serviceName");  

            Registry registry = LocateRegistry.getRegistry(host, port);
            accountingService = (IAccountingService) registry.lookup(serviceName);

            AppLogger.getLogger().info("RMI klijent: povezan na " + host + ":" 
                + port + " serviceName: " + serviceName);

        } catch (Exception e) {
            AppLogger.getLogger().severe("Greška prilikom povezivanja na RMI server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
 
    private void fetchNextOrder() {
        try {
            GetResponse response = channel.basicGet(queueName, false);
            
            if (response == null) {
                JOptionPane.showMessageDialog(this, "Nema novih narudžbi za " + supplierName + ".", 
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                booksTextArea.setText("");
                currentOrder = null;
            } else {
                currentOrder = response;
                
                String messageBody = new String(response.getBody(), StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(messageBody);
                
                String libraryName = json.optString("supplier", "");
                JSONArray booksArray = json.optJSONArray("books");
                
                StringBuilder sb = new StringBuilder();
                sb.append("Narudžba iz biblioteke: ").append(libraryName).append("\n");
                sb.append("Poručene knjige:\n");
                
                if (booksArray != null) {
                    for (int i = 0; i < booksArray.length(); i++) {
                        JSONObject bookObj = booksArray.getJSONObject(i);
                        String title = bookObj.optString("title", "");
                        int quantity = bookObj.optInt("quantity", 0);
                        
                        sb.append(" - ").append(title)
                          .append(" (x").append(quantity).append(")\n");
                    }
                }
                
                booksTextArea.setText(sb.toString());
            }
            
        } catch (IOException e) {
            AppLogger.getLogger().warning("Greška prilikom dohvatanja narudžbe: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void confirmOrder() {
        if (currentOrder == null) {
            JOptionPane.showMessageDialog(this, "Nema narudžbe za potvrdu.", 
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            String messageBody = new String(currentOrder.getBody(), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(messageBody);
            
            List<BookItem> bookItems = new ArrayList<>();
            JSONArray booksArray = json.optJSONArray("books");
            if (booksArray != null) {
                for (int i = 0; i < booksArray.length(); i++) {
                    JSONObject bookObj = booksArray.getJSONObject(i);
                    String title = bookObj.optString("title", "");
                    int quantity = bookObj.optInt("quantity", 0);
                    bookItems.add(new BookItem(title, quantity));
                }
            }

            String host = ConfigLoader.getProperty("server.host");  
            int port = Integer.parseInt(ConfigLoader.getProperty("server.port")); 
            for (BookItem item : bookItems) {
                if (item.getQuantity() <= 0) {
                    continue; 
                }

                BookDataWithText bookData = fetchBookFromSupplierServer(host, port, item.getTitle());
                
                if (bookData != null) {
                    sendBookToLibraryServer(bookData, item.getQuantity());
                } else {
                    AppLogger.getLogger().warning("Knjiga '" + item.getTitle() + "' nije pronađena na dobavljačevom serveru!");
                }
            }

            double totalPrice = generateRandomPrice();

            Invoice invoice = new Invoice(
                    supplierName,
                    LocalDateTime.now(),
                    bookItems, // ista lista
                    totalPrice
            );

            double pdv = 0.0;
            if (accountingService != null) {
                pdv = accountingService.processInvoice(invoice);
            } else {
                AppLogger.getLogger().warning("RMI service nije inicijalizovan!");
            }

            channel.basicAck(currentOrder.getEnvelope().getDeliveryTag(), false);

            JOptionPane.showMessageDialog(this, 
                "Narudžba je uspješno potvrđena!\nUkupna cijena: " + String.format("%.2f", totalPrice) 
                + "\nPDV (17%): " + String.format("%.2f", pdv), 
                "Potvrđeno", 
                JOptionPane.INFORMATION_MESSAGE);

            booksTextArea.setText("");
            currentOrder = null;

        } catch (Exception e) {
            AppLogger.getLogger().warning("Greška prilikom potvrde narudžbe: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private BookDataWithText fetchBookFromSupplierServer(String host, int port, String bookTitle) {
        BookDataWithText result = null;
        
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeBytes("REQUEST_FILE\n");
            out.writeBytes(bookTitle + "\n");

            String response = in.readUTF();
            if ("BOOK_NOT_FOUND".equals(response)) {
                return null;
            }

            BookDataWithText bd = new Gson().fromJson(response, BookDataWithText.class);

            int imageLen = in.readInt();
            if (imageLen > 0) {
                byte[] imageBytes = new byte[imageLen];
                in.readFully(imageBytes);
                bd.setCoverImageData(imageBytes);
            }

            long fileSize = in.readLong();
            if (fileSize > 0) {
                byte[] textBytes = new byte[(int) fileSize];
                in.readFully(textBytes);
                bd.setBookText(new String(textBytes, StandardCharsets.UTF_8));
            }

            result = bd;
        } catch (IOException ex) {
            AppLogger.getLogger().warning("Greška prilikom dohvaćanja knjige '" + bookTitle + "': " + ex.getMessage());
        }
        
        return result;
    }


    private void sendBookToLibraryServer(BookDataWithText bookData, int quantity) {
        try {
            JSONObject json = new JSONObject();
            json.put("title", bookData.getTitle());
            json.put("author", bookData.getAuthor());
            json.put("publishDate", bookData.getPublishDate());
            json.put("language", bookData.getLanguage());
            json.put("text", bookData.getBookText());
            json.put("quantity", quantity);

            if (bookData.getCoverImageData() != null && bookData.getCoverImageData().length > 0) {
                String base64Image = Base64.getEncoder().encodeToString(bookData.getCoverImageData());
                json.put("coverImage", base64Image);
            } else {
                json.put("coverImage", JSONObject.NULL);
            }

            URL url = new URL("http://localhost:8080/BibliotekaServer/api/knjige/postBook");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                AppLogger.getLogger().info("POST /knjige/postBook uspješan za knjigu: " + bookData.getTitle());
            } else {
                AppLogger.getLogger().warning("POST /knjige/postBook neuspio, code=" + responseCode);
            }

            conn.disconnect();

        } catch (Exception e) {
            AppLogger.getLogger().warning("Greška pri slanju POST zahtjeva: " + e.getMessage());
        }
    }
    

    private void rejectOrder() {
        if (currentOrder == null) {
            JOptionPane.showMessageDialog(this, "Nema narudžbe za odbijanje.", 
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        try {
            channel.basicReject(currentOrder.getEnvelope().getDeliveryTag(), false);
            JOptionPane.showMessageDialog(this, "Narudžba je odbijena!", 
                    "Odbijeno", JOptionPane.INFORMATION_MESSAGE);
            
            booksTextArea.setText("");
            currentOrder = null;
        } catch (IOException e) {
            AppLogger.getLogger().warning("Greška prilikom odbijanja narudžbe: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    private double generateRandomPrice() {
        double min = 50.0;
        double max = 2000.0;
        return min + (max - min) * Math.random();
    }


    public void close() {
        if (rabbitMQ != null) {
            try {
                rabbitMQ.close();
            } catch (IOException | TimeoutException e) {
                AppLogger.getLogger().severe("Greška prilikom zatvaranja RabbitMQ konekcije: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
