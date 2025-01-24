package com.example.CosmitologistsOffice.repository;

import com.example.CosmitologistsOffice.model.Cosmetologist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CosmetologistRepository extends JpaRepository<Cosmetologist, Long> {
    Cosmetologist findByTelegramChatId(Long chatId);
}
