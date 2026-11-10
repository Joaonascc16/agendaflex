package br.com.agendaflex.security;

import br.com.agendaflex.entity.VinculoUsuarioEstabelecimento;
import br.com.agendaflex.enums.StatusEstabelecimento;
import br.com.agendaflex.repository.VinculoUsuarioEstabelecimentoRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EstabelecimentoAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final VinculoUsuarioEstabelecimentoRepository vinculoRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        UsuarioPrincipal usuario = (UsuarioPrincipal) authentication.getPrincipal();
        var session = request.getSession(true);
        session.removeAttribute(TenantSessao.ESTABELECIMENTO_ID);

        if (usuario.isAdministradorGeral()) {
            response.sendRedirect("/super-admin");
            return;
        }

        List<VinculoUsuarioEstabelecimento> vinculos = vinculoRepository
                .findByUsuarioIdAndAtivoTrue(usuario.getId()).stream()
                .filter(v -> v.getEstabelecimento().getStatus() == StatusEstabelecimento.ATIVO)
                .toList();

        if (vinculos.isEmpty()) {
            response.sendRedirect("/sem-estabelecimento");
            return;
        }

        if (vinculos.size() == 1) {
            session.setAttribute(TenantSessao.ESTABELECIMENTO_ID,
                    vinculos.get(0).getEstabelecimento().getId());
            response.sendRedirect("/");
            return;
        }

        response.sendRedirect("/selecionar-estabelecimento");
    }
}
