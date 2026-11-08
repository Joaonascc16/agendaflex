package br.com.agendaflex.config;

import br.com.agendaflex.entity.Agendamento;
import br.com.agendaflex.entity.Bloqueio;
import br.com.agendaflex.entity.Cliente;
import br.com.agendaflex.entity.HorarioTrabalho;
import br.com.agendaflex.entity.Profissional;
import br.com.agendaflex.entity.Servico;
import br.com.agendaflex.enums.StatusAgendamento;
import br.com.agendaflex.repository.AgendamentoRepository;
import br.com.agendaflex.repository.BloqueioRepository;
import br.com.agendaflex.repository.ClienteRepository;
import br.com.agendaflex.repository.HorarioTrabalhoRepository;
import br.com.agendaflex.repository.ProfissionalRepository;
import br.com.agendaflex.repository.ServicoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados de exemplo para você ver o sistema funcionando assim que subir.
 * Desligue com agendaflex.dados-demo=false em application.properties.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agendaflex.dados-demo", havingValue = "true")
public class DadosIniciais implements CommandLineRunner {

    private final ServicoRepository servicoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ClienteRepository clienteRepository;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final BloqueioRepository bloqueioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void run(String... args) {
        if (servicoRepository.count() > 0) {
            return;
        }

        Servico corte = servicoRepository.save(new Servico("Corte de cabelo", "Corte com lavagem e finalização", 45, new BigDecimal("60.00")));
        Servico barba = servicoRepository.save(new Servico("Barba", "Barba com toalha quente", 30, new BigDecimal("40.00")));
        Servico coloracao = servicoRepository.save(new Servico("Coloração", "Coloração completa", 120, new BigDecimal("180.00")));
        Servico massagem = servicoRepository.save(new Servico("Massagem relaxante", "Sessão de 60 minutos", 60, new BigDecimal("120.00")));

        Profissional ana = new Profissional("Ana Souza", "Cabeleireira");
        ana.getServicos().add(corte);
        ana.getServicos().add(coloracao);
        ana = profissionalRepository.save(ana);

        Profissional bruno = new Profissional("Bruno Lima", "Barbeiro");
        bruno.getServicos().add(corte);
        bruno.getServicos().add(barba);
        bruno = profissionalRepository.save(bruno);

        Profissional carla = new Profissional("Carla Mendes", "Massoterapeuta");
        carla.getServicos().add(massagem);
        carla = profissionalRepository.save(carla);

        for (Profissional p : new Profissional[] {ana, bruno, carla}) {
            for (DayOfWeek dia : new DayOfWeek[] {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
                horarioTrabalhoRepository.save(new HorarioTrabalho(p, dia, LocalTime.of(8, 0), LocalTime.of(12, 0)));
                horarioTrabalhoRepository.save(new HorarioTrabalho(p, dia, LocalTime.of(13, 0), LocalTime.of(18, 0)));
            }
            horarioTrabalhoRepository.save(new HorarioTrabalho(p, DayOfWeek.SATURDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        }

        Cliente mariana = clienteRepository.save(new Cliente("Mariana Alves", "mariana@email.com", "(47) 99999-1111"));
        Cliente carlos = clienteRepository.save(new Cliente("Carlos Pereira", "carlos@email.com", "(47) 99999-2222"));
        Cliente fernanda = clienteRepository.save(new Cliente("Fernanda Costa", "fernanda@email.com", "(47) 99999-3333"));
        Cliente rafael = clienteRepository.save(new Cliente("Rafael Santos", "rafael@email.com", "(47) 99999-4444"));
        Cliente juliana = new Cliente("Juliana Rocha", "juliana@email.com", "(47) 99999-5555");
        juliana.setFaltas(3); // exemplo de cliente bloqueado pela regra de faltas
        clienteRepository.save(juliana);

        LocalDate hoje = LocalDate.now(clock);

        // Histórico: atendimentos já realizados e uma falta
        agendar(mariana, ana, corte, diaUtil(hoje, -3), 9, 0, StatusAgendamento.CONCLUIDO);
        agendar(carlos, bruno, barba, diaUtil(hoje, -2), 10, 30, StatusAgendamento.CONCLUIDO);
        agendar(fernanda, carla, massagem, diaUtil(hoje, -1), 14, 0, StatusAgendamento.CONCLUIDO);
        agendar(rafael, bruno, corte, diaUtil(hoje, -1), 16, 0, StatusAgendamento.FALTOU);

        // Próximos dias
        agendar(mariana, ana, coloracao, diaUtil(hoje, 1), 9, 0, StatusAgendamento.CONFIRMADO);
        agendar(carlos, bruno, corte, diaUtil(hoje, 1), 10, 0, StatusAgendamento.AGENDADO);
        agendar(fernanda, carla, massagem, diaUtil(hoje, 2), 15, 0, StatusAgendamento.CONFIRMADO);
        agendar(rafael, bruno, barba, diaUtil(hoje, 2), 11, 0, StatusAgendamento.AGENDADO);
        agendar(mariana, ana, corte, diaUtil(hoje, 3), 14, 30, StatusAgendamento.AGENDADO);

        // Folga da Ana na semana seguinte
        LocalDate folga = diaUtil(hoje, 6);
        bloqueioRepository.save(new Bloqueio(ana, folga.atTime(8, 0), folga.atTime(12, 0), "Consulta médica"));
    }

    private void agendar(Cliente cliente, Profissional profissional, Servico servico,
                         LocalDate dia, int hora, int minuto, StatusAgendamento status) {
        Agendamento a = new Agendamento();
        a.setCliente(cliente);
        a.setProfissional(profissional);
        a.setServico(servico);
        a.setInicio(dia.atTime(hora, minuto));
        a.setFim(servico.calcularFim(a.getInicio()));
        a.setStatus(status);
        a.setPrecoCobrado(servico.getPreco());
        agendamentoRepository.save(a);
    }

    /** Avança (ou volta) N dias úteis a partir de uma data. */
    private LocalDate diaUtil(LocalDate base, int deslocamento) {
        int passo = deslocamento >= 0 ? 1 : -1;
        int restantes = Math.abs(deslocamento);
        LocalDate dia = base;
        while (restantes > 0) {
            dia = dia.plusDays(passo);
            if (dia.getDayOfWeek() != DayOfWeek.SATURDAY && dia.getDayOfWeek() != DayOfWeek.SUNDAY) {
                restantes--;
            }
        }
        return dia;
    }
}
