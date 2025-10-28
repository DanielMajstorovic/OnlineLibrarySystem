package org.unibl.etf.mdp.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.unibl.etf.mdp.logger.AppLogger;

public class ConfigLoader {
    private static final Logger logger = AppLogger.getLogger();
    private static final Properties properties = new Properties();
    //private static final String CONFIG_FILE_PATH = "C:\\Users\\Dell\\Desktop\\MDP\\Daniel_Majstorovic_1176_21_MDP_PZ\\dobavljac_gui\\dobavljac_gui.properties";
    private static final String CONFIG_FILE_PATH = "dobavljac_gui.properties";
    
    static {
        loadProperties();     
    }

    private static void loadProperties() {
        try (FileInputStream fis = new FileInputStream(new File(CONFIG_FILE_PATH))) {
            properties.load(fis);
            //logger.info("Properties fajl uspešno učitan: " + CONFIG_FILE_PATH);
        } catch (IOException e) {
            logger.severe("Greška pri učitavanju properties fajla: " + CONFIG_FILE_PATH + " | " + e.getMessage());
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
