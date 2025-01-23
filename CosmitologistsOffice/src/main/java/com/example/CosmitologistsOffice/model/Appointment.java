package com.example.CosmitologistsOffice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long chatId;
    private String service;
    private String date;
    private String time;
    @Column(name = "booked_at")
    private Timestamp bookedAt;
    @Column(precision = 10, scale = 2)
    private BigDecimal price;
    @Column(name = "booked")
    private Boolean booked;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cosmetologist_id")
    private Cosmetologist cosmetologist;

    public void setChatUser(ChatUser chatUser) {
        this.chatId = chatUser.getChatId();
    }

    public boolean isBooked() {
        return booked != null;
    }

    public void setBooked(boolean booked) {
        this.booked = booked;
        if (booked) {
            this.bookedAt = new Timestamp(System.currentTimeMillis());
        } else {
            this.bookedAt = null;
        }
    }
}
