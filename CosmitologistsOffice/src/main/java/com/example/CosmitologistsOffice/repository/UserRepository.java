package com.example.CosmitologistsOffice.repository;

import com.example.CosmitologistsOffice.model.ChatUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<ChatUser, Long> {
    boolean findByUsername(String username);
}
