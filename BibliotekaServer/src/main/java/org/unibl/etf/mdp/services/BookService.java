package org.unibl.etf.mdp.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.unibl.etf.mdp.logger.AppLogger;
import org.unibl.etf.mdp.models.Book;
import org.unibl.etf.mdp.models.Reservation;
import org.unibl.etf.mdp.redis.RedisManager;

import com.google.gson.Gson;

import redis.clients.jedis.Jedis;

public class BookService {

    private static final String ALL_TITLES_KEY = "books:titles";
    private static final Gson gson = new Gson();
    public static List<Reservation> reservations = new ArrayList<>();
    private static int nextReservationId = 1;
      
    public Reservation addReservation(String memberName, List<String> titles) {
        Reservation r = new Reservation(nextReservationId++, memberName, titles);
        reservations.add(r);
        AppLogger.getLogger().info("Dodata rezervacija: " + r);
        return r;
    }

    public List<Reservation> getAllReservations() {
        return reservations;
    }

    public Reservation getReservationById(int id) {
        for (Reservation r : reservations) {
            if (r.getId() == id) {
                return r;
            }
        }
        return null;
    }

    public boolean removeReservation(int id) {
        return reservations.removeIf(r -> r.getId() == id);
    }

    public Book getFullBook(String title) {
        Book b = getBookByTitle(title);
        if (b == null) {
            b = new Book(title, "Nepoznat", "?", "?", "Placeholder text", 1, null);
        }
        return b;
    }
    
    public static void addReservation(Reservation r) {
        reservations.add(r);
        AppLogger.getLogger().info("Dodana rezervacija: " + r);
    }


    public void saveBook(Book book) {
        String title = book.getTitle();
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }

        String key = makeKey(title);

        try (Jedis jedis = RedisManager.getInstance().getJedis()) {
            String json = gson.toJson(book);
            jedis.set(key, json);

            jedis.sadd(ALL_TITLES_KEY, title);
        }
        AppLogger.getLogger().info("Knjiga snimljena/azurirana: " + title);
    }


    public boolean exists(String title) {
        try (Jedis jedis = RedisManager.getInstance().getJedis()) {
            String key = makeKey(title);
            return jedis.exists(key);
        }
    }

    public Book getBookByTitle(String title) {
        try (Jedis jedis = RedisManager.getInstance().getJedis()) {
            String key = makeKey(title);
            String json = jedis.get(key);
            if (json == null) {
                return null;
            }
            return gson.fromJson(json, Book.class);
        }
    }


    public boolean deleteBookByTitle(String title) {
        String key = makeKey(title);
        try (Jedis jedis = RedisManager.getInstance().getJedis()) {
            if (!jedis.exists(key)) {
                return false;
            }
            jedis.del(key);
            jedis.srem(ALL_TITLES_KEY, title);
        }
        return true;
    }

 
    public List<Book> getAllBooks() {
        List<Book> result = new ArrayList<>();
        try (Jedis jedis = RedisManager.getInstance().getJedis()) {
            Set<String> allTitles = jedis.smembers(ALL_TITLES_KEY);
            for (String t : allTitles) {
                String key = makeKey(t);
                String json = jedis.get(key);
                if (json != null) {
                    Book b = gson.fromJson(json, Book.class);
                    result.add(b);
                }
            }
        }
        return result;
    }

    private String makeKey(String title) {
    	return "book:" + title.replaceAll("\\s+", "_");

    }
}
