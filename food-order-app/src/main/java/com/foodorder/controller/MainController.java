package com.foodorder.controller;

import com.foodorder.model.CartItem;
import com.foodorder.model.Item;
import com.foodorder.model.ItemType;
import com.foodorder.service.GoogleSheetsService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    // ==== CONFIG: replace with your own Google Sheet details ====
    private static final String SPREADSHEET_ID = "1U6Fzdi4HOnM-d54bbWC4lEPt5qW0fPWElZ7t26c8C0o";
    private static final String SHEET_NAME = "Invoices";

    @FXML private TableView<Item> catalogTable;
    @FXML private TableColumn<Item, String> catalogIdCol;
    @FXML private TableColumn<Item, String> catalogNameCol;
    @FXML private TableColumn<Item, Double> catalogPriceCol;
    @FXML private TableColumn<Item, ItemType> catalogTypeCol;

    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> cartIdCol;
    @FXML private TableColumn<CartItem, String> cartNameCol;
    @FXML private TableColumn<CartItem, Double> cartPriceCol;
    @FXML private TableColumn<CartItem, Integer> cartAmountCol;
    @FXML private TableColumn<CartItem, Double> cartSubtotalCol;

    @FXML private Spinner<Integer> qtySpinner;
    @FXML private Label totalLabel;

    private final ObservableList<Item> catalog = FXCollections.observableArrayList(
            new Item("F001", "Fried Rice", 35000, ItemType.FOOD),
            new Item("F002", "Pho", 40000, ItemType.FOOD),
            new Item("F003", "Spring Rolls", 25000, ItemType.FOOD),
            new Item("D001", "Iced Coffee", 20000, ItemType.DRINK),
            new Item("D002", "Milk Tea", 25000, ItemType.DRINK),
            new Item("D003", "Orange Juice", 22000, ItemType.DRINK),
            new Item("O001", "Extra Napkins", 2000, ItemType.OTHER),
            new Item("O002", "Take-away Box", 3000, ItemType.OTHER)
    );

    private final ObservableList<CartItem> cart = FXCollections.observableArrayList();
    private final GoogleSheetsService sheetsService = new GoogleSheetsService(SPREADSHEET_ID, SHEET_NAME);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Catalog table bindings
        catalogIdCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        catalogNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        catalogPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        catalogTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        catalogTable.setItems(catalog);

        // Cart table bindings
        cartIdCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        cartNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        cartPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        cartAmountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        cartSubtotalCol.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        cartTable.setItems(cart);

        // Quantity spinner
        qtySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));

        recalcTotal();
    }

    @FXML
    private void handleAddToCart() {
        Item selected = catalogTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select an item first.");
            return;
        }
        int qty = qtySpinner.getValue();

        for (CartItem ci : cart) {
            if (ci.getItemId().equals(selected.getItemId())) {
                ci.setAmount(ci.getAmount() + qty);
                recalcTotal();
                return;
            }
        }
        cart.add(new CartItem(selected, qty));
        recalcTotal();
    }

    @FXML
    private void handleRemoveFromCart() {
        CartItem selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            cart.remove(selected);
            recalcTotal();
        }
    }

    @FXML
    private void handleCheckout() {
        if (cart.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cart is empty.");
            return;
        }

        double total = cart.stream().mapToDouble(CartItem::getSubtotal).sum();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("Confirm payment of %,.0f VND?", total));
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        // Snapshot the cart being paid, then clear it immediately so the UI
        // feels responsive while the upload happens in the background.
        List<CartItem> paidItems = new ArrayList<>(cart);
        cart.clear();
        recalcTotal();

        Thread uploadThread = new Thread(() -> {
            try {
                sheetsService.uploadInvoice(paidItems, total);
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.INFORMATION, "Payment successful. Invoice uploaded to Google Sheets."));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Payment recorded locally, but upload to Google Sheets failed:\n" + ex.getMessage()));
            }
        });
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    private void recalcTotal() {
        double total = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
        totalLabel.setText(String.format("Total: %,.0f VND", total));
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.showAndWait();
    }
}
