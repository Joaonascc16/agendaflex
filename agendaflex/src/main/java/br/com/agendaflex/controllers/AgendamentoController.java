package br.com.agendaflex.controllers;

import br.com.agendaflex.dtos.AgendamentoDTO;
import br.com.agendaflex.dtos.AgendamentoResumoDTO;
import br.com.agendaflex.dtos.ProfissionalDTO;
import br.com.agendaflex.enums.StatusAgendamento;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.service.AgendamentoService;
import br.com.agendaflex.service.ClienteService;
import br.com.agendaflex.service.ProfissionalService;
import br.com.agendaflex.service.ServicoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/agendamentos")
@RequiredArgsConstructor
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final ClienteService clienteService;
    private final ProfissionalService profissionalService;
    private final ServicoService servicoService;

    // ------------------------------------------------------------------
    // Listagem e detalhe
    // ------------------------------------------------------------------

    @GetMapping
    public String listar(@RequestParam(required = false) StatusAgendamento status,
                         @RequestParam(required = false) Long profissionalId,
                         Model model) {
        model.addAttribute("agendamentos", agendamentoService.listar(status, profissionalId));
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("profissionalSelecionado", profissionalId);
        model.addAttribute("statusOpcoes", StatusAgendamento.values());
        model.addAttribute("profissionais", profissionalService.listar());
        return "agendamentos/lista";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("agendamento", agendamentoService.buscar(id));
        return "agendamentos/detalhe";
    }

    // ------------------------------------------------------------------
    // Novo agendamento: serviço -> profissional -> data -> horário -> cliente
    // ------------------------------------------------------------------

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Long servicoId,
                       @RequestParam(required = false) Long profissionalId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                       @RequestParam(required = false) Long clienteId,
                       Model model) {
        List<ProfissionalDTO> profissionais = servicoId != null
                ? profissionalService.listarPorServico(servicoId)
                : profissionalService.listarAtivos();

        // Se o serviço mudou e o profissional escolhido não o realiza, a escolha é descartada.
        boolean profissionalValido = profissionalId != null
                && profissionais.stream().anyMatch(p -> p.getId().equals(profissionalId));
        Long profissionalEscolhido = profissionalValido ? profissionalId : null;

        model.addAttribute("servicos", servicoService.listarAtivos());
        model.addAttribute("profissionais", profissionais);
        model.addAttribute("clientes", clienteService.listarAtivos());
        model.addAttribute("servicoId", servicoId);
        model.addAttribute("profissionalId", profissionalEscolhido);
        model.addAttribute("clienteId", clienteId);
        model.addAttribute("data", data);
        model.addAttribute("hoje", LocalDate.now());

        if (servicoId != null && profissionalEscolhido != null && data != null) {
            model.addAttribute("slots", agendamentoService.horariosDisponiveis(profissionalEscolhido, servicoId, data));
        }
        return "agendamentos/novo";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute AgendamentoDTO dto, BindingResult result, RedirectAttributes ra) {
        LocalDate data = dto.getInicio() != null ? dto.getInicio().toLocalDate() : null;

        if (result.hasErrors()) {
            ra.addFlashAttribute("erro", "Escolha o cliente e um horário disponível para continuar.");
            return "redirect:/agendamentos/novo"
                    + parametros(dto.getServicoId(), dto.getProfissionalId(), data, dto.getClienteId());
        }
        try {
            AgendamentoResumoDTO criado = agendamentoService.criar(dto);
            ra.addFlashAttribute("sucesso", "Agendamento criado para " + criado.getClienteNome() + ".");
            return "redirect:/agendamentos/" + criado.getId();
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/agendamentos/novo"
                    + parametros(dto.getServicoId(), dto.getProfissionalId(), data, dto.getClienteId());
        }
    }

    // ------------------------------------------------------------------
    // Ações sobre um agendamento
    // ------------------------------------------------------------------

    @PostMapping("/{id}/confirmar")
    public String confirmar(@PathVariable Long id, RedirectAttributes ra) {
        executar(ra, "Agendamento confirmado.", () -> agendamentoService.confirmar(id));
        return "redirect:/agendamentos/" + id;
    }

    @PostMapping("/{id}/concluir")
    public String concluir(@PathVariable Long id, RedirectAttributes ra) {
        executar(ra, "Atendimento concluído.", () -> agendamentoService.concluir(id));
        return "redirect:/agendamentos/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, @RequestParam(required = false) String motivo,
                           RedirectAttributes ra) {
        executar(ra, "Agendamento cancelado.", () -> agendamentoService.cancelar(id, motivo));
        return "redirect:/agendamentos/" + id;
    }

    @PostMapping("/{id}/faltou")
    public String registrarFalta(@PathVariable Long id, RedirectAttributes ra) {
        try {
            AgendamentoResumoDTO resumo = agendamentoService.registrarFalta(id);
            String mensagem = "Falta registrada.";
            if (clienteService.buscar(resumo.getClienteId()).isBloqueado()) {
                mensagem += " O cliente atingiu o limite de faltas e está bloqueado para novos agendamentos.";
            }
            ra.addFlashAttribute("sucesso", mensagem);
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/agendamentos/" + id;
    }

    @GetMapping("/{id}/reagendar")
    public String formularioReagendar(@PathVariable Long id,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                      Model model) {
        AgendamentoResumoDTO agendamento = agendamentoService.buscar(id);
        model.addAttribute("agendamento", agendamento);
        model.addAttribute("data", data);
        model.addAttribute("hoje", LocalDate.now());
        if (data != null) {
            model.addAttribute("slots", agendamentoService.horariosDisponiveis(
                    agendamento.getProfissionalId(), agendamento.getServicoId(), data, id));
        }
        return "agendamentos/reagendar";
    }

    @PostMapping("/{id}/reagendar")
    public String reagendar(@PathVariable Long id,
                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime inicio,
                            RedirectAttributes ra) {
        if (inicio == null) {
            ra.addFlashAttribute("erro", "Escolha um novo horário.");
            return "redirect:/agendamentos/" + id + "/reagendar";
        }
        try {
            agendamentoService.reagendar(id, inicio);
            ra.addFlashAttribute("sucesso", "Agendamento reagendado. Ele volta para \"Agendado\" até ser confirmado de novo.");
            return "redirect:/agendamentos/" + id;
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
            return "redirect:/agendamentos/" + id + "/reagendar";
        }
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private void executar(RedirectAttributes ra, String mensagemDeSucesso, Runnable acao) {
        try {
            acao.run();
            ra.addFlashAttribute("sucesso", mensagemDeSucesso);
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
    }

    private String parametros(Long servicoId, Long profissionalId, LocalDate data, Long clienteId) {
        List<String> partes = new ArrayList<>();
        if (servicoId != null) {
            partes.add("servicoId=" + servicoId);
        }
        if (profissionalId != null) {
            partes.add("profissionalId=" + profissionalId);
        }
        if (data != null) {
            partes.add("data=" + data);
        }
        if (clienteId != null) {
            partes.add("clienteId=" + clienteId);
        }
        return partes.isEmpty() ? "" : "?" + String.join("&", partes);
    }
}
