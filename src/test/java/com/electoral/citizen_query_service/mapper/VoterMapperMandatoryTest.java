package com.electoral.citizen_query_service.mapper;

// ============================================================
//  TIPO: Unitaria — VoterMapper · Lógica de obligatoriedad
//  Atributos: Funcionalidad (RN-01) | Correctitud de límites
//
//  NOTA PARA DEVS (2026-05-17):
//  El rango implementado actualmente es age >= 21 && age <= 60.
//  RN-01 especifica 20-70 (inclusive). Los tests TC-CQ-001 a
//  TC-CQ-005 verifican el comportamiento ACTUAL del código.
//  Si RN-01 es intencional, actualizar VoterMapper.java línea 33
//  a: boolean isMandatory = age >= 20 && age <= 70;
// ============================================================

import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.entity.Voter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VoterMapper — Lógica de obligatoriedad (RN-01) y estado de voto")
class VoterMapperMandatoryTest {

    private VoterMapper mapper;

    // Fecha de referencia fija para todos los cálculos de edad.
    // Hoy = 2026-05-17 según la sesión de QA.
    private static final LocalDate HOY = LocalDate.of(2026, 5, 17);

    @BeforeEach
    void setUp() {
        mapper = new VoterMapper();
    }

    // ------------------------------------------------------------------
    // TC-CQ-001 | Límite inferior del rango obligatorio
    // Ciudadano de 21 años exactos → isMandatory = true
    // (El mapper usa >= 21; a 20 años el resultado es false —
    //  ver NOTA PARA DEVS sobre discrepancia con RN-01)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-001 | RN-01 | 21 años exactos → isMandatory=true (límite inferior implementado)")
    void tc_cq_001_veintiun_anios_exactos_es_obligatorio() {
        // birthDate = 2005-05-17 → 21 años el 2026-05-17
        Voter voter = voterConFechaNacimiento(LocalDate.of(2005, 5, 17));

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getIsMandatory())
                .as("21 años exactos debe estar dentro del rango obligatorio")
                .isTrue();
    }

    // ------------------------------------------------------------------
    // TC-CQ-002 | Un año por debajo del límite inferior
    // Ciudadano de 20 años → isMandatory = false (con rango actual 21-60)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-002 | RN-01 | 20 años → isMandatory=false (por debajo del rango 21-60 implementado)")
    void tc_cq_002_veinte_anios_no_es_obligatorio_con_rango_actual() {
        // birthDate = 2006-05-17 → 20 años el 2026-05-17
        Voter voter = voterConFechaNacimiento(LocalDate.of(2006, 5, 17));

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getIsMandatory())
                .as("20 años queda fuera del rango 21-60 actualmente implementado")
                .isFalse();
    }

    // ------------------------------------------------------------------
    // TC-CQ-003 | Límite superior del rango obligatorio
    // Ciudadano de 60 años exactos → isMandatory = true
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-003 | RN-01 | 60 años exactos → isMandatory=true (límite superior implementado)")
    void tc_cq_003_sesenta_anios_exactos_es_obligatorio() {
        // birthDate = 1966-05-17 → 60 años el 2026-05-17
        Voter voter = voterConFechaNacimiento(LocalDate.of(1966, 5, 17));

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getIsMandatory())
                .as("60 años exactos debe estar dentro del rango obligatorio")
                .isTrue();
    }

    // ------------------------------------------------------------------
    // TC-CQ-004 | Un año por encima del límite superior
    // Ciudadano de 61 años → isMandatory = false (con rango actual 21-60)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-004 | RN-01 | 61 años → isMandatory=false (por encima del rango 21-60 implementado)")
    void tc_cq_004_sesenta_un_anios_no_es_obligatorio_con_rango_actual() {
        // birthDate = 1965-05-17 → 61 años el 2026-05-17
        Voter voter = voterConFechaNacimiento(LocalDate.of(1965, 5, 17));

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getIsMandatory())
                .as("61 años queda fuera del rango 21-60 actualmente implementado")
                .isFalse();
    }

    // ------------------------------------------------------------------
    // TC-CQ-005 | Valor central dentro del rango
    // Ciudadano de 40 años → isMandatory = true
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-005 | RN-01 | 40 años (valor central) → isMandatory=true")
    void tc_cq_005_cuarenta_anios_es_obligatorio() {
        // birthDate = 1986-01-15 → 40 años el 2026-05-17
        Voter voter = voterConFechaNacimiento(LocalDate.of(1986, 1, 15));

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getIsMandatory()).isTrue();
    }

    // ------------------------------------------------------------------
    // TC-CQ-006 | birthDate = null → isMandatory = null
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-006 | RN-01 | birthDate=null → isMandatory=null")
    void tc_cq_006_sin_fecha_nacimiento_mandatory_es_null() {
        Voter voter = new Voter();
        voter.setDocument("000000001");
        voter.setPollingStation("Mesa X");
        voter.setHasVoted(false);
        voter.setBirthDate(null);

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getIsMandatory())
                .as("Sin fecha de nacimiento isMandatory debe ser null")
                .isNull();
    }

    // ------------------------------------------------------------------
    // TC-CQ-007 | Paramétrico: varios valores fuera del rango
    // ------------------------------------------------------------------
    @ParameterizedTest(name = "Edad {0} años (birthDate {1}) → isMandatory=false")
    @CsvSource({
        "19, 2007-05-17",   // 19 años — menor de 21
        "20, 2006-05-17",   // 20 años — menor de 21
        "61, 1965-05-17",   // 61 años — mayor de 60
        "70, 1956-05-17",   // 70 años — mayor de 60
        "80, 1946-01-01"    // 80 años — mayor de 60
    })
    @DisplayName("TC-CQ-007 | RN-01 | Edades fuera del rango 21-60 → isMandatory=false")
    void tc_cq_007_edades_fuera_de_rango_no_son_obligatorias(int edadEsperada, LocalDate birthDate) {
        Voter voter = voterConFechaNacimiento(birthDate);

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getIsMandatory())
                .as("Edad %d no debe ser obligatoria con rango 21-60", edadEsperada)
                .isFalse();
    }

    // ------------------------------------------------------------------
    // TC-CQ-008 | hasVoted=true → status="VOTED"
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-008 | Ciudadano que ya votó → status=VOTED")
    void tc_cq_008_hasVoted_true_produce_status_VOTED() {
        Voter voter = new Voter();
        voter.setDocument("111111111");
        voter.setPollingStation("Mesa 1");
        voter.setHasVoted(true);
        voter.setBirthDate(LocalDate.of(1990, 6, 15)); // 35 años, obligatorio

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getStatus()).isEqualTo("VOTED");
        assertThat(response.getIsMandatory()).isTrue();
    }

    // ------------------------------------------------------------------
    // TC-CQ-009 | hasVoted=false → status="NOT_VOTED"
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-009 | Ciudadano que no ha votado → status=NOT_VOTED")
    void tc_cq_009_hasVoted_false_produce_status_NOT_VOTED() {
        Voter voter = new Voter();
        voter.setDocument("222222222");
        voter.setPollingStation("Mesa 2");
        voter.setHasVoted(false);

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getStatus()).isEqualTo("NOT_VOTED");
    }

    // ------------------------------------------------------------------
    // TC-CQ-010 | hasVoted=null → status="NOT_VOTED"
    // (Boolean.TRUE.equals(null) = false → rama else)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-010 | hasVoted=null → status=NOT_VOTED (null-safe)")
    void tc_cq_010_hasVoted_null_produce_status_NOT_VOTED() {
        Voter voter = new Voter();
        voter.setDocument("333333333");
        voter.setPollingStation("Mesa 3");
        voter.setHasVoted(null);

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getStatus()).isEqualTo("NOT_VOTED");
    }

    // ------------------------------------------------------------------
    // TC-CQ-011 | hasFines=true se mapea correctamente
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-011 | hasFines=true se propaga al response")
    void tc_cq_011_hasFines_true_se_mapea() {
        Voter voter = new Voter();
        voter.setDocument("444444444");
        voter.setPollingStation("Mesa 4");
        voter.setHasVoted(false);
        voter.setHasFines(true);

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getHasFines()).isTrue();
    }

    // ------------------------------------------------------------------
    // TC-CQ-012 | hasFines=false se mapea correctamente
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-012 | hasFines=false se propaga al response")
    void tc_cq_012_hasFines_false_se_mapea() {
        Voter voter = new Voter();
        voter.setDocument("555555555");
        voter.setPollingStation("Mesa 5");
        voter.setHasVoted(false);
        voter.setHasFines(false);

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getHasFines()).isFalse();
    }

    // ------------------------------------------------------------------
    // TC-CQ-013 | pollingStation se mapea sin alteración
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-013 | pollingStation se propaga sin modificación")
    void tc_cq_013_pollingStation_no_se_altera() {
        String station = "Colegio Nacional Camilo Torres - Mesa 7 - Planta 2";
        Voter voter = new Voter();
        voter.setDocument("666666666");
        voter.setPollingStation(station);
        voter.setHasVoted(false);

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getPollingStation()).isEqualTo(station);
    }

    // ------------------------------------------------------------------
    // TC-CQ-014 | Ciudadano en día exacto de cumpleaños
    // Nacido el mismo día que la "fecha actual" del test → edad exacta
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-014 | Ciudadano cumple 21 hoy → isMandatory=true desde hoy")
    void tc_cq_014_cumple_21_hoy_es_obligatorio_desde_este_dia() {
        // Si nació el 2005-05-17 y hoy es 2026-05-17 → 21 años exactos
        Voter voter = voterConFechaNacimiento(LocalDate.of(2005, 5, 17));

        VoterResponse response = mapper.toResponse(voter);

        assertThat(response.getIsMandatory()).isTrue();
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------
    private Voter voterConFechaNacimiento(LocalDate birthDate) {
        Voter voter = new Voter();
        voter.setDocument("100000001");
        voter.setPollingStation("Mesa Test");
        voter.setHasVoted(false);
        voter.setHasFines(false);
        voter.setBirthDate(birthDate);
        return voter;
    }
}
