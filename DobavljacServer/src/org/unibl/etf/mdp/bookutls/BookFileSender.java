package org.unibl.etf.mdp.bookutls;

import java.io.*;
import java.util.logging.Level;

import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;

public class BookFileSender {
	
    public static void sendBookFile(String bookTitle, DataOutputStream out) {
        if (bookTitle == null || bookTitle.isEmpty()) {
            return;
        }
        String folderPath = ConfigLoader.getProperty("BOOK_FILES_PATH");
        File fileToSend = new File(folderPath, bookTitle);

        if (!fileToSend.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(fileToSend)) {
            long fileSize = fileToSend.length();
            out.writeLong(fileSize);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška pri slanju fajla: " + bookTitle, e);
        }
    }
}
