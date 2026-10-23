package com.careplus.patient.service;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {
	
	@Bean(name="csvUploadExecutor")
	public Executor csvUploadExceutor() {
		
		ThreadPoolTaskExecutor e=new ThreadPoolTaskExecutor();
		e.setCorePoolSize(2);
		e.setMaxPoolSize(3);
		e.setQueueCapacity(50);
		e.setThreadNamePrefix("csv-upload-");
		e.initialize();
		return e;
	}

}
