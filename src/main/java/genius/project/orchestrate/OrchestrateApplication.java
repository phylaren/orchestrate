package genius.project.orchestrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class OrchestrateApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestrateApplication.class, args);
    }

}