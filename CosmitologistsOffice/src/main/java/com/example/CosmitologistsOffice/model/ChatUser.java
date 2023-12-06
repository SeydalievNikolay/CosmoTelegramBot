package com.example.CosmitologistsOffice.model;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Entity(name = "usersDataTable")
public class ChatUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatId;

    private String firstName;

    private String lastName;
    @Column(unique = true)
    private String username;

    private Timestamp registeredAt;

}
