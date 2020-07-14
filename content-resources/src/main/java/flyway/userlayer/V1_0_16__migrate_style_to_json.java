package flyway.userlayer;

import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.UserDataStyleMigrator;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import java.sql.Connection;

/**
 * Migrate fields JSONObject to JSONArray from user_layer table
 */
public class V1_0_16__migrate_style_to_json implements JdbcMigration {
    private static final Logger LOG = LogFactory.getLogger(V1_0_16__migrate_style_to_json.class);
    private static final String STYLE_TABLE = "user_layer_style";
    private static final String LAYER_TABLE = "user_layer";
    private static final String STYLE_ID_COLUMN = "style_id";
    private static final String LAYER_NAME = "oskari:vuser_layer_data";
    private static final String PROPERTY_PREXIX = "userlayer";

    public void migrate(Connection connection) throws Exception {
        int count = UserDataStyleMigrator.migrateStyles(connection, LAYER_TABLE, STYLE_TABLE, STYLE_ID_COLUMN);
        LOG.info("Migrated", count, "styles from table:", STYLE_TABLE, "to options column in table:", LAYER_TABLE);
        UserDataStyleMigrator.updateBaseLayerOptions(LAYER_NAME, PROPERTY_PREXIX, null);
        LOG.info("Updated options for:", LAYER_NAME);
    }
}
