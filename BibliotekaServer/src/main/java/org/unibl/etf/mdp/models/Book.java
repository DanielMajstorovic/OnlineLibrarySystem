package org.unibl.etf.mdp.models;

public class Book {
    
    private String title;
    private String author;
    private String publishDate;
    private String language;
    
    private String text;
    
    private int quantity;
    
    private String coverImage;

    public Book() {
    }

    public Book(String title, String author, String publishDate, String language, String text, int quantity, String coverImage) {
        this.title = title;
        this.author = author;
        this.publishDate = publishDate;
        this.language = language;
        this.text = text;
        this.quantity = quantity;
        this.coverImage = coverImage;
    }

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
    
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCoverImage() {
        return coverImage;
    }
    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    @Override
    public String toString() {
        return "Book{" +
                "title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", publishDate='" + publishDate + '\'' +
                ", language='" + language + '\'' +
                ", text='" + (text != null ? (text.length() > 30 ? text.substring(0,30) + "..." : text) : "null") + '\'' +
                ", quantity=" + quantity +
                ", coverImage='" + (coverImage != null ? "BASE64_STRING(length=" + coverImage.length() + ")" : "null") + '\'' +
                '}';
    }
}
