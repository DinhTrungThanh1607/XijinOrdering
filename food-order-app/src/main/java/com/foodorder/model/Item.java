package com.foodorder.model;

public class Item {
    private final String itemId;
    private final String name;
    private final double price;
    private final ItemType type;

    public Item(String itemId, String name, double price, ItemType type) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.type = type;
    }

    public String getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public ItemType getType() {
        return type;
    }

    @Override
    public String toString() {
        return name + " (" + itemId + ") - " + price;
    }
}
