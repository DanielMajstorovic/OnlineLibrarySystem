package org.unibl.etf.mdp.gui;

import org.json.JSONArray;
import org.json.JSONObject;
import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.Base64;

public class MemberBooksPanel extends JPanel {

    private static final String BOOKS_API_URL = ConfigLoader.getProperty("api.books.url");
    private static final String CREATE_RESERVATION_URL = BOOKS_API_URL + "/createReservation";

    private JTable table;
    private BookTableModel tableModel;

    private JTextField searchField;
    private JButton btnSearch, btnRefresh, btnDetails, btnReserve;

    private List<BookData> allBooks = new ArrayList<>();

    private String memberName;

    public MemberBooksPanel(String memberName) {
        this.memberName = memberName;

        setLayout(new BorderLayout(10, 10));

        if (BOOKS_API_URL == null || BOOKS_API_URL.trim().isEmpty()) {
            AppLogger.getLogger().severe("api.books.url nije definisan u app.properties!");
        }

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        searchField = new JTextField(20);
        btnSearch = new JButton("Traži");
        btnRefresh = new JButton("Osvježi");
        btnDetails = new JButton("Detalji");
        btnReserve = new JButton("Rezerviši");

        topPanel.add(new JLabel("Pretraga naslova:"));
        topPanel.add(searchField);
        topPanel.add(btnSearch);
        topPanel.add(btnRefresh);
        topPanel.add(btnDetails);
        topPanel.add(btnReserve);

        add(topPanel, BorderLayout.NORTH);

        tableModel = new BookTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); 
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadAllBooks());
        btnSearch.addActionListener(e -> onSearch());
        btnDetails.addActionListener(e -> onDetails());
        btnReserve.addActionListener(e -> onReserve());

        loadAllBooks();
    }


    private void loadAllBooks() {
        if (!checkApiUrl()) return;

        SwingUtilities.invokeLater(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(BOOKS_API_URL).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                AppLogger.getLogger().info("GET /knjige => code=" + code);

                if (code == 200) {
                    String resp = readStream(conn.getInputStream());
                    JSONArray arr = new JSONArray(resp);

                    allBooks.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        BookData b = parseBookJson(obj);
                        allBooks.add(b);
                    }
                   
                    tableModel.setData(new ArrayList<>(allBooks));
                } else {
                    JOptionPane.showMessageDialog(this, "Greška GET /knjige, code=" + code,
                            "Greška", JOptionPane.ERROR_MESSAGE);
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Greška loadAllBooks: " + e.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        });
    }


    private void onSearch() {
        String query = searchField.getText().toLowerCase().trim();
        
        if (query.isEmpty()) {
            tableModel.setData(new ArrayList<>(allBooks));
            return;
        }
        
        List<BookData> filtered = new ArrayList<>();
        for (BookData b : allBooks) {
            if (b.title != null && b.title.toLowerCase().contains(query)) {
                filtered.add(b);
            }
        }
        tableModel.setData(filtered);
    }

  
    private void onDetails() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite jednu knjigu!", "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        BookData sel = tableModel.getRow(row);

        // Prvih 100 linija (mozda je trebalo ukloniti "metapodatke")
        String[] lines = (sel.text != null) ? sel.text.split("\n") : new String[]{};
        StringBuilder partial = new StringBuilder();
        for(int i=0; i < Math.min(100, lines.length); i++) {
            partial.append(lines[i]).append("\n");
        }

        showDetailsDialog(sel, partial.toString());
    }


    private void onReserve() {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "Odaberite barem jednu knjigu!", "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> chosenTitles = new ArrayList<>();
        for(int r : selectedRows) {
            BookData b = tableModel.getRow(r);
            if(b.title != null) {
                chosenTitles.add(b.title);
            }
        }
        if(chosenTitles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nema validnih naslova za rezervisanje!", "Greška", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JSONObject j = new JSONObject();
        j.put("memberName", memberName);
        j.put("titles", chosenTitles);

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(CREATE_RESERVATION_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try(OutputStream os = conn.getOutputStream()) {
                os.write(j.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            AppLogger.getLogger().info("POST /createReservation => code=" + code);

            if(code == 200) {
            	JOptionPane.showMessageDialog(this,
            		    "<html>Uspješno ste rezervisali knjige: " + chosenTitles + 
            		    "<br><i>Knjige će Vam biti poslane na mejl ukoliko bibliotekar to odobri.</i></html>",
            		    "Info", JOptionPane.INFORMATION_MESSAGE);

            } else {
                String err = readStream(conn.getErrorStream());
                JOptionPane.showMessageDialog(this,
                        "Greška kod rezervisanja!\n" + err,
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
            conn.disconnect();
        } catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Greška:\n" + e.getMessage(), "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ====================== Pomocne metode ======================

    private boolean checkApiUrl() {
        if (BOOKS_API_URL == null || BOOKS_API_URL.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "API URL nije definisan (pogledati app.properties).",
                    "Greška", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private String readStream(InputStream is) {
        if(is == null) return "";
        try(BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while((line=br.readLine())!=null) {
                sb.append(line);
            }
            return sb.toString();
        } catch(IOException e) {
            return "";
        }
    }

    private BookData parseBookJson(JSONObject obj) {
        BookData b = new BookData();
        b.title = obj.optString("title", "");
        b.author = obj.optString("author", "");
        b.publishDate = obj.optString("publishDate", "");
        b.language = obj.optString("language", "");
        b.text = obj.optString("text", "");
        b.coverImage = obj.optString("coverImage", null);
        return b;
    }

    private void showDetailsDialog(BookData book, String partialText) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Detalji knjige", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(800,500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10,10));

        JPanel topPanel = new JPanel(new GridLayout(0,1,5,5));
        topPanel.add(new JLabel("Title: " + book.title));
        topPanel.add(new JLabel("Author: " + book.author));
        topPanel.add(new JLabel("Publish Date: " + book.publishDate));
        topPanel.add(new JLabel("Language: " + book.language));
        dialog.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1,2,10,10));

        JTextArea textArea = new JTextArea(partialText);
        textArea.setEditable(false);
        centerPanel.add(new JScrollPane(textArea));

        JLabel imageLabel = new JLabel("Nema slike");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if(book.coverImage != null && !book.coverImage.isEmpty() && !book.coverImage.equals("null")) {
            try {
                byte[] imgBytes = Base64.getDecoder().decode(book.coverImage);
                ImageIcon icon = new ImageIcon(imgBytes);
 
                Image scaled = icon.getImage().getScaledInstance(200, -1, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaled);
                imageLabel.setIcon(icon);
                imageLabel.setText("");
            } catch(Exception e) {
                imageLabel.setText("Greška dekodiranja slike!");
            }
        }
        centerPanel.add(imageLabel);

        dialog.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Zatvori");
        bottomPanel.add(btnClose);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        btnClose.addActionListener(e -> dialog.setVisible(false));

        dialog.setVisible(true);
    }


    private static class BookData {
        String title;
        String author;
        String publishDate;
        String language;
        String text;
        String coverImage; 
    }

    private class BookTableModel extends AbstractTableModel {
        private List<BookData> data = new ArrayList<>();
        private final String[] cols = {"Title", "Author", "PublishDate", "Language"};

        @Override
        public int getRowCount() {
            return data.size();
        }
        @Override
        public int getColumnCount() {
            return cols.length;
        }
        @Override
        public String getColumnName(int column) {
            return cols[column];
        }
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BookData b = data.get(rowIndex);
            switch(columnIndex) {
                case 0: return b.title;
                case 1: return b.author;
                case 2: return b.publishDate;
                case 3: return b.language;
                default: return "";
            }
        }
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
        public void setData(List<BookData> newData) {
            this.data = newData;
            fireTableDataChanged();
        }
        public BookData getRow(int row) {
            return data.get(row);
        }
    }
}
