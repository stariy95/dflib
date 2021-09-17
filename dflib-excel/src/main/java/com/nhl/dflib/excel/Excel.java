package com.nhl.dflib.excel;

import com.nhl.dflib.DataFrame;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

/**
 * An entry point to work with Excel files.
 *
 * @since 0.13
 */
public class Excel {

    public static Map<String, DataFrame> load(InputStream in) {
        return loader().load(in);
    }

    public static Map<String, DataFrame> load(File file) {
        return loader().load(file);
    }

    public static Map<String, DataFrame> load(String filePath) {
        return loader().load(filePath);
    }

    public static Map<String, DataFrame> load(Path path) {
        return loader().load(path);
    }

    public static DataFrame loadSheet(InputStream in, String sheetName) {
        return loader().loadSheet(in, sheetName);
    }

    public static DataFrame loadSheet(File file, String sheetName) {
        return loader().loadSheet(file, sheetName);
    }

    public static DataFrame loadSheet(String filePath, String sheetName) {
        return loader().loadSheet(filePath, sheetName);
    }

    public static DataFrame loadSheet(Path path, String sheetName) {
        return loader().loadSheet(path, sheetName);
    }

    public static ExcelLoader loader() {
        return new ExcelLoader();
    }
}