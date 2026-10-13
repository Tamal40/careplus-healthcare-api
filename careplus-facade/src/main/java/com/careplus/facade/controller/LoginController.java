package com.careplus.facade.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.careplus.facade.dto.LoginRequest;
import com.careplus.facade.dto.LoginResponse;
import com.careplus.facade.service.AuthenticationService;

@RestController
@RequestMapping("/api/auth")
public class LoginController {
	
	@Autowired
	 private AuthenticationService authService;
	
	@PostMapping("/login")
	 public ResponseEntity<LoginResponse> Login(@RequestBody LoginRequest req){
		 String token = authService.login(req);
		 LoginResponse res=new LoginResponse();
		 res.setToken(token);

		 return ResponseEntity.status(HttpStatusCode.valueOf(200)).
					body(res);
		 
	 }

}
