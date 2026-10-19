package com.litehed.hytalemodels.blockymodel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

// Reads a Blocky JSON file into a JsonObject
public class BlockyTokenizer implements AutoCloseable {
    private final BufferedReader reader;
    private final JsonObject root;

    /**
     * Reads the whole file. The stream is closed by {@link #close()}, or here if the file is not valid JSON
     *
     * @param inputStream the file to read
     */
    public BlockyTokenizer(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        try {
            this.root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (RuntimeException e) {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    public JsonObject getRoot() {
        return root;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}