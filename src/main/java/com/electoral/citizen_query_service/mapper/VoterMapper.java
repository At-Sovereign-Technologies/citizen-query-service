package com.electoral.citizen_query_service.mapper;

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

        return response;
    }
}