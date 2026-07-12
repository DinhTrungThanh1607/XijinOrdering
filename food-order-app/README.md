# XijinOrdering

A JavaFX desktop app for ordering food, drinks, and other items. Menu items
(with photos and descriptions) are saved locally in a SQLite database, and
every completed checkout is uploaded as a new row to a Google Sheet.

## Features

- 🍽️ Browse a menu of Food / Drink / Other items, each with a photo,
  price, category badge, and description (click an item's image to see it
  enlarged with full details)
- 🛒 Add items to a cart, adjust quantities, remove items, and check out
- ➕ Add new menu items on the fly — item ID is auto-generated per category
  (F001, F002... for Food, D001, D002... for Drink, O001, O002... for Other)
- 🗑️ Remove menu items you no longer want to sell
- 💾 **Menu persists across restarts** — saved in a local SQLite database
  (`xijinordering.db`), no external database server needed
- 🧾 On checkout, an invoice row (date, time, item IDs, total) is uploaded
  to a Google Sheet via the Google Sheets API
- ⚙️ Configure the target Spreadsheet ID and sheet name directly from the
  app's **Settings** button — no code editing required, saved locally in
  `config.properties`

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
    │   │   ├── Item.java                 itemId, name, price, type, imagePath, description
    │   │   ├── ItemType.java             FOOD, DRINK, OTHER
    │   │   └── CartItem.java             Item + amount (quantity in order)
    │   └── service/
    │       ├── DatabaseService.java      Reads/writes the menu to SQLite
    │       ├── ConfigService.java        Reads/writes Settings (Spreadsheet ID, sheet name)
    │       └── GoogleSheetsService.java  OAuth + append invoice row to Google Sheets
    └── resources/
        ├── com/foodorder/view/
        │   ├── main.fxml                 UI layout (catalog + cart + checkout)
        │   └── style.css                 App theme
        └── credentials.json               <-- you must add this file yourself
```

The project follows MVC: `main.fxml` defines the main window layout,
`MainController.java` contains the behavior (including a few dialogs built
directly in code: Add New Item, Item Detail, Settings), and `Main.java`
just loads the FXML and shows it.

Two files/folders are created automatically the first time you run the app
and are **not** part of the source code:
- `xijinordering.db` — the local SQLite database holding your menu
- `config.properties` — your saved Spreadsheet ID / sheet name
- `item-images/` — copies of any photos you attach to menu items
- `tokens/` — your cached Google sign-in, created after your first checkout

## Setup

### 1. Requirements
- JDK 17+
- Maven 3.8+

### 2. Google Sheets API setup
1. Go to https://console.cloud.google.com/ and create a new project (or use an existing one).
2. Enable the **Google Sheets API** for that project (APIs & Services → Library).
3. Configure the **Google Auth Platform** (OAuth consent screen): app name, your
   email as support/contact email, Audience = External, and add your own
   Google account under **Test users** (required while the app is unverified).
4. Go to **Clients → Create client**.
   - Application type: **Desktop app**.
5. Open the created client, expand "Additional information," and download
   the **Client secret** JSON. Rename it to `credentials.json` and place it at:
   `src/main/resources/credentials.json`
6. Create a new Google Sheet, and rename its bottom tab (default "Sheet1")
   to whatever you plan to use as the sheet name (default in this app:
   `Invoices`).
7. Copy the Spreadsheet ID from the sheet's URL:
   `https://docs.google.com/spreadsheets/d/SPREADSHEET_ID_HERE/edit`

### 3. Configure the app (no code editing needed)
Run the app once, click the **⚙ Settings** button (top right), and paste in
your Spreadsheet ID and sheet name. Click Save — these are remembered for
next time in `config.properties`.

### 4. Run it
```bash
mvn clean javafx:run
```

The first time you check out an order, a browser window opens asking you to
sign in to Google and grant access. After that, the sign-in is cached in
`tokens/`, so you won't need to sign in again.

## CI/CD (GitHub Actions)

This repo includes two workflows under `.github/workflows/` (at the repo
root, one level above `food-order-app/`):

- **`ci.yml`** — runs on every push/PR to `main`. Compiles the project with
  Maven to catch build errors early. It only *compiles* (doesn't run the
  app), since GitHub's runners have no display for a JavaFX window to open.
- **`release.yml`** — runs when you push a version tag (e.g. `v1.0.0`).
  Builds a single runnable "fat jar" (all dependencies bundled in) and
  attaches it to a new GitHub Release automatically.

To cut a release:
```bash
git tag v1.0.0
git push origin v1.0.0
```
Then check the **Releases** page on GitHub — a `xijinordering-1.0.0.jar`
will appear there a minute or two later. Anyone can then run it with:
```bash
java -jar xijinordering-1.0.0.jar
```
as long as they have JDK 17+ installed (they'll still need their own
`credentials.json` and Settings configured on first run, same as above).

> Note: this jar is *not* a native Windows installer (no `.exe`/`.msi`).
> Producing one reliably would need `jpackage` with a JDK build that
> bundles JavaFX, which is a fair bit more setup — ask if you'd like help
> adding that later.

## Notes
- Each invoice row uploaded to Sheets is: `[date, time, comma-separated itemIds, total]`.
- Menu item photos are copied into `item-images/`, renamed to the item's ID
  (e.g. `F001.jpg`), so they aren't lost if you move the original file.
