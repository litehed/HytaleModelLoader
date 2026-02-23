package com.litehed.hytalemodels.blockymodel;

import com.google.common.base.Charsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

// Tokenizer for reading BlockyModel JSON files. It reads the entire file into memory and parses it as a JsonObject.
public class BlockyTokenizer implements AutoCloseable {
    private final BufferedReader lineReader;
    private final JsonObject root;

    /**
     * Creates a new BlockyTokenizer that reads from the given InputStream
     *
     * @param inputStream The InputStream to read from
     */
    public BlockyTokenizer(InputStream inputStream) {
        this.lineReader = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
        this.root = JsonParser.parseReader(lineReader).getAsJsonObject();
    }

    public JsonObject getRoot() {
        return root;
    }

    @Override
    public void close() throws Exception {
        this.lineReader.close();
    }
}
