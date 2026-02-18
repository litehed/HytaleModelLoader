package com.litehed.hytalemodels.blockymodel;

import com.google.gson.JsonObject;

public class ParserUtil {
    public static float getFloatOrDefault(JsonObject obj, String key, float defaultValue) {
        return obj.has(key) ? obj.get(key).getAsFloat() : defaultValue;
    }

    public static int getIntOrDefault(JsonObject obj, String key, int defaultValue) {
        return obj.has(key) ? obj.get(key).getAsInt() : defaultValue;
    }

    public static boolean getBooleanOrDefault(JsonObject obj, String key, boolean defaultValue) {
        return obj.has(key) ? obj.get(key).getAsBoolean() : defaultValue;
    }
}
