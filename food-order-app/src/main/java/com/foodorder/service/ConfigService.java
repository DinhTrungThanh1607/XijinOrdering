package com.foodorder.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Reads and writes simple app settings (Google Spreadsheet ID, Sheet name)
 * to a local "config.properties" file, so the user can configure them from
 * the UI instead of editing source code.
 */
public class ConfigService {

    private static final String CONFIG_FILE = "config.properties";
    private static final String KEY_SPREADSHEET_ID = "spreadsheetId";
    private static final String KEY_SHEET_NAME = "sheetName";
    private static final String DEFAULT_SHEET_NAME = "Invoices";

    private final Properties props = new Properties();

    public ConfigService() {
        load();
    }

    private void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getSpreadsheetId() {
        return props.getProperty(KEY_SPREADSHEET_ID, "");
    }

    public String getSheetName() {
        return props.getProperty(KEY_SHEET_NAME, DEFAULT_SHEET_NAME);
    }

    public void save(String spreadsheetId, String sheetName) {
        props.setProperty(KEY_SPREADSHEET_ID, spreadsheetId);
        props.setProperty(KEY_SHEET_NAME, sheetName);
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "XijinOrdering configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}