package com.careplus.facade.controller;
import java.io.IOException;
import java.util.Enumeration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

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
         @RequestBody(required = false) byte[] body) throws IOException {

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
    	
    	HttpEntity<?> entity;

    	if (contentType != null
                && contentType.startsWith(
                        MediaType.MULTIPART_FORM_DATA_VALUE)) {

            if (!(request instanceof MultipartHttpServletRequest multipartRequest)) {
                return ResponseEntity.badRequest().build();
            }

            MultipartFile file = multipartRequest.getFile("file");

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            MultiValueMap<String, Object> multipartBody =
                    new LinkedMultiValueMap<>();

            multipartBody.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            // Remove headers belonging to the original incoming request.
            headers.remove(HttpHeaders.CONTENT_LENGTH);
            headers.remove(HttpHeaders.TRANSFER_ENCODING);
            headers.remove(HttpHeaders.CONTENT_TYPE);

            // RestTemplate will generate the new multipart boundary.
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            entity = new HttpEntity<>(multipartBody, headers);

        } else {
            body = request.getInputStream().readAllBytes();
            entity = new HttpEntity<>(body, headers);
        }
     
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