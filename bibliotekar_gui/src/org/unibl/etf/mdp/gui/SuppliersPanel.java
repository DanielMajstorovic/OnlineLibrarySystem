package org.unibl.etf.mdp.gui;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.rabbitmq.RabbitMQHelper;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;

public class SuppliersPanel extends JPanel {

    private Map<String, List<String>> suppliersData; 
    private JList<String> suppliersList; 
    private DefaultListModel<String> suppliersListModel;

    private JTable booksTable;
    private BookTableModel bookTableModel;

    public SuppliersPanel() {
        setLayout(new BorderLayout());
        initGUI();
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                refresh(); 
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {}

            @Override
            public void ancestorMoved(AncestorEvent event) {}
        });
    }

    private void initGUI() {
        suppliersListModel = new DefaultListModel<>();
        suppliersList = new JList<>(suppliersListModel);
        suppliersList.setBorder(new TitledBorder("Dobavljači"));
        suppliersList.addListSelectionListener(e -> onSupplierSelected());

        bookTableModel = new BookTableModel();
        booksTable = new JTable(bookTableModel);
        booksTable.setFillsViewportHeight(true);
        
        booksTable.setRowHeight(25);

        TableColumn quantityColumn = booksTable.getColumnModel().getColumn(1);
        quantityColumn.setCellEditor(new SpinnerEditor());


        JButton confirmButton = new JButton("Potvrdi izbor");
        confirmButton.addActionListener(this::onConfirm);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JScrollPane(suppliersList), BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JScrollPane(booksTable), BorderLayout.CENTER);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
        add(confirmButton, BorderLayout.SOUTH);
    }

    private void refresh() {
        suppliersData = loadSuppliersFromServer();
        updateSuppliersList();
    }

    private void updateSuppliersList() {
        suppliersListModel.clear();
        if (suppliersData != null) {
            for (String supplier : suppliersData.keySet()) {
                suppliersListModel.addElement(supplier);
            }
        }
    }

    private void onSupplierSelected() {
        String selectedSupplier = suppliersList.getSelectedValue();
        if (selectedSupplier != null && suppliersData != null && suppliersData.containsKey(selectedSupplier)) {
            List<String> books = suppliersData.get(selectedSupplier);
            bookTableModel.setBooks(books);
        } else {
            bookTableModel.setBooks(Collections.emptyList());
        }
    }

    private void onConfirm(ActionEvent e) {
        String selectedSupplier = suppliersList.getSelectedValue();
        if (selectedSupplier == null) {
            JOptionPane.showMessageDialog(this, 
                "Niste odabrali dobavljača!", 
                "Greška", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Integer> booksToOrder = bookTableModel.getBooksToOrder();
        if (booksToOrder.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Niste unijeli pozitivnu količinu ni za jednu knjigu!", 
                "Greška", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder message = new StringBuilder("Da li želite poslati narudžbu?\n");
        message.append("Dobavljač: ").append(selectedSupplier).append("\nKnjige:\n");
        for (Map.Entry<String, Integer> entry : booksToOrder.entrySet()) {
            message.append(" - ").append(entry.getKey())
                   .append(" (x").append(entry.getValue()).append(")\n");
        }

        int result = JOptionPane.showConfirmDialog(this, 
                message.toString(), 
                "Potvrda narudžbe", 
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            sendOrderToRabbitMQ(selectedSupplier, booksToOrder);
        }
    }

    private void sendOrderToRabbitMQ(String supplier, Map<String, Integer> booksToOrder) {
        try (RabbitMQHelper rabbitMQ = new RabbitMQHelper()) {

            StringBuilder orderMessage = new StringBuilder();
            orderMessage.append("{\"supplier\":\"").append(supplier).append("\",\"books\":[");
            int i = 0;
            for (Map.Entry<String, Integer> entry : booksToOrder.entrySet()) {
                orderMessage.append("{\"title\":\"")
                            .append(entry.getKey())
                            .append("\",\"quantity\":")
                            .append(entry.getValue())
                            .append("}");
                if (i < booksToOrder.size() - 1) {
                    orderMessage.append(",");
                }
                i++;
            }
            orderMessage.append("]}");

            rabbitMQ.sendMessage(supplier, orderMessage.toString());

            JOptionPane.showMessageDialog(this, 
                "Narudžba je uspešno poslata!", 
                "Uspeh", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Došlo je do greške prilikom slanja narudžbe: " + ex.getMessage(), 
                "Greška", 
                JOptionPane.ERROR_MESSAGE);
            AppLogger.getLogger().warning("RabbitMQ greška: " + ex.getMessage());
        }
    }

    private Map<String, List<String>> loadSuppliersFromServer() {
        Map<String, List<String>> suppliers = new HashMap<>();
        try {
            String host = ConfigLoader.getProperty("offer.socket.host");
            int port = Integer.parseInt(ConfigLoader.getProperty("offer.socket.port"));
            Socket socket = new Socket(host, port);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.writeUTF("GET_SUPPLIERS");

            String response = in.readUTF();
            if ("SUPPLIERS_LIST".equals(response)) {
                while (true) {
                    String supplierName = in.readUTF();
                    if ("END_SUPPLIERS".equals(supplierName)) {
                        break;
                    }

                    List<String> books = new ArrayList<>();
                    while (true) {
                        String book = in.readUTF();
                        if ("END_BOOKS".equals(book)) {
                            break;
                        }
                        books.add(book);
                    }
                    suppliers.put(supplierName, books);
                }
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Nema dostupnih dobavljača.", 
                    "Info", 
                    JOptionPane.INFORMATION_MESSAGE);
            }

            socket.close();
        } catch (IOException e) {
            AppLogger.getLogger().warning("Greska: " + e.getMessage());
        }
        return suppliers;
    }


    private static class BookTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {"Knjiga", "Količina"};
        private final List<BookQuantity> data = new ArrayList<>();

        public void setBooks(List<String> books) {
            data.clear();
            for (String book : books) {
                data.add(new BookQuantity(book, 0)); 
            }
            fireTableDataChanged();
        }

        public Map<String, Integer> getBooksToOrder() {
            Map<String, Integer> result = new HashMap<>();
            for (BookQuantity bq : data) {
                if (bq.getQuantity() > 0) {
                    result.put(bq.getTitle(), bq.getQuantity());
                }
            }
            return result;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 1) {
                return Integer.class; 
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BookQuantity bq = data.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return bq.getTitle();
                case 1:
                    return bq.getQuantity();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            BookQuantity bq = data.get(rowIndex);
            if (columnIndex == 1 && aValue instanceof Integer) {
                bq.setQuantity((Integer) aValue);
            }
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }


    private static class BookQuantity {
        private String title;
        private int quantity;

        public BookQuantity(String title, int quantity) {
            this.title = title;
            this.quantity = quantity;
        }

        public String getTitle() {
            return title;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            if (quantity < 0)
                quantity = 0; 
            this.quantity = quantity;
        }
    }


    private static class SpinnerEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner;

        public SpinnerEditor() {
            spinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            spinner.setValue(value);
            return spinner;
        }
    }
}
