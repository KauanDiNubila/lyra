package app.lyra;

import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

public abstract class SignedInControllerTest {

    protected static final UserId SIGNED_IN = UserId.random();
    protected static final List<UserId> CALLERS = new ArrayList<>();

    @BeforeEach
    void signIn() {
        TestSessions.signIn(SIGNED_IN);
        CALLERS.clear();
    }

    @AfterEach
    void signOut() {
        TestSessions.signOut();
    }
}
