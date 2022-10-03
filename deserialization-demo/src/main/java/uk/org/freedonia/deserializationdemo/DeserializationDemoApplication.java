package uk.org.freedonia.deserializationdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeserializationDemoApplication {

	public static void main(String[] args) {
		System.setProperty("com.sun.jndi.ldap.object.trustURLCodebase","true");
		System.setProperty("com.sun.jndi.rmi.object.trustURLCodebase","true");

		SpringApplication.run(DeserializationDemoApplication.class, args);
	}

}
