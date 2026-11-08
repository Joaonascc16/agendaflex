package br.com.agendaflex.controllers;

import br.com.agendaflex.dtos.BloqueioDTO;
import br.com.agendaflex.dtos.HorarioTrabalhoDTO;
import br.com.agendaflex.dtos.OpcaoDTO;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.service.BloqueioService;
import br.com.agendaflex.service.HorarioTrabalhoService;
import br.com.agendaflex.service.ProfissionalService;
import br.com.agendaflex.util.TextoUtil;
import jakarta.validation.Valid;
import java.time.DayOfWeek;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Expediente semanal e bloqueios (férias, folgas, reuniões) de um profissional. */
@Controller
@RequestMapping("/profissionais/{profissionalId}/disponibilidade")
@RequiredArgsConstructor
public class DisponibilidadeController {

    private static final String VIEW = "profissionais/disponibilidade";

    private final ProfissionalService profissionalService;
    private final HorarioTrabalhoService horarioTrabalhoService;
    private final BloqueioService bloqueioService;

    @GetMapping
    public String exibir(@PathVariable Long profissionalId, Model model) {
        carregar(profissionalId, model);
        return VIEW;
    }

    @PostMapping("/horarios")
    public String adicionarHorario(@PathVariable Long profissionalId,
                                   @Valid @ModelAttribute("novoHorario") HorarioTrabalhoDTO dto,
                                   BindingResult result, Model model, RedirectAttributes ra) {
        dto.setProfissionalId(profissionalId);
        if (result.hasErrors()) {
            carregar(profissionalId, model);
            return VIEW;
        }
        try {
            horarioTrabalhoService.adicionar(dto);
            ra.addFlashAttribute("sucesso", "Horário de trabalho adicionado.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return voltar(profissionalId);
    }

    @PostMapping("/horarios/{horarioId}/remover")
    public String removerHorario(@PathVariable Long profissionalId, @PathVariable Long horarioId,
                                 RedirectAttributes ra) {
        horarioTrabalhoService.remover(horarioId);
        ra.addFlashAttribute("sucesso", "Horário de trabalho removido.");
        return voltar(profissionalId);
    }

    @PostMapping("/bloqueios")
    public String adicionarBloqueio(@PathVariable Long profissionalId,
                                    @Valid @ModelAttribute("novoBloqueio") BloqueioDTO dto,
                                    BindingResult result, Model model, RedirectAttributes ra) {
        dto.setProfissionalId(profissionalId);
        if (result.hasErrors()) {
            carregar(profissionalId, model);
            return VIEW;
        }
        try {
            bloqueioService.criar(dto);
            ra.addFlashAttribute("sucesso", "Bloqueio criado. Esse período não aparece mais para agendamento.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return voltar(profissionalId);
    }

    @PostMapping("/bloqueios/{bloqueioId}/remover")
    public String removerBloqueio(@PathVariable Long profissionalId, @PathVariable Long bloqueioId,
                                  RedirectAttributes ra) {
        bloqueioService.remover(bloqueioId);
        ra.addFlashAttribute("sucesso", "Bloqueio removido.");
        return voltar(profissionalId);
    }

    private void carregar(Long profissionalId, Model model) {
        model.addAttribute("profissional", profissionalService.buscar(profissionalId));
        model.addAttribute("horarios", horarioTrabalhoService.listarPorProfissional(profissionalId));
        model.addAttribute("bloqueios", bloqueioService.listarVigentes(profissionalId));
        model.addAttribute("diasSemana", Arrays.stream(DayOfWeek.values())
                .map(dia -> new OpcaoDTO(dia.name(), TextoUtil.nomeDoDia(dia)))
                .toList());
        if (!model.containsAttribute("novoHorario")) {
            model.addAttribute("novoHorario", new HorarioTrabalhoDTO());
        }
        if (!model.containsAttribute("novoBloqueio")) {
            model.addAttribute("novoBloqueio", new BloqueioDTO());
        }
    }

    private String voltar(Long profissionalId) {
        return "redirect:/profissionais/" + profissionalId + "/disponibilidade";
    }
}
