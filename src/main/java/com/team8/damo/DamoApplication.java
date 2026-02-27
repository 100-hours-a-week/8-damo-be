package com.team8.damo;

import co.elastic.apm.attach.ElasticApmAttacher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

@SpringBootApplication
public class DamoApplication {

	public static void main(String[] args) {
        ElasticApmAttacher.attach();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(DamoApplication.class, args);
	}

}
