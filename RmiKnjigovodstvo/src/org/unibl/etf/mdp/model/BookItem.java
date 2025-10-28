package org.unibl.etf.mdp.model;

import java.io.Serializable;

public class BookItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String title;
    private int quantity;
    
    public BookItem() {
    }

    public BookItem(String title, int quantity) {
        this.title = title;
        this.quantity = quantity;
    }

    public String getTitle() {
        return title;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
