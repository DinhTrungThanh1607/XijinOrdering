package com.foodorder.service;

import com.foodorder.model.CartItem;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles authentication with Google and appending invoice rows to a
 * Google Sheet whenever a checkout happens.
 *
 * Setup required before this class will work:
 *   1. Create a project in Google Cloud Console.
 *   2. Enable the "Google Sheets API" for that project.
 *   3. Create OAuth 2.0 Client ID credentials of type "Desktop app".
 *   4. Download the JSON and save it as: src/main/resources/credentials.json
 *   5. Create a Google Sheet, share it with your own account, and copy its
 *      Spreadsheet ID (the long string in the sheet's URL) into
 *      SPREADSHEET_ID below, or pass it into the constructor.
 */
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "Food Order App";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String spreadsheetId;
    private final String sheetName;
    private Sheets sheetsService;

    public GoogleSheetsService(String spreadsheetId, String sheetName) {
        this.spreadsheetId = spreadsheetId;
        this.sheetName = sheetName;
    }

    /**
     * Lazily builds and caches an authorized Sheets API client.
     * The first time this runs it will open a browser window asking the
     * user to log in and grant access; a token is then cached under
     * ./tokens so future runs do not need to log in again.
     */
    private Sheets getSheetsService() throws GeneralSecurityException, IOException {
        if (sheetsService != null) {
            return sheetsService;
        }
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(HTTP_TRANSPORT);
        sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        return sheetsService;
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStreamReader in = new InputStreamReader(
                GoogleSheetsService.class.getResourceAsStream(CREDENTIALS_FILE_PATH));
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, in);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        // Let the OS pick any free port automatically instead of a fixed one,
        // so this doesn't fail if something else is already using that port.
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Appends one row to the sheet representing a paid invoice:
     * [date, time, comma-separated itemIds, total value]
     */
    public AppendValuesResponse uploadInvoice(List<CartItem> paidItems, double total)
            throws GeneralSecurityException, IOException {

        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DATE_FMT);
        String time = now.format(TIME_FMT);

        String itemIds = paidItems.stream()
                .map(CartItem::getItemId)
                .collect(Collectors.joining(", "));

        List<Object> row = new ArrayList<>();
        row.add(date);
        row.add(time);
        row.add(itemIds);
        row.add(total);

        List<List<Object>> values = new ArrayList<>();
        values.add(row);

        ValueRange body = new ValueRange().setValues(values);

        String range = sheetName + "!A:D";

        return getSheetsService().spreadsheets().values()
                .append(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();
    }
}