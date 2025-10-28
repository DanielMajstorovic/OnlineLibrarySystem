package org.unibl.etf.mdp.repositories;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.models.Clan;

public class ClanRepository {
    private static final Logger logger = AppLogger.getLogger();
    private static String FILE_PATH = System.getProperty("user.dir") + File.separator + ConfigLoader.getProperty("users.path");
    
    static {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            logger.info("XML fajl ne postoji. Kreiram novi sa početnim podacima...");
            initializeDefaultUsers();
        }
    }

    public static List<Clan> getAllUsers() {
        List<Clan> clanovi = new ArrayList<>();

        try (XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(FILE_PATH)))) {
            while (true) {
                try {
                    Clan clan = (Clan) decoder.readObject();
                    clanovi.add(clan);
                } catch (Exception e) {
                    break;
                }
            }
        } catch (Exception e) {
            logger.severe("Greška pri učitavanju korisnika iz XML-a: " + e.getMessage());
        }

        return clanovi;
    }

    public static void saveAllUsers(List<Clan> clanovi) {
        try (XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(FILE_PATH)))) {
            for (Clan c : clanovi) {
                encoder.writeObject(c);
            }
            //logger.info("Lista članova uspešno sačuvana u XML fajl (upisano " + clanovi.size() + " članova).");
        } catch (Exception e) {
            logger.severe("Greška pri upisu korisnika u XML: " + e.getMessage());
        }
    }

    private static void initializeDefaultUsers() {
        List<Clan> clanovi = new ArrayList<>();
        clanovi.add(new Clan("Daniel", "Majstorovic", "Adresa 1", "danielmajstorovic033@gmail.com", "daniel", "12345678", true));
        clanovi.add(new Clan("Ana", "Anić", "Adresa 2", "ana@email.com", "ana", "12345678", true));
        clanovi.add(new Clan("Ivan", "Ivić", "Adresa 3", "ivan@email.com", "ivan", "12345678", false));

        saveAllUsers(clanovi);
    }
}
