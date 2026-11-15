package pl.piomin.services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CommandServiceApplication {

    /** Axon's update checker reports JVM and OS details to Axoniq; off unless the property or variable says otherwise. */
    static final String UPDATE_CHECKER_DISABLED = "axoniq.update-checker.disabled";

    public static void main(String[] args) {
        if (System.getProperty(UPDATE_CHECKER_DISABLED) == null) {
            System.setProperty(UPDATE_CHECKER_DISABLED, "true");
        }
        SpringApplication.run(CommandServiceApplication.class, args);
    }
}
