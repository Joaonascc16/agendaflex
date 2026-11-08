package br.com.agendaflex.controllers;

import br.com.agendaflex.dtos.ProfissionalDTO;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.service.ProfissionalService;
import br.com.agendaflex.service.ServicoService;
import jakarta.validation.Valid;
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

@Controller
@RequestMapping("/profissionais")
@RequiredArgsConstructor
public class ProfissionalController {

    private final ProfissionalService profissionalService;
    private final ServicoService servicoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("profissionais", profissionalService.listar());
        return "profissionais/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("profissional", new ProfissionalDTO());
        model.addAttribute("servicos", servicoService.listar());
        return "profissionais/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("profissional", profissionalService.buscar(id));
        model.addAttribute("servicos", servicoService.listar());
        return "profissionais/form";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("profissional") ProfissionalDTO profissional, BindingResult result,
                         Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("servicos", servicoService.listar());
            return "profissionais/form";
        }
        try {
            profissionalService.salvar(profissional);
            ra.addFlashAttribute("sucesso", "Profissional salvo.");
            return "redirect:/profissionais";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            model.addAttribute("servicos", servicoService.listar());
            return "profissionais/form";
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        try {
            profissionalService.excluir(id);
            ra.addFlashAttribute("sucesso", "Profissional excluído.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/profissionais";
    }
}
