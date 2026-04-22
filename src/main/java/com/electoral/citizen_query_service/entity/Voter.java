package com.electoral.citizen_query_service.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class Voter {

    @Id
    private String document;
    private String pollingStation;
    private Boolean hasVoted;
    private Boolean hasFines;
    private LocalDate birthDate;
}