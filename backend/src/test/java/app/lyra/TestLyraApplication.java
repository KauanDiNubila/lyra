package app.lyra;

import org.springframework.boot.SpringApplication;

public class TestLyraApplication {

	public static void main(String[] args) {
		SpringApplication.from(LyraApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
