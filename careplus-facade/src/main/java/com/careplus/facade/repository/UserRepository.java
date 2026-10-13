package com.careplus.facade.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.careplus.facade.dto.AppUser;

@Repository
public interface UserRepository extends JpaRepository<AppUser,Long> {
	
	  Optional<AppUser> findByUsername(String username);

	    boolean existsByUsername(String username);

}
