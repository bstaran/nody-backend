package org.nodystudio.nodybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NodyBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(NodyBackendApplication.class, args);
  }

}
