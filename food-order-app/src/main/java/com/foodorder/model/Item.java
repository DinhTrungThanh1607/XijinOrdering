package com.foodorder.model;

public class Item {
    private final String itemId;
    private final String name;
    private final double price;
    private final ItemType type;
    private final String imagePath;  // local file path to the item's picture, or null if none
    private final String description; // short description shown in the item detail popup

    public Item(String itemId, String name, double price, ItemType type) {
        this(itemId, name, price, type, null, "");
    }

    public Item(String itemId, String name, double price, ItemType type, String imagePath) {
        this(itemId, name, price, type, imagePath, "");
    }

    public Item(String itemId, String name, double price, ItemType type, String imagePath, String description) {
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.type = type;
        this.imagePath = imagePath;
        this.description = description == null ? "" : description;
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

    public String getImagePath() {
        return imagePath;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name + " (" + itemId + ") - " + price;
    }
}