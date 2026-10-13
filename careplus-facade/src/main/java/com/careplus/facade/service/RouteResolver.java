package com.careplus.facade.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RouteResolver {
	
	@Autowired
	private ServiceProperties properties;

    public String resolve(String path) {

        if (path.startsWith("/api/patients")) {
            return properties.getPatient().getUrl();
        }

        if (path.startsWith("/api/doctors")) {
            return properties.getDoctor().getUrl();
        }

        if (path.startsWith("/api/appointments")) {
            return properties.getAppointment().getUrl();
        }

        throw new IllegalArgumentException(
                "No route configured for: " + path
        );
    }

}
