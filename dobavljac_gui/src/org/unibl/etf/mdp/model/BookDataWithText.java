package org.unibl.etf.mdp.model;

import java.io.Serializable;

public class BookDataWithText implements Serializable {
    private static final long serialVersionUID = 1L;

    private String title;
    private String author;
    private String publishDate;
    private String language;

    private byte[] coverImageData;

    private String bookText;

    public BookDataWithText() {
    }

    public String getTitle() {
        return title;
    }
    public String getAuthor() {
        return author;
    }
    public String getPublishDate() {
        return publishDate;
    }
    public String getLanguage() {
        return language;
    }
    public byte[] getCoverImageData() {
        return coverImageData;
    }
    public String getBookText() {
        return bookText;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }
    public void setLanguage(String language) {
        this.language = language;
    }
    public void setCoverImageData(byte[] coverImageData) {
        this.coverImageData = coverImageData;
    }
    public void setBookText(String bookText) {
        this.bookText = bookText;
    }
}
