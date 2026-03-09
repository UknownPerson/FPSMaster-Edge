package top.fpsmaster.modules.config.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigMigrationRegistry {

    private static final List<ConfigMigration> MIGRATIONS = new ArrayList<>();

    private ConfigMigrationRegistry() {
    }

    public static List<ConfigMigration> getMigrations() {
        return Collections.unmodifiableList(MIGRATIONS);
    }

    public static ConfigMigration findMigration(int fromVersion) {
        for (ConfigMigration migration : MIGRATIONS) {
            if (migration.getFromVersion() == fromVersion) {
                return migration;
            }
        }
        return null;
    }
}
