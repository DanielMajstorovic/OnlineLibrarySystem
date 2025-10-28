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
import java.util.ArrayList;
import java.util.List;

public class ReservationsPanel extends JPanel {

    private static final String BOOKS_API_URL = ConfigLoader.getProperty("api.books.url");
    private static final String GET_RESERVATIONS_URL = BOOKS_API_URL + "/reservations";
    private static final String APPROVE_URL = BOOKS_API_URL + "/approveReservation";
    private static final String REJECT_URL = BOOKS_API_URL + "/rejectReservation";

    private JTable table;
    private ReservationTableModel tableModel;

    private JButton btnRefresh;
    private JButton btnApprove;
    private JButton btnReject;

    public ReservationsPanel() {
        setLayout(new BorderLayout(10, 10));

        if (BOOKS_API_URL == null || BOOKS_API_URL.trim().isEmpty()) {
            AppLogger.getLogger().severe("api.books.url nije definisan u app.properties!");
        }

        tableModel = new ReservationTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnRefresh = new JButton("Osvježi");
        btnApprove = new JButton("Odobri");
        btnReject = new JButton("Odbij");

        topPanel.add(btnRefresh);
        topPanel.add(btnApprove);
        topPanel.add(btnReject);

        add(topPanel, BorderLayout.NORTH);

        btnRefresh.addActionListener(e -> loadAllReservations());
        btnApprove.addActionListener(e -> approveSelectedReservation());
        btnReject.addActionListener(e -> rejectSelectedReservation());

        loadAllReservations();
    }

    private void loadAllReservations() {
        SwingUtilities.invokeLater(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(GET_RESERVATIONS_URL).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                AppLogger.getLogger().info("GET /reservations => code=" + code);

                if(code == 200) {
                    String resp = readStream(conn.getInputStream());
                    JSONArray arr = new JSONArray(resp);

                    List<ReservationData> data = new ArrayList<>();
                    for(int i=0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        ReservationData r = parseReservation(obj);
                        data.add(r);
                    }
                    tableModel.setData(data);
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Greška pri GET /reservations, code=" + code,
                        "Greška", JOptionPane.ERROR_MESSAGE);
                }
                conn.disconnect();
            } catch(Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Greška:\n" + e.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        });
    }


    private void approveSelectedReservation() {
        int row = table.getSelectedRow();
        if(row == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite rezervaciju iz tabele!", "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ReservationData sel = tableModel.getRow(row);

        int choice = JOptionPane.showConfirmDialog(this, 
                "Odobriti rezervaciju ID=" + sel.id + "?", 
                "Potvrda", JOptionPane.YES_NO_OPTION);
        if(choice != JOptionPane.YES_OPTION) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(APPROVE_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("id", sel.id);

            try(OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            AppLogger.getLogger().info("POST /approveReservation => code=" + code);

            if(code == 200) {
                JOptionPane.showMessageDialog(this, 
                        "Rezervacija odobrena i uklonjena!", 
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                loadAllReservations();
            } else {
                String err = readStream(conn.getErrorStream());
                JOptionPane.showMessageDialog(this, 
                        "Greška pri odobravanju:\n" + err, 
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
            conn.disconnect();

        } catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Greška:\n" + e.getMessage(),
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void rejectSelectedReservation() {
        int row = table.getSelectedRow();
        if(row == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite rezervaciju!", "Upozorenje", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ReservationData sel = tableModel.getRow(row);

        int choice = JOptionPane.showConfirmDialog(this,
                "Odbiti rezervaciju ID=" + sel.id + "?",
                "Potvrda", JOptionPane.YES_NO_OPTION);
        if(choice != JOptionPane.YES_OPTION) return;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(REJECT_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            body.put("id", sel.id);

            try(OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            AppLogger.getLogger().info("POST /rejectReservation => code=" + code);

            if(code == 200) {
                JOptionPane.showMessageDialog(this, 
                        "Rezervacija odbijena i uklonjena!", 
                        "Info", JOptionPane.INFORMATION_MESSAGE);
                loadAllReservations();
            } else {
                String err = readStream(conn.getErrorStream());
                JOptionPane.showMessageDialog(this,
                        "Greška pri odbijanju:\n" + err,
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
            conn.disconnect();
        } catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Greška:\n" + e.getMessage(),
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========== POMOĆNE METODE ===========

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

    private ReservationData parseReservation(JSONObject obj) {
        ReservationData r = new ReservationData();
        r.id = obj.optInt("id", 0);
        r.memberName = obj.optString("memberName","");
        // naslovi mogu biti JSON array
        r.titles = new ArrayList<>();
        if(obj.has("titles")) {
            JSONArray arr = obj.getJSONArray("titles");
            for(int i=0; i<arr.length(); i++) {
                r.titles.add(arr.getString(i));
            }
        }
        r.approved = obj.optBoolean("approved", false);
        return r;
    }


    private static class ReservationData {
        int id;
        String memberName;
        List<String> titles;
        boolean approved;
    }

    private class ReservationTableModel extends AbstractTableModel {
        private List<ReservationData> data = new ArrayList<>();
        private final String[] colNames = {"ID", "Member", "Titles"};

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
            ReservationData r = data.get(rowIndex);
            switch(columnIndex) {
                case 0: return r.id;
                case 1: return r.memberName;
                case 2: 
                    // spoji naslove
                    return String.join(", ", r.titles);
                default:
                    return "";
            }
        }
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
        public void setData(List<ReservationData> newData) {
            this.data = newData;
            fireTableDataChanged();
        }
        public ReservationData getRow(int row) {
            return data.get(row);
        }
    }
}
