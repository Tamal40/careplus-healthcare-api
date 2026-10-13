package com.careplus.appointment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.careplus.appointment.model.AppointmentDto;
import com.careplus.common.dto.Status;

@Component
public class SchedularJob {
	
	@Autowired
	private RestTemplate template;
	
	@Autowired
	private AppointmentService service;
	
	@Scheduled(cron = "0 * * * * *")
	public void bookAppointment() {
		AppointmentDto app=new AppointmentDto();
		app.setDoctor_id(1L);
		app.setPatient_id(1L);
		app.setAppointmentDate( LocalDate.of(2026, 10, 14));
		app.setAppointmentTime(LocalTime.of(15, 07));
		app.setCreated_at(LocalDateTime.now());
		app.setUpdated_at(LocalDateTime.now());
		app.setReason("Concurrency");
		app.setStatus(Status.BOOKED.toString());
		
		ExecutorService ex=Executors.newFixedThreadPool(100);
		CyclicBarrier ba=new CyclicBarrier(100);
		List<Future<?>> future=new ArrayList<>();
		for(int i=0;i<100;i++) {
			future.add(ex.submit(()->{
				try {
				ba.await();
				service.createAppointment(app, "Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJkb2N0b3IxIiwiaWF0IjoxNzkxODEzNzAyLCJyb2xlIjoiUk9MRV9ET0NUT1IiLCJleHAiOjE3OTE4MTczMDJ9.zFHhE5xxElaXc4tFRm5G73Nkdf--oAUyiqrMwoMy8yUIl-HR5BDrAACTwvcQcsrz");
				}
				catch(Exception exx) {
					exx.printStackTrace();
				}
			}));
		}
		for (Future<?> futur : future) {
		    try {
				futur.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		ex.shutdown();
		
	}

}
