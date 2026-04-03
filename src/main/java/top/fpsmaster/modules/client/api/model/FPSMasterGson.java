package top.fpsmaster.modules.client.api.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public final class FPSMasterGson {
    private static final Gson INSTANCE = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .disableHtmlEscaping()
            .create();

    private FPSMasterGson() {
    }

    public static Gson getInstance() {
        return INSTANCE;
    }

    public static String toJson(Object obj) {
        return INSTANCE.toJson(obj);
    }

    public static JsonObject toJsonObject(Object obj) {
        return INSTANCE.toJsonTree(obj).getAsJsonObject();
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return INSTANCE.fromJson(json, classOfT);
    }
}
