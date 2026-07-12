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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

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

    private final ObservableList<Item> catalog = FXCollections.observableArrayList();

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
    private void handleRemoveMenuItem() {
        Item selected = catalogTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Please select an item in the menu first.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove \"" + selected.getName() + "\" (" + selected.getItemId() + ") from the menu?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            catalog.remove(selected);
        }
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

    @FXML
    private void handleAddNewItem() {
        Dialog<Item> dialog = new Dialog<>();
        dialog.setTitle("Add New Item to Menu");
        dialog.setHeaderText("Enter the details of the new item");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Grilled Chicken");
        TextField priceField = new TextField();
        priceField.setPromptText("e.g. 45000");
        ComboBox<ItemType> typeBox = new ComboBox<>(FXCollections.observableArrayList(ItemType.values()));
        Label idPreviewLabel = new Label();
        idPreviewLabel.setStyle("-fx-font-weight: bold;");

        typeBox.valueProperty().addListener((obs, oldV, newV) ->
                idPreviewLabel.setText(generateNextId(newV)));
        typeBox.setValue(ItemType.FOOD);
        idPreviewLabel.setText(generateNextId(typeBox.getValue()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeBox, 1, 0);
        grid.add(new Label("Item ID:"), 0, 1);
        grid.add(idPreviewLabel, 1, 1);
        grid.add(new Label("Name:"), 0, 2);
        grid.add(nameField, 1, 2);
        grid.add(new Label("Price (VND):"), 0, 3);
        grid.add(priceField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        // Disable "Add" until the required fields are filled in.
        Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        Runnable validate = () -> addButton.setDisable(
                nameField.getText().trim().isEmpty() || priceField.getText().trim().isEmpty());
        nameField.textProperty().addListener((obs, oldV, newV) -> validate.run());
        priceField.textProperty().addListener((obs, oldV, newV) -> validate.run());

        dialog.setResultConverter(buttonType -> {
            if (buttonType == addButtonType) {
                String name = nameField.getText().trim();
                String priceText = priceField.getText().trim();

                double price;
                try {
                    price = Double.parseDouble(priceText);
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR, "Price must be a valid number.");
                    return null;
                }
                if (price <= 0) {
                    showAlert(Alert.AlertType.ERROR, "Price must be greater than 0.");
                    return null;
                }

                // Recompute the ID at confirm time in case anything changed.
                String id = generateNextId(typeBox.getValue());
                return new Item(id, name, price, typeBox.getValue());
            }
            return null;
        });

        Optional<Item> result = dialog.showAndWait();
        result.ifPresent(item -> {
            catalog.add(item);
            catalogTable.getSelectionModel().select(item);
            catalogTable.scrollTo(item);
        });
    }

    /**
     * Generates the next itemId for a given type, e.g. FOOD -> F001, F002, ...
     * DRINK -> D001, D002, ... OTHER -> O001, O002, ...
     * Scans the current catalog for the highest existing number with that
     * type's prefix and returns prefix + (max + 1), zero-padded to 3 digits.
     */
    private String generateNextId(ItemType type) {
        String prefix = switch (type) {
            case FOOD -> "F";
            case DRINK -> "D";
            case OTHER -> "O";
        };

        int maxNum = 0;
        for (Item item : catalog) {
            String id = item.getItemId();
            if (id != null && id.startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(id.substring(prefix.length()));
                    maxNum = Math.max(maxNum, num);
                } catch (NumberFormatException ignored) {
                    // itemId with that prefix but non-numeric suffix; skip it
                }
            }
        }
        return prefix + String.format("%03d", maxNum + 1);
    }

    private void recalcTotal() {
        double total = cart.stream().mapToDouble(CartItem::getSubtotal).sum();
        totalLabel.setText(String.format("%,.0f VND", total));
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.showAndWait();
    }
}
