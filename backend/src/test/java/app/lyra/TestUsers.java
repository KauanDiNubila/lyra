package app.lyra;

import app.lyra.shared.domain.UserId;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class TestUsers {

    private TestUsers() {
    }

    public static UserId insert(JdbcClient jdbc) {
        UserId id = UserId.random();
        jdbc.sql("""
                        insert into app_user (id, provider, provider_subject, email, name, created_at)
                        values (:id, 'GOOGLE', :subject, :email, 'Test user', now())
                        """)
                .param("id", id.value())
                .param("subject", "subject-" + id)
                .param("email", id + "@example.com")
                .update();
        return id;
    }
}
