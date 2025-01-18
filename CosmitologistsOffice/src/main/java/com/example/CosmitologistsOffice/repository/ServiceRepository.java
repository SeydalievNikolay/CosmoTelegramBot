package com.example.CosmitologistsOffice.repository;

import com.example.CosmitologistsOffice.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    Optional<Service> findByName(String defaultService);
}
