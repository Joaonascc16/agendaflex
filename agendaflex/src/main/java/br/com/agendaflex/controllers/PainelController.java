package br.com.agendaflex.controllers;

import br.com.agendaflex.dtos.ProfissionalDTO;
import br.com.agendaflex.service.PainelService;
import br.com.agendaflex.service.ProfissionalService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PainelController {

    private final PainelService painelService;
    private final ProfissionalService profissionalService;

    @GetMapping("/")
    public String painel(Model model) {
        model.addAttribute("painel", painelService.resumo());
        return "painel";
    }

    @GetMapping("/agenda")
    public String agenda(@RequestParam(required = false) Long profissionalId,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                         Model model) {
        List<ProfissionalDTO> profissionais = profissionalService.listarAtivos();
        model.addAttribute("profissionais", profissionais);

        if (!profissionais.isEmpty()) {
            Long escolhido = profissionalId != null ? profissionalId : profissionais.get(0).getId();
            model.addAttribute("profissionalId", escolhido);
            model.addAttribute("semana", painelService.montarSemana(escolhido, data));
        }
        return "agenda";
    }
}
