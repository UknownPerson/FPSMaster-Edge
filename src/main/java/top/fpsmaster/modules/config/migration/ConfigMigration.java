package top.fpsmaster.modules.config.migration;

import com.google.gson.JsonObject;

public interface ConfigMigration {

    int getFromVersion();

    int getToVersion();

    JsonObject migrate(JsonObject source);
}
