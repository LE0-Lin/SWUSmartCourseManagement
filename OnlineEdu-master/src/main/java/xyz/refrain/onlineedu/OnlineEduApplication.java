package xyz.refrain.onlineedu;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application Entrance - Final Fix Version
 */
@EnableScheduling
@ConfigurationPropertiesScan("xyz.refrain.onlineedu.config.properties")
@SpringBootApplication
public class OnlineEduApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineEduApplication.class, args);
	}

}
