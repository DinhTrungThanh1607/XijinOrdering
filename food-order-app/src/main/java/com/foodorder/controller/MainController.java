package com.foodorder.controller;

import com.foodorder.model.CartItem;
import com.foodorder.model.Item;
import com.foodorder.model.ItemType;
import com.foodorder.service.ConfigService;
import com.foodorder.service.DatabaseService;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private final ConfigService configService = new ConfigService();
    private final DatabaseService databaseService = new DatabaseService();

    @FXML private TableView<Item> catalogTable;
    @FXML private TableColumn<Item, String> catalogImageCol;
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
    @FXML private Label cartCountLabel;
    @FXML private Label cartSubtotalLabel;

    private final ObservableList<Item> catalog = FXCollections.observableArrayList();

    private final ObservableList<CartItem> cart = FXCollections.observableArrayList();
    private final GoogleSheetsService sheetsService =
            new GoogleSheetsService(configService.getSpreadsheetId(), configService.getSheetName());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Catalog table bindings
        catalogImageCol.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
        catalogImageCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Item rowItem = getTableView().getItems().get(getIndex());
                javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane();
                wrapper.setStyle("-fx-cursor: hand;");
                wrapper.setOnMouseClicked(e -> showItemDetailDialog(rowItem));

                if (imagePath != null && !imagePath.isBlank() && new java.io.File(imagePath).exists()) {
                    ImageView view = new ImageView(new Image(
                            new java.io.File(imagePath).toURI().toString(), 34, 34, true, true));
                    javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(34, 34);
                    clip.setArcWidth(10);
                    clip.setArcHeight(10);
                    view.setClip(clip);
                    view.setMouseTransparent(true); // let clicks pass through to the wrapper
                    wrapper.getChildren().add(view);
                } else if (rowItem != null) {
                    Label fallback = new Label(typeIcon(rowItem.getType()));
                    fallback.setStyle("-fx-font-size: 18px;");
                    fallback.setMouseTransparent(true);
                    wrapper.getChildren().add(fallback);
                } else {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                setGraphic(wrapper);
                setText(null);
            }
        });
        catalogIdCol.setCellValueFactory(new PropertyValueFactory<>("itemId"));
        catalogNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        catalogPriceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        catalogTypeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        catalogTypeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ItemType type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(typeIcon(type) + " " + type);
                    badge.getStyleClass().add("type-badge");
                    switch (type) {
                        case FOOD -> badge.getStyleClass().add("type-badge-food");
                        case DRINK -> badge.getStyleClass().add("type-badge-drink");
                        case OTHER -> badge.getStyleClass().add("type-badge-other");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        catalogTable.setItems(catalog);

        // Load previously saved menu items from the local database.
        catalog.addAll(databaseService.loadAllItems());

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
    private void handleOpenSettings() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Google Sheets Settings");
        dialog.setHeaderText("Configure where invoices get uploaded");
        applyAppStyle(dialog);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField spreadsheetIdField = new TextField(sheetsService.getSpreadsheetId());
        spreadsheetIdField.setPromptText("e.g. 1U6Fzdi4HOnM-d54bbWC4lEPt5qW0fPWElZ7t26c8C0o");
        spreadsheetIdField.setPrefWidth(320);
        TextField sheetNameField = new TextField(sheetsService.getSheetName());
        sheetNameField.setPromptText("e.g. Invoices");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.add(new Label("Spreadsheet ID:"), 0, 0);
        grid.add(spreadsheetIdField, 1, 0);
        grid.add(new Label("Sheet (tab) name:"), 0, 1);
        grid.add(sheetNameField, 1, 1);

        Label hint = new Label(
                "Tip: the Spreadsheet ID is the long string in your Google Sheet's URL,\n" +
                        "between \"/d/\" and \"/edit\".");
        hint.setWrapText(true);
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        grid.add(hint, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(spreadsheetIdField.getText().trim().isEmpty());
        spreadsheetIdField.textProperty().addListener((obs, oldV, newV) ->
                saveButton.setDisable(newV.trim().isEmpty()));

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                String id = spreadsheetIdField.getText().trim();
                String name = sheetNameField.getText().trim().isEmpty()
                        ? "Invoices" : sheetNameField.getText().trim();

                sheetsService.setSpreadsheetId(id);
                sheetsService.setSheetName(name);
                configService.save(id, name);

                showAlert(Alert.AlertType.INFORMATION, "Settings saved.");
            }
            return null;
        });

        dialog.showAndWait();
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
        applyAppStyle(confirm);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            catalog.remove(selected);
            databaseService.deleteItem(selected.getItemId());
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

        if (sheetsService.getSpreadsheetId() == null || sheetsService.getSpreadsheetId().isBlank()) {
            showAlert(Alert.AlertType.WARNING,
                    "Please configure your Google Sheets ID first (⚙ Settings button, top right).");
            return;
        }

        double total = cart.stream().mapToDouble(CartItem::getSubtotal).sum();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                String.format("Confirm payment of %,.0f VND?", total));
        applyAppStyle(confirm);
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
        applyAppStyle(dialog);

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Grilled Chicken");
        TextField priceField = new TextField();
        priceField.setPromptText("e.g. 45000");
        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("e.g. Crispy fried chicken with garlic and pepper, served hot.");
        descriptionField.setPrefRowCount(3);
        descriptionField.setWrapText(true);

        ComboBox<ItemType> typeBox = new ComboBox<>(FXCollections.observableArrayList(ItemType.values()));
        typeBox.setPrefWidth(200);
        typeBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ItemType type, boolean empty) {
                super.updateItem(type, empty);
                setText(empty || type == null ? null : typeIcon(type) + "  " + type);
            }
        });
        typeBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ItemType type, boolean empty) {
                super.updateItem(type, empty);
                setText(empty || type == null ? null : typeIcon(type) + "  " + type);
            }
        });

        Label idPreviewLabel = new Label();
        idPreviewLabel.getStyleClass().add("type-badge");

        // Image picker
        ImageView previewView = new ImageView();
        previewView.setFitWidth(56);
        previewView.setFitHeight(56);
        previewView.setPreserveRatio(true);
        File[] selectedImageFile = new File[1]; // mutable holder for use inside lambdas
        Button chooseImageButton = new Button("Choose Image");
        chooseImageButton.getStyleClass().add("btn-manage-add");
        chooseImageButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select an image for this item");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File file = fileChooser.showOpenDialog(chooseImageButton.getScene().getWindow());
            if (file != null) {
                selectedImageFile[0] = file;
                previewView.setImage(new Image(file.toURI().toString(), 56, 56, true, true));
            }
        });
        HBox imagePickerBox = new HBox(10, chooseImageButton, previewView);
        imagePickerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Runnable updateIdPreview = () -> {
            ItemType type = typeBox.getValue();
            idPreviewLabel.getStyleClass().removeAll("type-badge-food", "type-badge-drink", "type-badge-other");
            switch (type) {
                case FOOD -> idPreviewLabel.getStyleClass().add("type-badge-food");
                case DRINK -> idPreviewLabel.getStyleClass().add("type-badge-drink");
                case OTHER -> idPreviewLabel.getStyleClass().add("type-badge-other");
            }
            idPreviewLabel.setText(typeIcon(type) + " " + generateNextId(type));
        };
        typeBox.valueProperty().addListener((obs, oldV, newV) -> updateIdPreview.run());
        typeBox.setValue(ItemType.FOOD);
        updateIdPreview.run();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(14);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeBox, 1, 0);
        grid.add(new Label("Item ID:"), 0, 1);
        grid.add(idPreviewLabel, 1, 1);
        grid.add(new Label("Name:"), 0, 2);
        grid.add(nameField, 1, 2);
        grid.add(new Label("Price (VND):"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Image:"), 0, 4);
        grid.add(imagePickerBox, 1, 4);
        grid.add(new Label("Description:"), 0, 5);
        grid.add(descriptionField, 1, 5);

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

                String imagePath = null;
                if (selectedImageFile[0] != null) {
                    imagePath = saveItemImage(selectedImageFile[0], id);
                }

                return new Item(id, name, price, typeBox.getValue(), imagePath, descriptionField.getText().trim());
            }
            return null;
        });

        Optional<Item> result = dialog.showAndWait();
        result.ifPresent(item -> {
            catalog.add(item);
            databaseService.insertItem(item);
            catalogTable.getSelectionModel().select(item);
            catalogTable.scrollTo(item);
        });
    }

    /**
     * Copies a user-picked image into a local "item-images" folder, renamed
     * to the item's ID (e.g. "F001.jpg"), so it's kept alongside the app and
     * can be reloaded by file path next time. Returns the new path, or null
     * if the copy failed.
     */
    private String saveItemImage(File sourceFile, String itemId) {
        try {
            Path targetDir = Path.of("item-images");
            Files.createDirectories(targetDir);

            String originalName = sourceFile.getName();
            String extension = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : ".png";

            Path targetFile = targetDir.resolve(itemId + extension);
            Files.copy(sourceFile.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            return targetFile.toString();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.WARNING, "Item was added, but the image could not be saved.");
            return null;
        }
    }

    /**
     * Opens a popup showing an enlarged image (or a big fallback icon),
     * name, category, price, and description for the clicked item.
     */
    private void showItemDetailDialog(Item item) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(item.getName());
        applyAppStyle(dialog);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 24, 10, 24));
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setPrefWidth(320);

        if (item.getImagePath() != null && !item.getImagePath().isBlank()
                && new File(item.getImagePath()).exists()) {
            ImageView bigImage = new ImageView(new Image(
                    new File(item.getImagePath()).toURI().toString(), 220, 220, true, true));
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(220, 220);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            bigImage.setClip(clip);
            content.getChildren().add(bigImage);
        } else {
            Label bigIcon = new Label(typeIcon(item.getType()));
            bigIcon.setStyle("-fx-font-size: 72px;");
            content.getChildren().add(bigIcon);
        }

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2D3436;");

        Label typeBadge = new Label(typeIcon(item.getType()) + " " + item.getType());
        typeBadge.getStyleClass().add("type-badge");
        switch (item.getType()) {
            case FOOD -> typeBadge.getStyleClass().add("type-badge-food");
            case DRINK -> typeBadge.getStyleClass().add("type-badge-drink");
            case OTHER -> typeBadge.getStyleClass().add("type-badge-other");
        }

        Label priceLabel = new Label(String.format("%,.0f VND", item.getPrice()));
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #FF6B35;");

        HBox metaRow = new HBox(10, typeBadge, priceLabel);
        metaRow.setAlignment(javafx.geometry.Pos.CENTER);

        Label descriptionLabel = new Label(
                item.getDescription() == null || item.getDescription().isBlank()
                        ? "No description provided."
                        : item.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #718096; -fx-text-alignment: center;");
        descriptionLabel.setAlignment(javafx.geometry.Pos.CENTER);

        content.getChildren().addAll(nameLabel, metaRow, descriptionLabel);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
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
        int itemCount = cart.stream().mapToInt(CartItem::getAmount).sum();

        totalLabel.setText(String.format("%,.0f VND", total));
        cartSubtotalLabel.setText(String.format("%,.0f VND", total));
        cartCountLabel.setText(String.valueOf(itemCount));
    }

    /**
     * Returns a small emoji icon representing an item type, used in the
     * catalog table and in the "Add New Item" dialog's type picker.
     */
    private String typeIcon(ItemType type) {
        return switch (type) {
            case FOOD -> "\uD83C\uDF7D"; // 🍽
            case DRINK -> "\uD83E\uDD64"; // 🥤
            case OTHER -> "\uD83D\uDCE6"; // 📦
        };
    }

    /**
     * Dialogs open in their own window/scene and do NOT automatically inherit
     * the main window's stylesheet, so without this every popup would render
     * with plain default JavaFX styling instead of matching the app's theme.
     */
    private void applyAppStyle(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/foodorder/view/style.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        applyAppStyle(alert);
        alert.showAndWait();
    }
}