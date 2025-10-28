package org.unibl.etf.mdp.bookutls;

import org.imgscalr.Scalr;
import org.unibl.etf.mdp.config.ConfigLoader;
import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.model.BookData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

public class BookLoader {

    public static List<BookData> loadAllBooks() {
        List<String> links = readLinksFromFile();
        List<BookData> books = new ArrayList<>();

        for (String link : links) {
            books.add(parseBookFromLink(link));
        }
        return books;
    }

    private static List<String> readLinksFromFile() {
        List<String> linkList = new ArrayList<>();
        String linkFilePath = ConfigLoader.getProperty("LINK_FILE_PATH");
        try (BufferedReader br = new BufferedReader(new FileReader(linkFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                linkList.add(line.trim());
            }
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška pri čitanju link fajla: ", e);
        }
        return linkList;
    }

    private static BookData parseBookFromLink(String link) {
        BookData book = new BookData();
        try {
        	
            URL url = new URL(link);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder textContent = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
//                if (inputLine.startsWith("*** START OF THE PROJECT GUTENBERG EBOOK")) {
//                    break;
//                }
                textContent.append(inputLine).append("\n");

                if (inputLine.startsWith("Title:")) {
                    book.setTitle(extractValue(inputLine));
                } else if (inputLine.startsWith("Author:")) {
                    book.setAuthor(extractValue(inputLine));
                } else if (inputLine.startsWith("Release date:")) {
                    book.setPublishDate(extractValue(inputLine));
                } else if (inputLine.startsWith("Language:")) {
                    book.setLanguage(extractValue(inputLine));
                }
            }
            in.close();

            saveBookFileLocally(book.getTitle(), textContent.toString());

            BufferedImage cover = loadCoverImage(link);
            book.setCoverImage(cover);

        } catch (MalformedURLException e) {
            AppLogger.getLogger().log(Level.WARNING, "Nevažeći URL: " + link, e);
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška pri čitanju sadržaja linka: " + link, e);
        }
        return book;
    }


    private static String extractValue(String line) {
        String[] parts = line.split(": ", 2);
        if (parts.length == 2) {
            return parts[1].trim();
        }
        return "";
    }

    private static void saveBookFileLocally(String title, String content) {
        if (title == null || title.isEmpty()) {
            return;
        }
        String folderPath = ConfigLoader.getProperty("BOOK_FILES_PATH");
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File outFile = new File(folder, title);

        try (PrintWriter pw = new PrintWriter(outFile)) {
            pw.println(content);
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška pri snimanju fajla za knjigu: " + title, e);
        }
    }


    private static BufferedImage loadCoverImage(String bookLink) {
        String textExt = ".txt";
        String imgExt  = ".cover.medium.jpg";

        String imageUrlString = bookLink.replace(textExt, imgExt);

        try {
            URL imageUrl = new URL(imageUrlString);
            BufferedImage rawImage = ImageIO.read(imageUrl);

            if (rawImage != null) {
                return Scalr.resize(rawImage, 100, 112);
            }
        } catch (IOException e) {
            AppLogger.getLogger().log(Level.WARNING, "Greška pri učitavanju slike: " + imageUrlString, e);
        }
        return null;
    }
}
