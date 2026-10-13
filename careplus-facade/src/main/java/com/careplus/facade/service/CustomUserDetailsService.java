package com.careplus.facade.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.careplus.facade.dto.AppUser;
import com.careplus.facade.repository.UserRepository;

@Component
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	UserRepository repo;
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser user=repo.findByUsername(username)
				.orElseThrow(()->new UsernameNotFoundException("User Not Found"));
		return User.builder().username(username).password(user.getPassword()).roles(user.getRole().name()).build();
		}

}
