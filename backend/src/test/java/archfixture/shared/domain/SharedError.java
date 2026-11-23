package archfixture.shared.domain;

public class SharedError extends RuntimeException {

    public SharedError(String message) {
        super(message);
    }
}
