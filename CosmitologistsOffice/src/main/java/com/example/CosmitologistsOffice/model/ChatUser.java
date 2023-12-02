package com.example.CosmitologistsOffice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.sql.Timestamp;
@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity(name = "usersDataTable")
public class ChatUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatId;

    private String firstName;

    private String lastName;
    @Column(unique = true)
    private String userName;

    private Timestamp registeredAt;

}
