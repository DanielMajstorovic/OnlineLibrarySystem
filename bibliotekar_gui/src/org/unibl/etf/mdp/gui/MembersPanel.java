package org.unibl.etf.mdp.gui;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.*;

import org.json.*;
import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.data.ClanData;
import org.unibl.etf.mdp.logger.AppLogger;

public class MembersPanel extends JPanel {

	
	private static final Logger logger = AppLogger.getLogger();

    private static final String GET_ALL_USERS_URL = ConfigLoader.getProperty("api.get_all_users");
    private static final String REGISTER_USER_URL = ConfigLoader.getProperty("api.register_user");
    private static final String ENABLE_USER_URL = ConfigLoader.getProperty("api.enable_user");
    private static final String DISABLE_USER_URL = ConfigLoader.getProperty("api.disable_user");
    private static final String DELETE_USER_URL = ConfigLoader.getProperty("api.delete_user");
    private static final String UPDATE_USER_URL = ConfigLoader.getProperty("api.update_user");

    private JTable table;
    private MemberTableModel tableModel;

    private JButton btnEnable, btnDisable, btnDelete, btnUpdate, btnAdd;

    public MembersPanel() {
        setLayout(new BorderLayout(10, 10));

        tableModel = new MemberTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnEnable = new JButton("Omogući");
        btnDisable = new JButton("Onemogući");
        btnDelete = new JButton("Briši");
        btnUpdate = new JButton("Ažuriraj");
        btnAdd = new JButton("Dodaj");

        topPanel.add(btnEnable);
        topPanel.add(btnDisable);
        topPanel.add(btnDelete);
        topPanel.add(btnUpdate);
        topPanel.add(btnAdd);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        
        JButton btnRefresh = new JButton("Osveži");
        add(btnRefresh, BorderLayout.SOUTH);

        
        btnRefresh.addActionListener(e -> loadAllMembers());
        btnEnable.addActionListener(e -> enableSelectedMember());
        btnDisable.addActionListener(e -> disableSelectedMember());
        btnDelete.addActionListener(e -> deleteSelectedMember());
        btnUpdate.addActionListener(e -> updateSelectedMember());
        btnAdd.addActionListener(e -> addNewMember());

        
        loadAllMembers();
    }

    private void loadAllMembers() {
        SwingUtilities.invokeLater(() -> {
            try {
                URL url = new URL(GET_ALL_USERS_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
              
                    String response = readResponse(conn.getInputStream());
                    JSONArray jsonArr = new JSONArray(response);

                    List<ClanData> newData = new ArrayList<>();
                    for (int i = 0; i < jsonArr.length(); i++) {
                        JSONObject obj = jsonArr.getJSONObject(i);

                        ClanData clan = new ClanData();
                        clan.setUsername(obj.optString("username"));
                        clan.setFirstName(obj.optString("firstName"));
                        clan.setLastName(obj.optString("lastName"));
                        clan.setEmail(obj.optString("email"));
                        clan.setAddress(obj.optString("address"));
                        clan.setPassword(obj.optString("password"));
                        clan.setApproved(obj.optBoolean("approved"));

                        newData.add(clan);
                    }
                    tableModel.setData(newData);
                } else {
                    JOptionPane.showMessageDialog(this, "Greška prilikom učitavanja članova. Kod: " + responseCode,
                            "Greška", JOptionPane.ERROR_MESSAGE);
                }
                conn.disconnect();
            } catch (Exception e) {
            	logger.severe("Greška prilikom konekcije na server: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Greška prilikom konekcije na server:\n" + e.getMessage(),
                        "Greška", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void enableSelectedMember() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite člana iz tabele.", "Upozorenje",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = (String) tableModel.getValueAt(selectedRow, 0);
        String urlStr = ENABLE_USER_URL + username;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                // Uspeh
                loadAllMembers();
                JOptionPane.showMessageDialog(this, "Korisnik omogućen.");
            } else {
                JOptionPane.showMessageDialog(this, "Greška: " + readResponse(conn.getErrorStream()));
            }
            conn.disconnect();
        } catch (Exception e) {
        	logger.severe("Greška: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
        }
    }


    private void disableSelectedMember() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite člana iz tabele.", "Upozorenje",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = (String) tableModel.getValueAt(selectedRow, 0);
        String urlStr = DISABLE_USER_URL + username;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                loadAllMembers();
                JOptionPane.showMessageDialog(this, "Korisnik onemogućen.");
            } else {
                JOptionPane.showMessageDialog(this, "Greška: " + readResponse(conn.getErrorStream()));
            }
            conn.disconnect();
        } catch (Exception e) {
        	logger.severe("Greška: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
        }
    }


    private void deleteSelectedMember() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite člana iz tabele.", "Upozorenje",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String username = (String) tableModel.getValueAt(selectedRow, 0);

        int choice = JOptionPane.showConfirmDialog(this,
                "Da li ste sigurni da želite obrisati člana: " + username + "?",
                "Brisanje", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String urlStr = DELETE_USER_URL + username;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                loadAllMembers();
                JOptionPane.showMessageDialog(this, "Korisnik obrisan.");
            } else {
                JOptionPane.showMessageDialog(this, "Greška: " + readResponse(conn.getErrorStream()));
            }
            conn.disconnect();
        } catch (Exception e) {
        	logger.severe("Greška: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
        }
    }

    private void updateSelectedMember() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Odaberite člana iz tabele.", "Upozorenje",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        ClanData clan = tableModel.getRowData(selectedRow);

        UpdateMemberDialog dialog = new UpdateMemberDialog(SwingUtilities.getWindowAncestor(this), clan);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            
            ClanData updated = dialog.getUpdatedClanData();
            String urlStr = UPDATE_USER_URL + updated.getUsername();
            
            JSONObject json = new JSONObject();
            json.put("firstName", updated.getFirstName());
            json.put("lastName", updated.getLastName());
            json.put("email", updated.getEmail());
            json.put("address", updated.getAddress());
            json.put("password", updated.getPassword());
            //json.put("approved", updated.isApproved());

            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes());
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    loadAllMembers();
                    JOptionPane.showMessageDialog(this, "Podaci korisnika ažurirani.");
                } else {
                    JOptionPane.showMessageDialog(this, "Greška: " + readResponse(conn.getErrorStream()));
                }
                conn.disconnect();
            } catch (Exception e) {
            	logger.severe("Greška: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
            }
        }
    }


    private void addNewMember() {
        AddMemberDialog dialog = new AddMemberDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            ClanData clan = dialog.getNewClanData();
            
            JSONObject json = new JSONObject();
            json.put("username", clan.getUsername());
            json.put("firstName", clan.getFirstName());
            json.put("lastName", clan.getLastName());
            json.put("email", clan.getEmail());
            json.put("address", clan.getAddress());
            json.put("password", clan.getPassword());
            json.put("approved", clan.isApproved());

            try {
                URL url = new URL(REGISTER_USER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes());
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    loadAllMembers();
                    JOptionPane.showMessageDialog(this, "Novi član uspješno registrovan.");
                } else if (responseCode == 409) {
                    
                    JOptionPane.showMessageDialog(this, "Korisničko ime već postoji. (HTTP 409)");
                } else {
                    JOptionPane.showMessageDialog(this, "Greška prilikom registracije: "
                            + readResponse(conn.getErrorStream()));
                }
                conn.disconnect();
            } catch (Exception e) {
            	logger.severe("Greška: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Greška: " + e.getMessage());
            }
        }
    }

    private String readResponse(InputStream is) {
        if (is == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
        	logger.severe("Greška: " + e.getMessage());
            return "";
        }
    }
}
