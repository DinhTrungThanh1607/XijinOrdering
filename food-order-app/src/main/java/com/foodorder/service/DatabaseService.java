package com.foodorder.service;

import com.foodorder.model.Item;
import com.foodorder.model.ItemType;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes menu items to a local SQLite database file
 * ("xijinordering.db"), so the menu survives closing and reopening the app.
 */
public class DatabaseService {

    private static final String DB_URL = "jdbc:sqlite:xijinordering.db";

    public DatabaseService() {
        initTable();
    }

    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS items (" +
                "item_id TEXT PRIMARY KEY," +
                "name TEXT NOT NULL," +
                "price REAL NOT NULL," +
                "type TEXT NOT NULL," +
                "image_path TEXT," +
                "description TEXT" +
                ")";
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // If the table already existed from before this field was added,
        // add the column now. Ignore the error if it's already there.
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE items ADD COLUMN description TEXT");
        } catch (SQLException ignored) {
            // column already exists — nothing to do
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Loads every saved menu item, ordered by item ID (F001, F002, D001, ...).
     */
    public List<Item> loadAllItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT item_id, name, price, type, image_path, description FROM items ORDER BY item_id";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(new Item(
                        rs.getString("item_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        ItemType.valueOf(rs.getString("type")),
                        rs.getString("image_path"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public void insertItem(Item item) {
        String sql = "INSERT INTO items (item_id, name, price, type, image_path, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getItemId());
            ps.setString(2, item.getName());
            ps.setDouble(3, item.getPrice());
            ps.setString(4, item.getType().toString());
            ps.setString(5, item.getImagePath());
            ps.setString(6, item.getDescription());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteItem(String itemId) {
        String sql = "DELETE FROM items WHERE item_id = ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}