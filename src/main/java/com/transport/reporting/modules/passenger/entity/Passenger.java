package com.transport.reporting.modules.passenger.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PASSENGER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "passengerId")
    private Long passengerId;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phoneNumber", length = 30)
    private String phoneNumber;

    @Column(name = "emailVerified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;
}
