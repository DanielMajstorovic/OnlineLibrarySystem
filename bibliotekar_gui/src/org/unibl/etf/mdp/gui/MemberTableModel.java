package org.unibl.etf.mdp.gui;

import java.util.*;

import javax.swing.table.*;

import org.unibl.etf.mdp.data.ClanData;

public class MemberTableModel extends AbstractTableModel {

    private List<ClanData> data = new ArrayList<>();

    private final String[] columnNames = {
            "Username", "First Name", "Last Name", "Email", "Address", "Password", "Approved"
    };

    public void setData(List<ClanData> newData) {
        this.data = newData;
        fireTableDataChanged();
    }

    public ClanData getRowData(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= data.size())
            return null;
        return data.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ClanData clan = data.get(rowIndex);
        switch (columnIndex) {
            case 0: return clan.getUsername();
            case 1: return clan.getFirstName();
            case 2: return clan.getLastName();
            case 3: return clan.getEmail();
            case 4: return clan.getAddress();
            case 5: return clan.getPassword();
            case 6: return clan.isApproved();
            default: return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 6) { // approved
            return Boolean.class;
        }
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
}
