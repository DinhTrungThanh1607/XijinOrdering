package com.foodorder.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class CartItem {
    private final Item item;
    private final SimpleIntegerProperty amount;

    public CartItem(Item item, int amount) {
        this.item = item;
        this.amount = new SimpleIntegerProperty(amount);
    }

    public Item getItem() {
        return item;
    }

    public String getItemId() {
        return item.getItemId();
    }

    public String getName() {
        return item.getName();
    }

    public double getPrice() {
        return item.getPrice();
    }

    public int getAmount() {
        return amount.get();
    }

    public void setAmount(int amount) {
        this.amount.set(amount);
    }

    public SimpleIntegerProperty amountProperty() {
        return amount;
    }

    public double getSubtotal() {
        return item.getPrice() * getAmount();
    }
}
