package com.careplus.facade.controller;
import java.util.Enumeration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.careplus.facade.dto.LoginRequest;
import com.careplus.facade.dto.LoginResponse;
import com.careplus.facade.service.AuthenticationService;
import com.careplus.facade.service.RouteResolver;

import jakarta.servlet.http.HttpServletRequest;
@RestController
public class FacadeController {
 @GetMapping("/api/status")
 public String status(){return "CarePlus facade running; service routing not implemented yet.";}
 // TODO: call patient, doctor and appointment APIs from this module.
 
 @Autowired
 private  RestTemplate restTemplate;
 
 @Autowired
 private  RouteResolver routeResolver;


 @RequestMapping("/api/**")
 public ResponseEntity<byte[]> forward(
         HttpServletRequest request,
         @RequestBody(required = false) byte[] body) {

     String path = request.getRequestURI();

     String serviceUrl = routeResolver.resolve(path);

     String targetUrl = serviceUrl + path;
     
     if (request.getQueryString() != null) {
         targetUrl += "?" + request.getQueryString();
     }


     HttpMethod method =
             HttpMethod.valueOf(request.getMethod());

     HttpHeaders headers = copyHeaders(request);
     String contentType=request.getContentType();
     if(contentType!=null) {
     headers.set(HttpHeaders.CONTENT_TYPE, contentType);
     }
     String authorization =
    	        request.getHeader(
    	                HttpHeaders.AUTHORIZATION
    	        );

    	if (authorization != null) {
    	    headers.set(
    	            HttpHeaders.AUTHORIZATION,
    	            authorization
    	    );
    	}

     HttpEntity<byte[]> entity =
             new HttpEntity<>(body, headers);
     System.out.println(authorization);

     try {
		return restTemplate.exchange(
		         targetUrl,
		         method,
		         entity,
		         byte[].class
		 );
	} catch (HttpStatusCodeException e) {
		return ResponseEntity.status(e.getStatusCode()).
				headers(e.getResponseHeaders()).
				body(e.getResponseBodyAsByteArray());
	}
 }

 private HttpHeaders copyHeaders(
         HttpServletRequest request) {

     HttpHeaders headers = new HttpHeaders();

     Enumeration<String> headerNames =
             request.getHeaderNames();

     while (headerNames.hasMoreElements()) {

         String headerName =
                 headerNames.nextElement();

         headers.add(
                 headerName,
                 request.getHeader(headerName)
         );
     }

     return headers;
 }
}