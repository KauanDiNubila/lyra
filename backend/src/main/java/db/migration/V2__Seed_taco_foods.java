package db.migration;

import app.lyra.food.domain.SearchText;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

public class V2__Seed_taco_foods extends BaseJavaMigration {

    private static final String RESOURCE = "/db/taco/taco-foods.tsv";
    private static final String INSERT = """
            insert into food (id, name, search_name, source, source_reference, reference_amount, reference_unit,
                              kcal, protein, carbohydrate, fat, fiber, sodium)
            values (?, ?, ?, 'TACO', ?, 100, 'GRAM', ?, ?, ?, ?, ?, ?)
            """;

    @Override
    public void migrate(Context context) throws Exception {
        try (InputStream input = V2__Seed_taco_foods.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("TACO data not found at " + RESOURCE);
            }
            List<String> lines = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines().skip(1).toList();
            try (PreparedStatement insert = context.getConnection().prepareStatement(INSERT)) {
                for (String line : lines) {
                    bind(insert, line.split("\t", -1));
                    insert.addBatch();
                }
                insert.executeBatch();
            }
        }
    }

    private static UUID idOf(String tacoNumber) {
        return UUID.nameUUIDFromBytes(("taco:" + tacoNumber).getBytes(StandardCharsets.UTF_8));
    }

    private static void bind(PreparedStatement insert, String[] cells) throws Exception {
        if (cells.length != 8) {
            throw new IllegalStateException("Expected 8 columns in TACO row: " + String.join("|", cells));
        }
        insert.setObject(1, idOf(cells[0]));
        insert.setString(2, cells[1]);
        insert.setString(3, SearchText.normalize(cells[1]));
        insert.setString(4, "taco:" + cells[0]);
        for (int column = 2; column < 8; column++) {
            insert.setBigDecimal(column + 3, cells[column].isEmpty() ? null : new BigDecimal(cells[column]));
        }
    }
}
