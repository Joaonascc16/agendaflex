package br.com.agendaflex.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.agendaflex.dtos.HorarioTrabalhoDTO;
import br.com.agendaflex.entity.Estabelecimento;
import br.com.agendaflex.entity.HorarioTrabalho;
import br.com.agendaflex.entity.Profissional;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.repository.HorarioTrabalhoRepository;
import br.com.agendaflex.repository.ProfissionalRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HorarioTrabalhoServiceTest {

    @Mock
    private HorarioTrabalhoRepository horarioRepository;

    @Mock
    private ProfissionalRepository profissionalRepository;

    @Mock
    private TenantContext tenantContext;

    private HorarioTrabalhoService horarioService;
    private Profissional profissional;

    @BeforeEach
    void preparar() {
        Estabelecimento estabelecimento = new Estabelecimento("Principal", "principal", null, null);
        estabelecimento.setId(1L);
        when(tenantContext.obterEstabelecimentoId()).thenReturn(1L);
        lenient().when(tenantContext.obterEstabelecimento()).thenReturn(estabelecimento);
        horarioService = new HorarioTrabalhoService(horarioRepository, profissionalRepository, tenantContext);
        profissional = new Profissional("Ana", "Cabeleireira");
        profissional.setId(1L);

        lenient().when(profissionalRepository.findByIdAndEstabelecimentoId(1L, 1L)).thenReturn(Optional.of(profissional));
    }

    @Test
    @DisplayName("Cadastra uma janela de expediente válida")
    void cadastraHorarioValido() {
        HorarioTrabalhoDTO dto = novoHorario(LocalTime.of(8, 0), LocalTime.of(12, 0));
        when(horarioRepository.findByEstabelecimentoIdAndProfissionalIdAndDiaSemanaOrderByHoraInicio(1L, 1L, DayOfWeek.MONDAY))
                .thenReturn(List.of());
        when(horarioRepository.save(any(HorarioTrabalho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        HorarioTrabalhoDTO salvo = horarioService.adicionar(dto);

        assertThat(salvo.getProfissionalId()).isEqualTo(1L);
        assertThat(salvo.getDiaSemana()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(salvo.getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(salvo.getHoraFim()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("Rejeita uma janela que termina antes de começar")
    void rejeitaHorarioInvertido() {
        HorarioTrabalhoDTO dto = novoHorario(LocalTime.of(14, 0), LocalTime.of(12, 0));

        assertThatThrownBy(() -> horarioService.adicionar(dto))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("hora final");

        verify(horarioRepository, never()).save(any(HorarioTrabalho.class));
    }

    @Test
    @DisplayName("Rejeita janelas que se sobrepõem no mesmo dia")
    void rejeitaSobreposicao() {
        HorarioTrabalho existente = new HorarioTrabalho(
                profissional, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        HorarioTrabalhoDTO dto = novoHorario(LocalTime.of(11, 30), LocalTime.of(15, 0));

        when(horarioRepository.findByEstabelecimentoIdAndProfissionalIdAndDiaSemanaOrderByHoraInicio(1L, 1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existente));

        assertThatThrownBy(() -> horarioService.adicionar(dto))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("se sobrepõe");

        verify(horarioRepository, never()).save(any(HorarioTrabalho.class));
    }

    @Test
    @DisplayName("Permite janelas consecutivas sem intervalo entre elas")
    void permiteHorariosQueApenasSeEncostam() {
        HorarioTrabalho existente = new HorarioTrabalho(
                profissional, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        HorarioTrabalhoDTO dto = novoHorario(LocalTime.of(12, 0), LocalTime.of(17, 0));

        when(horarioRepository.findByEstabelecimentoIdAndProfissionalIdAndDiaSemanaOrderByHoraInicio(1L, 1L, DayOfWeek.MONDAY))
                .thenReturn(List.of(existente));
        when(horarioRepository.save(any(HorarioTrabalho.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        HorarioTrabalhoDTO salvo = horarioService.adicionar(dto);

        assertThat(salvo.getHoraInicio()).isEqualTo(LocalTime.of(12, 0));
        assertThat(salvo.getHoraFim()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    @DisplayName("Remove uma janela de expediente existente")
    void removeHorarioExistente() {
        HorarioTrabalho horario = new HorarioTrabalho(
                profissional, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0));
        horario.setId(5L);
        when(horarioRepository.findByIdAndEstabelecimentoId(5L, 1L)).thenReturn(Optional.of(horario));

        horarioService.remover(5L);

        verify(horarioRepository).delete(horario);
    }

    private HorarioTrabalhoDTO novoHorario(LocalTime inicio, LocalTime fim) {
        HorarioTrabalhoDTO dto = new HorarioTrabalhoDTO();
        dto.setProfissionalId(1L);
        dto.setDiaSemana(DayOfWeek.MONDAY);
        dto.setHoraInicio(inicio);
        dto.setHoraFim(fim);
        return dto;
    }
}
