package br.com.agendaflex.controllers;

import br.com.agendaflex.dtos.ClienteDTO;
import br.com.agendaflex.exception.RegraNegocioException;
import br.com.agendaflex.service.AgendamentoService;
import br.com.agendaflex.service.ClienteService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final AgendamentoService agendamentoService;

    @GetMapping
    public String listar(@RequestParam(required = false) String busca, Model model) {
        model.addAttribute("clientes", clienteService.listar(busca));
        model.addAttribute("busca", busca);
        return "clientes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("cliente", new ClienteDTO());
        return "clientes/form";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("cliente", clienteService.buscar(id));
        model.addAttribute("historico", agendamentoService.listarPorCliente(id));
        return "clientes/form";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("cliente") ClienteDTO cliente, BindingResult result,
                         Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            return voltarAoFormulario(cliente, model);
        }
        try {
            clienteService.salvar(cliente);
            ra.addFlashAttribute("sucesso", "Cliente salvo.");
            return "redirect:/clientes";
        } catch (RegraNegocioException e) {
            model.addAttribute("erro", e.getMessage());
            return voltarAoFormulario(cliente, model);
        }
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes ra) {
        try {
            clienteService.excluir(id);
            ra.addFlashAttribute("sucesso", "Cliente excluído.");
        } catch (RegraNegocioException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/zerar-faltas")
    public String zerarFaltas(@PathVariable Long id, RedirectAttributes ra) {
        clienteService.zerarFaltas(id);
        ra.addFlashAttribute("sucesso", "Faltas zeradas. O cliente já pode fazer novos agendamentos.");
        return "redirect:/clientes/" + id + "/editar";
    }

    private String voltarAoFormulario(ClienteDTO cliente, Model model) {
        if (cliente.getId() != null) {
            model.addAttribute("historico", agendamentoService.listarPorCliente(cliente.getId()));
        }
        return "clientes/form";
    }
}
