package com.example.CosmitologistsOffice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cosmetologists")
public class Cosmetologist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(name = "nick_name", unique = true)
    private String nickName;
    @Column
    private String phone;
    @Column
    private String email;
    @Column(name = "telegram_chat_id")
    private Long telegramChatId;
    @OneToMany(mappedBy = "cosmetologist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Appointment> appointments;
}
