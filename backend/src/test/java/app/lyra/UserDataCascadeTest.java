package app.lyra;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UserDataCascadeTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    void everyTableWithAUserIdDeletesItsRowsWhenTheUserIsDeleted() {
        assertThat(tablesWhoseUserIdDoesNotCascade()).isEmpty();
    }

    @Test
    void theCheckFindsATableThatForgetsTheCascade() {
        jdbc.sql("create table forgotten_user_data (id uuid primary key, user_id uuid not null references app_user (id))").update();

        assertThat(tablesWhoseUserIdDoesNotCascade()).containsExactly("forgotten_user_data");
    }

    private List<String> tablesWhoseUserIdDoesNotCascade() {
        return jdbc.sql("""
                        select c.relname
                        from pg_class c
                        join pg_namespace n on n.oid = c.relnamespace
                        join pg_attribute a on a.attrelid = c.oid and a.attname = 'user_id' and not a.attisdropped
                        where c.relkind = 'r' and n.nspname = 'public'
                          and not exists (
                              select 1 from pg_constraint k
                              where k.conrelid = c.oid and k.contype = 'f' and k.confrelid = 'app_user'::regclass
                                and k.confdeltype = 'c' and a.attnum = any (k.conkey))
                        order by c.relname
                        """)
                .query(String.class)
                .list();
    }
}
