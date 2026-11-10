package br.com.agendaflex.controllers;

import br.com.agendaflex.security.TenantSessao;
import br.com.agendaflex.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UsuarioPrincipal usuario)) {
            return "auth/login";
        }

        if (usuario.isAdministradorGeral()) {
            return "redirect:/super-admin";
        }

        var session = request.getSession(false);
        if (session != null && session.getAttribute(TenantSessao.ESTABELECIMENTO_ID) != null) {
            return "redirect:/";
        }
        return "redirect:/selecionar-estabelecimento";
    }

    @GetMapping("/sem-estabelecimento")
    public String semEstabelecimento() {
        return "auth/sem-estabelecimento";
    }

    @GetMapping("/sem-acesso")
    public String semAcesso() {
        return "auth/sem-acesso";
    }
}
