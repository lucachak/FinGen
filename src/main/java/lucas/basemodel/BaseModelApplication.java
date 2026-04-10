package lucas.basemodel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // <-- NOVO IMPORT
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BaseModelApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaseModelApplication.class, args);
    }
}