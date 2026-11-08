package br.com.agendaflex.controllers;

import br.com.agendaflex.dtos.ServicoDTO;
import br.com.agendaflex.exception.RegraNegocioException;
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
@RequestMapping("/servicos")
@RequiredArgsConstructor
public class ServicoController {

    private final ServicoService servicoService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("servicos", servicoService.listar());
        return "servicos/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("servico", new ServicoDTO());
        return "servicos/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("servico", servicoService.buscar(id));
        return "servicos/form";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("servico") ServicoDTO servico, BindingResult result,
                         Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "servicos/form";
        }
        try {
            servicoService.salvar(servico);
            ra.addFlashAttribute("sucesso", "Serviço salvo.");
            return "redirect:/servicos";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return "servicos/form";
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        try {
            servicoService.excluir(id);
            ra.addFlashAttribute("sucesso", "Serviço excluído.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/servicos";
    }
}
