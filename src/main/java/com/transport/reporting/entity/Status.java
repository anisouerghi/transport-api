package com.transport.reporting.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "STATUS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Status {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statusId")
    private Long statusId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "displayOrder", nullable = false)
    private Integer displayOrder;
}
