package com.electoral.citizen_query_service.service;

// ============================================================
//  TIPO: Unitaria — CitizenService
//  Verifica: lógica de caché Redis, fallback y consulta a BD
//  Stack: JUnit 5 + Mockito (sin Spring context)
// ============================================================

import com.electoral.citizen_query_service.cache.RedisCacheAdapter;
import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.entity.Voter;
import com.electoral.citizen_query_service.exception.ResourceNotFoundException;
import com.electoral.citizen_query_service.mapper.VoterMapper;
import com.electoral.citizen_query_service.repository.VoterRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CitizenService — Lógica de caché y consulta a BD")
class CitizenServiceTest {

    @Mock private VoterRepository repository;
    @Mock private RedisCacheAdapter cache;
    @Mock private VoterMapper mapper;
    @InjectMocks private CitizenService service;

    // ObjectMapper real: el servicio lo usa para convertir el objeto cacheado
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @BeforeEach
    void injectRealObjectMapper() throws Exception {
        // Inyectamos el ObjectMapper real via reflexión porque @InjectMocks
        // no lo inyecta automáticamente (es final/no-mock)
        var field = CitizenService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(service, objectMapper);
    }

    // ------------------------------------------------------------------
    // TC-CQ-015 | CACHE HIT: segunda consulta no toca la BD
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-015 | Cache HIT — retorna respuesta cacheada sin consultar BD")
    void tc_cq_015_cache_hit_no_consulta_bd() {
        VoterResponse cached = respuestaBase("123456789");
        // El adaptador devuelve un LinkedHashMap (como lo haría Redis/Jackson)
        Object rawCache = objectMapper.convertValue(cached, Object.class);
        when(cache.get("voter:123456789")).thenReturn(rawCache);

        VoterResponse result = service.getVoterInfo("123456789");

        assertThat(result.getDocument()).isEqualTo("123456789");
        assertThat(result.getPollingStation()).isEqualTo("Mesa Test");
        // BD no debe ser consultada
        verify(repository, never()).findById(any());
        // Ni se debe guardar nada en caché
        verify(cache, never()).set(any(), any());
    }

    // ------------------------------------------------------------------
    // TC-CQ-016 | CACHE MISS: consulta BD y almacena en caché
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-016 | Cache MISS — consulta BD y guarda en Redis")
    void tc_cq_016_cache_miss_consulta_bd_y_almacena() {
        when(cache.get("voter:987654321")).thenReturn(null);

        Voter voter = voterBase("987654321");
        when(repository.findById("987654321")).thenReturn(Optional.of(voter));

        VoterResponse expected = respuestaBase("987654321");
        when(mapper.toResponse(voter)).thenReturn(expected);

        VoterResponse result = service.getVoterInfo("987654321");

        assertThat(result).isEqualTo(expected);
        // Debe guardar en caché el resultado
        ArgumentCaptor<Object> cachedValue = ArgumentCaptor.forClass(Object.class);
        verify(cache).set(eq("voter:987654321"), cachedValue.capture());
        assertThat(cachedValue.getValue()).isEqualTo(expected);
    }

    // ------------------------------------------------------------------
    // TC-CQ-017 | Voter no encontrado → ResourceNotFoundException
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-017 | Voter no registrado → lanza ResourceNotFoundException")
    void tc_cq_017_voter_no_encontrado_lanza_excepcion() {
        when(cache.get("voter:999999999")).thenReturn(null);
        when(repository.findById("999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getVoterInfo("999999999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Voter not found");

        // No debe intentar almacenar en caché
        verify(cache, never()).set(any(), any());
    }

    // ------------------------------------------------------------------
    // TC-CQ-018 | Objeto cacheado malformado → fallback a BD
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-018 | Objeto cacheado malformado → fallback transparente a BD")
    void tc_cq_018_cache_malformado_usa_bd_como_fallback() {
        // El caché devuelve un entero en lugar del objeto esperado
        when(cache.get("voter:111222333")).thenReturn(42);

        Voter voter = voterBase("111222333");
        when(repository.findById("111222333")).thenReturn(Optional.of(voter));

        VoterResponse expected = respuestaBase("111222333");
        when(mapper.toResponse(voter)).thenReturn(expected);

        VoterResponse result = service.getVoterInfo("111222333");

        assertThat(result).isEqualTo(expected);
        // A pesar del error de conversión, el resultado viene de BD
        verify(repository).findById("111222333");
    }

    // ------------------------------------------------------------------
    // TC-CQ-019 | Cache devuelve null explícito → va a BD
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-019 | cache.get() retorna null → consulta BD normalmente")
    void tc_cq_019_cache_null_consulta_bd() {
        when(cache.get("voter:555666777")).thenReturn(null);

        Voter voter = voterBase("555666777");
        when(repository.findById("555666777")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(respuestaBase("555666777"));

        service.getVoterInfo("555666777");

        verify(repository, times(1)).findById("555666777");
    }

    // ------------------------------------------------------------------
    // TC-CQ-020 | La clave Redis tiene el formato "voter:{document}"
    // ------------------------------------------------------------------
    @Test
    @DisplayName("TC-CQ-020 | La clave de caché es 'voter:{document}'")
    void tc_cq_020_clave_redis_tiene_formato_correcto() {
        when(cache.get("voter:DOC-001")).thenReturn(null);

        Voter voter = voterBase("DOC-001");
        when(repository.findById("DOC-001")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(respuestaBase("DOC-001"));

        service.getVoterInfo("DOC-001");

        verify(cache).get("voter:DOC-001");
        verify(cache).set(eq("voter:DOC-001"), any());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private Voter voterBase(String document) {
        Voter v = new Voter();
        v.setDocument(document);
        v.setPollingStation("Mesa Test");
        v.setHasVoted(false);
        v.setHasFines(false);
        v.setBirthDate(LocalDate.of(1990, 1, 1));
        return v;
    }

    private VoterResponse respuestaBase(String document) {
        VoterResponse r = new VoterResponse();
        r.setDocument(document);
        r.setPollingStation("Mesa Test");
        r.setStatus("NOT_VOTED");
        r.setHasFines(false);
        r.setIsMandatory(true);
        return r;
    }
}
