package com.example.CosmitologistsOffice.repository;

import com.example.CosmitologistsOffice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Appointment findByChatId(long chatId);

    List<Appointment> findByDate(String date);

    Appointment findByChatIdAndService(long chatId, String selectedService);
}
