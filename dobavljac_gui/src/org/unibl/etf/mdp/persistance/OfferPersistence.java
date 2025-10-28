package org.unibl.etf.mdp.persistance;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;

public class OfferPersistence {

    private static final String OFFERS_FOLDER = ConfigLoader.getProperty("offers.folder");

    public static Set<String> loadOffersForSupplier(String supplierName) {
        Set<String> result = new HashSet<>();
        File folder = new File(OFFERS_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File offerFile = new File(folder, "offers_" + supplierName + ".txt");
        if (!offerFile.exists()) {
            return result; 
        }

        try (BufferedReader br = new BufferedReader(new FileReader(offerFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    result.add(line.trim());
                }
            }
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Greska pri ucitavanju ponude: ", e);
        }
        return result;
    }

    public static void saveOffersForSupplier(String supplierName, Set<String> offers) {
        File folder = new File(OFFERS_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File offerFile = new File(folder, "offers_" + supplierName + ".txt");

        try (PrintWriter pw = new PrintWriter(new FileWriter(offerFile))) {
            for (String title : offers) {
                pw.println(title);
            }
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Greska pri snimanju ponude: ", e);
        }
    }
}
