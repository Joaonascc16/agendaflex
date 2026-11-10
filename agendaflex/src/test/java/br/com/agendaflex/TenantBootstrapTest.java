package br.com.agendaflex;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.agendaflex.repository.AgendamentoRepository;
import br.com.agendaflex.repository.BloqueioRepository;
import br.com.agendaflex.repository.ClienteRepository;
import br.com.agendaflex.repository.EstabelecimentoRepository;
import br.com.agendaflex.repository.HorarioTrabalhoRepository;
import br.com.agendaflex.repository.ProfissionalRepository;
import br.com.agendaflex.repository.ServicoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "agendaflex.dados-demo=true")
class TenantBootstrapTest {

    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProfissionalRepository profissionalRepository;
    @Autowired private ServicoRepository servicoRepository;
    @Autowired private HorarioTrabalhoRepository horarioTrabalhoRepository;
    @Autowired private BloqueioRepository bloqueioRepository;
    @Autowired private AgendamentoRepository agendamentoRepository;

    @Test
    void criaEstabelecimentoPadraoEAssociaDadosLegados() {
        var estabelecimento = estabelecimentoRepository
                .findBySlug("agendaflex-principal")
                .orElseThrow();

        assertThat(estabelecimento.getNome()).isEqualTo("AgendaFlex Principal");
        assertThat(clienteRepository.findAll()).isNotEmpty()
                .allMatch(cliente -> cliente.getEstabelecimento() != null
                        && cliente.getEstabelecimento().getId().equals(estabelecimento.getId()));
        assertThat(profissionalRepository.findAll()).isNotEmpty()
                .allMatch(profissional -> profissional.getEstabelecimento() != null
                        && profissional.getEstabelecimento().getId().equals(estabelecimento.getId()));
        assertThat(servicoRepository.findAll()).isNotEmpty()
                .allMatch(servico -> servico.getEstabelecimento() != null
                        && servico.getEstabelecimento().getId().equals(estabelecimento.getId()));
        assertThat(horarioTrabalhoRepository.findAll()).isNotEmpty()
                .allMatch(horario -> horario.getEstabelecimento() != null
                        && horario.getEstabelecimento().getId().equals(estabelecimento.getId()));
        assertThat(bloqueioRepository.findAll()).isNotEmpty()
                .allMatch(bloqueio -> bloqueio.getEstabelecimento() != null
                        && bloqueio.getEstabelecimento().getId().equals(estabelecimento.getId()));
        assertThat(agendamentoRepository.findAll()).isNotEmpty()
                .allMatch(agendamento -> agendamento.getEstabelecimento() != null
                        && agendamento.getEstabelecimento().getId().equals(estabelecimento.getId()));
    }
}
