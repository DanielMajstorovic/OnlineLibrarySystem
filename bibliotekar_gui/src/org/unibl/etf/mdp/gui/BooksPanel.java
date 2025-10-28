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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class BooksPanel extends JPanel {

    private static final String BOOKS_API_URL = ConfigLoader.getProperty("api.books.url");

    private JTable table;
    private BookTableModel tableModel;

    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnView;

    public BooksPanel() {
        setLayout(new BorderLayout(10, 10));

        if (BOOKS_API_URL == null || BOOKS_API_URL.trim().isEmpty()) {
            AppLogger.getLogger().severe("api.books.url nije definisan u app.properties!");
        }

        tableModel = new BookTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnAdd = new JButton("Dodaj");
        btnUpdate = new JButton("Ažuriraj");
        btnDelete = new JButton("Obriši");
        btnRefresh = new JButton("Osvježi");
        btnView = new JButton("Pregled");

        topPanel.add(btnAdd);
        topPanel.add(btnUpdate);
        topPanel.add(btnDelete);
        topPanel.add(btnRefresh);
        topPanel.add(btnView);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadAllBooks());
        btnAdd.addActionListener(e -> onAddBook());
        btnUpdate.addActionListener(e -> onUpdateBook());
        btnDelete.addActionListener(e -> onDeleteBook());
        btnView.addActionListener(e -> onViewBook());

        loadAllBooks();
    }


    private void loadAllBooks() {
        if (BOOKS_API_URL == null || BOOKS_API_URL.trim().isEmpty()) {
            AppLogger.getLogger().severe("BOOKS_API_URL je null ili prazan - ne možemo učitati knjige.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                String urlStr = BOOKS_API_URL;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                AppLogger.getLogger().info("GET /knjige -> code: " + responseCode);

                if (responseCode == 200) {
                    String response = readStream(conn.getInputStream());
                    JSONArray jsonArr = new JSONArray(response);

                    List<BookData> newData = new ArrayList<>();
                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject obj = jsonArr.getJSONObject(i);
                        BookData book = parseBookJson(obj);
                        newData.add(book);
                    }
                    tableModel.setData(newData);

                } else {
                    JOptionPane.showMessageDialog(this,
                            "Greška prilikom učitavanja knjiga. Kod: " + responseCode,
                            "Greška", JOptionPane.ERROR_MESSAGE);
                }
                conn.disconnect();
            } catch (Exception e) {
                AppLogger.getLogger().severe("Greška prilikom konekcije na server: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Greška prilikom konekcije na server:\n" + e.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        });
    }


    private void onAddBook() {
        if (!checkApiUrl()) return;

        AddBookDialog dialog = new AddBookDialog(SwingUtilities.getWindowAncestor(this), null);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            BookData newBook = dialog.getBook();
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(BOOKS_API_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject j = toJsonObject(newBook);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(j.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                AppLogger.getLogger().info("POST /knjige -> code: " + responseCode);

                if (responseCode == 201) {
                    JOptionPane.showMessageDialog(this, "Knjiga uspješno kreirana: " + newBook.title);
                    loadAllBooks();
                } else {
                    String err = readStream(conn.getErrorStream());
                    JOptionPane.showMessageDialog(this, "Greška prilikom kreiranja knjige:\n" + err,
                            "Greška", JOptionPane.ERROR_MESSAGE);
                }
                conn.disconnect();
            } catch (Exception e) {
                AppLogger.getLogger().severe("Greška onAddBook: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Greška:\n" + e.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void onUpdateBook() {
        if (!checkApiUrl()) return;

        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite knjigu iz tabele.", "Upozorenje",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        BookData oldData = tableModel.getRow(selectedRow);

        UpdateBookDialog dialog = new UpdateBookDialog(SwingUtilities.getWindowAncestor(this), oldData);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            BookData updated = dialog.getBook();

            try {
                String encodedTitle = URLEncoder.encode(oldData.title, StandardCharsets.UTF_8.name());
                String urlStr = BOOKS_API_URL + "/" + encodedTitle;
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject j = toJsonObject(updated);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(j.toString().getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                AppLogger.getLogger().info("PUT /knjige/" + oldData.title + " -> code: " + responseCode);

                if (responseCode == 200) {
                    JOptionPane.showMessageDialog(this, "Knjiga ažurirana: " + oldData.title);
                    loadAllBooks();
                } else {
                    String err = readStream(conn.getErrorStream());
                    JOptionPane.showMessageDialog(this, "Greška prilikom ažuriranja knjige:\n" + err,
                            "Greška", JOptionPane.ERROR_MESSAGE);
                }
                conn.disconnect();
            } catch (Exception e) {
                AppLogger.getLogger().severe("Greška onUpdateBook: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Greška:\n" + e.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    private void onDeleteBook() {
        if (!checkApiUrl()) return;

        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite knjigu iz tabele.", "Upozorenje",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        BookData selected = tableModel.getRow(selectedRow);

        int choice = JOptionPane.showConfirmDialog(this,
                "Da li ste sigurni da želite obrisati knjigu: " + selected.title + "?",
                "Brisanje knjige", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            String encodedTitle = URLEncoder.encode(selected.title, StandardCharsets.UTF_8.name());
            String urlStr = BOOKS_API_URL + "/" + encodedTitle;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            AppLogger.getLogger().info("DELETE /knjige/" + selected.title + " -> code=" + responseCode);

            if (responseCode == 200) {
                loadAllBooks();
                JOptionPane.showMessageDialog(this, "Knjiga obrisana: " + selected.title);
            } else {
                String err = readStream(conn.getErrorStream());
                JOptionPane.showMessageDialog(this, "Greška: " + err);
            }
            conn.disconnect();
        } catch (Exception e) {
            AppLogger.getLogger().severe("Greška onDeleteBook: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Greška:\n" + e.getMessage(),
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onViewBook() {
        if (!checkApiUrl()) return;

        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite knjigu iz tabele.", "Upozorenje",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        BookData selected = tableModel.getRow(selectedRow);

        try {
            String encodedTitle = URLEncoder.encode(selected.title, StandardCharsets.UTF_8.name());
            String urlStr = BOOKS_API_URL + "/" + encodedTitle;

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                String resp = readStream(conn.getInputStream());
                JSONObject obj = new JSONObject(resp);
                BookData fullBook = parseBookJson(obj);

                showBookDetailsDialog(fullBook);

            } else {
                String err = readStream(conn.getErrorStream());
                JOptionPane.showMessageDialog(this, "Greška: " + err, "Greška", JOptionPane.ERROR_MESSAGE);
            }
            conn.disconnect();

        } catch (Exception e) {
            AppLogger.getLogger().severe("Greška onViewBook: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Greška:\n" + e.getMessage(),
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void showBookDetailsDialog(BookData book) {
        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Pregled knjige",
            Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setSize(800, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10,10));

        JPanel topPanel = new JPanel(new GridLayout(0,1,5,5));
        topPanel.add(new JLabel("Title: " + book.title));
        topPanel.add(new JLabel("Author: " + book.author));
        topPanel.add(new JLabel("Publish Date: " + book.publishDate));
        topPanel.add(new JLabel("Language: " + book.language));
        topPanel.add(new JLabel("Quantity: " + book.quantity));
        dialog.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        JTextArea textArea = new JTextArea(book.text != null ? book.text : "");
        textArea.setEditable(false);
        JScrollPane textScrollPane = new JScrollPane(textArea);
        centerPanel.add(textScrollPane);

        JLabel imageLabel = new JLabel("Nema slike");
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (book.coverImage != null && !book.coverImage.isEmpty() && !book.coverImage.equals("null")) {
            try {
                byte[] imgBytes = Base64.getDecoder().decode(book.coverImage);
                ImageIcon icon = new ImageIcon(imgBytes);
                Image scaled = icon.getImage().getScaledInstance(200, -1, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaled);
                imageLabel.setIcon(icon);
                imageLabel.setText("");
            } catch (Exception ex) {
                imageLabel.setText("Greška prilikom dekodiranja slike!");
            }
        }

        centerPanel.add(imageLabel);
        dialog.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("Zatvori");
        btnClose.addActionListener(e -> dialog.setVisible(false));
        bottomPanel.add(btnClose);

        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }


    // ==============================  HELPERS   ===========================================

    private boolean checkApiUrl() {
        if (BOOKS_API_URL == null || BOOKS_API_URL.trim().isEmpty()) {
            AppLogger.getLogger().severe("api.books.url je null/prazan - provjerite app.properties!");
            JOptionPane.showMessageDialog(this, "API URL nije definisan! Pogledati app.properties.", "Greška", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private String readStream(InputStream is) {
        if (is == null)
            return "";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while((line=br.readLine())!=null) {
                sb.append(line);
            }
            return sb.toString();
        } catch(IOException e) {
            AppLogger.getLogger().severe("readStream error: " + e.getMessage());
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
        b.coverImage = obj.optString("coverImage", null); // base64
        b.quantity = obj.optInt("quantity", 0);
        return b;
    }

    private JSONObject toJsonObject(BookData b) {
        JSONObject j = new JSONObject();
        j.put("title", b.title);
        j.put("author", b.author);
        j.put("publishDate", b.publishDate);
        j.put("language", b.language);
        j.put("text", b.text);
        j.put("quantity", b.quantity);
        j.put("coverImage", b.coverImage != null ? b.coverImage : JSONObject.NULL);
        return j;
    }

    private static class BookData {
        String title;
        String author;
        String publishDate;
        String language;
        String text;
        String coverImage; // base64
        int quantity;
    }

    private static class BookTableModel extends AbstractTableModel {

        private List<BookData> data = new ArrayList<>();
        private final String[] colNames = {
            "Title", "Author", "PublishDate", "Language", "Quantity"
        };

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BookData b = data.get(rowIndex);
            switch (columnIndex) {
                case 0: return b.title;
                case 1: return b.author;
                case 2: return b.publishDate;
                case 3: return b.language;
                case 4: return b.quantity;
                default: return "";
            }
        }

        public void setData(List<BookData> newData) {
            this.data = newData;
            fireTableDataChanged();
        }

        public BookData getRow(int index) {
            return data.get(index);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    private class AddBookDialog extends JDialog {
        private boolean confirmed = false;
        private BookData book = new BookData();

        private JTextField titleField;
        private JTextField authorField;
        private JTextField publishDateField;
        private JTextField languageField;
        private JTextField quantityField;
        private JTextField coverImageField; // Base64
        private JTextArea textArea;

        public AddBookDialog(Window owner, BookData initData) {
            super(owner, "Dodaj knjigu", ModalityType.APPLICATION_MODAL);
            setSize(450, 400);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10,10));

            JPanel formPanel = new JPanel(new GridLayout(0,2,5,5));

            formPanel.add(new JLabel("Title:"));
            titleField = new JTextField();
            formPanel.add(titleField);

            formPanel.add(new JLabel("Author:"));
            authorField = new JTextField();
            formPanel.add(authorField);

            formPanel.add(new JLabel("PublishDate:"));
            publishDateField = new JTextField();
            formPanel.add(publishDateField);

            formPanel.add(new JLabel("Language:"));
            languageField = new JTextField();
            formPanel.add(languageField);

            formPanel.add(new JLabel("Quantity:"));
            quantityField = new JTextField("0");
            formPanel.add(quantityField);

            formPanel.add(new JLabel("CoverImage (Base64):"));
            coverImageField = new JTextField();
            formPanel.add(coverImageField);

            formPanel.add(new JLabel("Text:"));
            textArea = new JTextArea(3, 20);
            formPanel.add(new JScrollPane(textArea));

            add(formPanel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnOk = new JButton("OK");
            JButton btnCancel = new JButton("Cancel");
            bottomPanel.add(btnOk);
            bottomPanel.add(btnCancel);
            add(bottomPanel, BorderLayout.SOUTH);

            btnOk.addActionListener(e -> {
                book.title = titleField.getText();
                book.author = authorField.getText();
                book.publishDate = publishDateField.getText();
                book.language = languageField.getText();
                book.text = textArea.getText();
                book.coverImage = coverImageField.getText();
                try {
                    book.quantity = Integer.parseInt(quantityField.getText());
                } catch (Exception ex) {
                    book.quantity = 0;
                }

                confirmed = true;
                setVisible(false);
            });

            btnCancel.addActionListener(e -> {
                confirmed = false;
                setVisible(false);
            });
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public BookData getBook() {
            return book;
        }
    }

    private class UpdateBookDialog extends JDialog {
        private boolean confirmed = false;
        private BookData book;

        private JTextField titleField;
        private JTextField authorField;
        private JTextField publishDateField;
        private JTextField languageField;
        private JTextField quantityField;
        private JTextField coverImageField;
        private JTextArea textArea;

        public UpdateBookDialog(Window owner, BookData oldData) {
            super(owner, "Ažuriranje knjige", ModalityType.APPLICATION_MODAL);
            setSize(450, 400);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(10,10));

            this.book = new BookData();

            book.title = oldData.title;
            book.author = oldData.author;
            book.publishDate = oldData.publishDate;
            book.language = oldData.language;
            book.quantity = oldData.quantity;
            book.coverImage = oldData.coverImage;
            book.text = oldData.text;

            JPanel formPanel = new JPanel(new GridLayout(0,2,5,5));

            formPanel.add(new JLabel("Title (read-only):"));
            titleField = new JTextField(book.title);
            titleField.setEditable(false);
            formPanel.add(titleField);

            formPanel.add(new JLabel("Author:"));
            authorField = new JTextField(book.author);
            formPanel.add(authorField);

            formPanel.add(new JLabel("PublishDate:"));
            publishDateField = new JTextField(book.publishDate);
            formPanel.add(publishDateField);

            formPanel.add(new JLabel("Language:"));
            languageField = new JTextField(book.language);
            formPanel.add(languageField);

            formPanel.add(new JLabel("Quantity:"));
            quantityField = new JTextField(String.valueOf(book.quantity));
            formPanel.add(quantityField);

            formPanel.add(new JLabel("CoverImage (Base64):"));
            coverImageField = new JTextField(book.coverImage != null ? book.coverImage : "");
            formPanel.add(coverImageField);

            formPanel.add(new JLabel("Text:"));
            textArea = new JTextArea(book.text, 3, 20);
            formPanel.add(new JScrollPane(textArea));

            add(formPanel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnOk = new JButton("OK");
            JButton btnCancel = new JButton("Cancel");
            bottomPanel.add(btnOk);
            bottomPanel.add(btnCancel);
            add(bottomPanel, BorderLayout.SOUTH);

            btnOk.addActionListener(e -> {
    
                book.author = authorField.getText();
                book.publishDate = publishDateField.getText();
                book.language = languageField.getText();
                book.coverImage = coverImageField.getText();
                book.text = textArea.getText();
                try {
                    book.quantity = Integer.parseInt(quantityField.getText());
                } catch (Exception ex) {
                    book.quantity = 0;
                }

                confirmed = true;
                setVisible(false);
            });

            btnCancel.addActionListener(e -> {
                confirmed = false;
                setVisible(false);
            });

            add(formPanel, BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        public boolean isConfirmed() {
            return confirmed;
        }

        public BookData getBook() {
            return book;
        }
    }
}
