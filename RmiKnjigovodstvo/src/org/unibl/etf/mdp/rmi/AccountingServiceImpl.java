package org.unibl.etf.mdp.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.model.*;

public class AccountingServiceImpl extends UnicastRemoteObject implements IAccountingService {

    private static final long serialVersionUID = 1L;

    protected AccountingServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public double processInvoice(Invoice invoice) throws RemoteException {
        AppLogger.getLogger().info("Primio račun od dobavljača: " + invoice.getSupplierName());

        String baseFolder = ConfigLoader.getProperty("invoices.folder");
        if (baseFolder == null || baseFolder.trim().isEmpty()) {
            baseFolder = "invoices"; 
        }

        File supplierDir = new File(baseFolder, invoice.getSupplierName());
        if (!supplierDir.exists()) {
            supplierDir.mkdirs();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String dateString = invoice.getDateTime().format(formatter);
        File invoiceFile = new File(supplierDir, "Invoice_" + dateString + ".json");

        JSONObject json = new JSONObject();
        json.put("supplierName", invoice.getSupplierName());
        json.put("dateTime", invoice.getDateTime().toString());
        json.put("totalPrice", invoice.getTotalPrice());

        JSONArray booksArray = new JSONArray();
        if (invoice.getBooks() != null) {
            for (BookItem bi : invoice.getBooks()) {
                JSONObject b = new JSONObject();
                b.put("title", bi.getTitle());
                b.put("quantity", bi.getQuantity());
                booksArray.put(b);
            }
        }
        json.put("books", booksArray);

        try (FileWriter fw = new FileWriter(invoiceFile)) {
            fw.write(json.toString(4)); 
            AppLogger.getLogger().info("Račun snimljen: " + invoiceFile.getAbsolutePath());
        } catch (Exception e) {
            AppLogger.getLogger().severe("Greška pri snimanju računa: " + e.getMessage());
        }

        double pdv = invoice.getTotalPrice() * 0.17;
        AppLogger.getLogger().info("PDV za ovaj račun iznosi: " + pdv);

        return pdv;
    }
}
