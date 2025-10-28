package org.unibl.etf.mdp.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Invoice implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String supplierName;
    private LocalDateTime dateTime;
    private List<BookItem> books;
    private double totalPrice; 

    public Invoice() {
    }

    public Invoice(String supplierName, LocalDateTime dateTime, List<BookItem> books, double totalPrice) {
        this.supplierName = supplierName;
        this.dateTime = dateTime;
        this.books = books;
        this.totalPrice = totalPrice;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public List<BookItem> getBooks() {
        return books;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public void setBooks(List<BookItem> books) {
        this.books = books;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
}
