package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SUPPORT_TYPE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supportTypeId")
    private Long supportTypeId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;
}
