package com.electoral.citizen_query_service.mapper;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Component;

import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.entity.Voter;

@Component
public class VoterMapper {

    public VoterResponse toResponse(Voter voter) {
        VoterResponse response = new VoterResponse();

        response.setDocument(voter.getDocument());
        response.setPollingStation(voter.getPollingStation());

        response.setStatus(
                Boolean.TRUE.equals(voter.getHasVoted())
                        ? "VOTED"
                        : "NOT_VOTED"
        );

        response.setHasFines(voter.getHasFines());

        LocalDate birthDate = voter.getBirthDate();

        if (birthDate != null) {
            int age = Period.between(birthDate, LocalDate.now()).getYears();

            boolean isMandatory = age >= 21 && age <= 60;

            response.setIsMandatory(isMandatory);
        } else {
            response.setIsMandatory(null); // o false si prefieres
        }

        return response;
    }
}