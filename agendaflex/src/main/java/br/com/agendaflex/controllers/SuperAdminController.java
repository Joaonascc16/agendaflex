package br.com.agendaflex.controllers;

import br.com.agendaflex.repository.EstabelecimentoRepository;
import br.com.agendaflex.repository.UsuarioRepository;
import br.com.agendaflex.security.UsuarioPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public String painel(@AuthenticationPrincipal UsuarioPrincipal usuario, Model model) {
        model.addAttribute("nomeAdministrador", usuario.getNome());
        model.addAttribute("totalEstabelecimentos", estabelecimentoRepository.count());
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        return "super-admin/painel";
    }
}
