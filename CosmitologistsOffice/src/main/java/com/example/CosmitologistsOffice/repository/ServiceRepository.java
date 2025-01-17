package com.example.CosmitologistsOffice.repository;

import com.example.CosmitologistsOffice.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    String findByName(String defaultService);
}
