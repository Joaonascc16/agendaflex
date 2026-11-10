package br.com.agendaflex.controllers;

import br.com.agendaflex.entity.VinculoUsuarioEstabelecimento;
import br.com.agendaflex.enums.StatusEstabelecimento;
import br.com.agendaflex.repository.VinculoUsuarioEstabelecimentoRepository;
import br.com.agendaflex.security.TenantSessao;
import br.com.agendaflex.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SelecaoEstabelecimentoController {

    private final VinculoUsuarioEstabelecimentoRepository vinculoRepository;

    @GetMapping("/selecionar-estabelecimento")
    public String listar(@AuthenticationPrincipal UsuarioPrincipal usuario, Model model) {
        List<VinculoUsuarioEstabelecimento> vinculos = vinculoRepository
                .findByUsuarioIdAndAtivoTrue(usuario.getId()).stream()
                .filter(v -> v.getEstabelecimento().getStatus() == StatusEstabelecimento.ATIVO)
                .toList();

        if (vinculos.isEmpty()) {
            return "redirect:/sem-estabelecimento";
        }

        model.addAttribute("vinculos", vinculos);
        return "auth/selecionar-estabelecimento";
    }

    @PostMapping("/selecionar-estabelecimento")
    public String selecionar(@AuthenticationPrincipal UsuarioPrincipal usuario,
                              @RequestParam Long estabelecimentoId,
                              HttpServletRequest request,
                              RedirectAttributes redirectAttributes) {
        VinculoUsuarioEstabelecimento vinculo = vinculoRepository
                .findByUsuarioIdAndEstabelecimentoId(usuario.getId(), estabelecimentoId)
                .filter(VinculoUsuarioEstabelecimento::isAtivo)
                .filter(v -> v.getEstabelecimento().getStatus() == StatusEstabelecimento.ATIVO)
                .orElse(null);

        if (vinculo == null) {
            redirectAttributes.addFlashAttribute("erro",
                    "Você não possui acesso ativo a esse estabelecimento.");
            return "redirect:/selecionar-estabelecimento";
        }

        request.getSession(true).setAttribute(TenantSessao.ESTABELECIMENTO_ID,
                vinculo.getEstabelecimento().getId());
        return "redirect:/";
    }
}
