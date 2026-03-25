package com.prashant.propertysearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The entry point for the PropertySearchApplication Spring Boot application.
 *
 * This class is responsible for bootstrapping and launching the application
 * using Spring Boot's {@code SpringApplication.run()} method.
 *
 * Key Features:
 * - Initializes the Spring Boot application context.

 * - Serves as the main entry point for application execution.
 */
@SpringBootApplication
public class PropertySearchApplication {

	public static void main(String[] args) {
		SpringApplication.run(PropertySearchApplication.class, args);
	}

}
