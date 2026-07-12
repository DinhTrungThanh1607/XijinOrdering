# Food Order App

JavaFX app for ordering food, drink, and other items. On checkout, the invoice
(date, time, itemIds, total value) is appended as a new row to a Google Sheet.

## Project structure

```
food-order-app/
├── pom.xml
└── src/main/
    ├── java/com/foodorder/
    │   ├── Main.java                     Loads main.fxml and shows the window
    │   ├── controller/
    │   │   └── MainController.java       All UI logic (bound to main.fxml)
    │   ├── model/
    │   │   ├── Item.java                 itemId, name, price, type
    │   │   ├── ItemType.java             FOOD, DRINK, OTHER
    │   │   └── CartItem.java             Item + amount (quantity in order)
    │   └── service/
    │       └── GoogleSheetsService.java  OAuth + append invoice row
    └── resources/
        ├── com/foodorder/view/
        │   └── main.fxml                 UI layout (catalog + cart + checkout)
        └── credentials.json               <-- you must add this file yourself
```

The project follows MVC: `main.fxml` defines the layout, `MainController.java`
contains all the behavior, and `Main.java` just loads the FXML and shows it.

## Setup

### 1. Requirements
- JDK 17+
- Maven 3.8+

### 2. Google Sheets API setup
1. Go to https://console.cloud.google.com/ and create a new project (or use an existing one).
2. Enable the **Google Sheets API** for that project (APIs & Services → Library).
3. Go to **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
   - Application type: **Desktop app**.
4. Download the resulting JSON file, rename it to `credentials.json`, and place it at:
   `src/main/resources/credentials.json`
5. Create a new Google Sheet. Add a header row if you like, e.g.:
   `Date | Time | Item IDs | Total`
6. Copy the Spreadsheet ID from the sheet's URL:
   `https://docs.google.com/spreadsheets/d/SPREADSHEET_ID_HERE/edit`
7. Open `MainController.java` and set:
   ```java
   private static final String SPREADSHEET_ID = "SPREADSHEET_ID_HERE";
   private static final String SHEET_NAME = "Invoices"; // must match your sheet tab name
   ```

### 3. Run it
```bash
mvn clean javafx:run
```

The first time you check out an order, a browser window will open asking you
to log in to Google and grant access. After that, a token is cached in the
`tokens/` folder so you won't need to log in again.

## Notes
- The item catalog is hardcoded in `Main.java` for now (`catalog` list) — easy
  to swap for a database or file later.
- Each invoice row uploaded to Sheets is: `[date, time, comma-separated itemIds, total]`.
