package org.unibl.etf.mdp.model;

import java.awt.image.BufferedImage;

public class BookData {
    private String title;
    private String author;
    private String publishDate;
    private String language;

    private transient BufferedImage coverImage;

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getPublishDate() {
        return publishDate;
    }
    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }
    public String getLanguage() {
        return language;
    }
    public void setLanguage(String language) {
        this.language = language;
    }
    public BufferedImage getCoverImage() {
        return coverImage;
    }
    public void setCoverImage(BufferedImage coverImage) {
        this.coverImage = coverImage;
    }
}
