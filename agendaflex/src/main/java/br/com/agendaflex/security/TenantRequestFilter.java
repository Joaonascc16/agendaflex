package br.com.agendaflex.security;

import br.com.agendaflex.entity.VinculoUsuarioEstabelecimento;
import br.com.agendaflex.enums.StatusEstabelecimento;
import br.com.agendaflex.repository.VinculoUsuarioEstabelecimentoRepository;
import br.com.agendaflex.service.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class TenantRequestFilter extends OncePerRequestFilter {

    private final TenantContext tenantContext;
    private final VinculoUsuarioEstabelecimentoRepository vinculoRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof UsuarioPrincipal usuario) {
                var authorities = new ArrayList<GrantedAuthority>();
                if (usuario.isAdministradorGeral()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                } else {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }

                vinculoSelecionado(request, usuario.getId()).ifPresent(vinculo -> {
                    tenantContext.definirEstabelecimento(vinculo.getEstabelecimento().getId());
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + vinculo.getPapel().name()));
                });

                UsernamePasswordAuthenticationToken autenticacaoDoTenant =
                        UsernamePasswordAuthenticationToken.authenticated(
                                usuario, authentication.getCredentials(), authorities);
                autenticacaoDoTenant.setDetails(authentication.getDetails());
                SecurityContextHolder.getContext().setAuthentication(autenticacaoDoTenant);
            }

            filterChain.doFilter(request, response);
        } finally {
            tenantContext.limpar();
        }
    }

    private Optional<VinculoUsuarioEstabelecimento> vinculoSelecionado(
            HttpServletRequest request, Long usuarioId) {
        var session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object valor = session.getAttribute(TenantSessao.ESTABELECIMENTO_ID);
        if (!(valor instanceof Number numero)) {
            return Optional.empty();
        }

        Long estabelecimentoId = numero.longValue();
        Optional<VinculoUsuarioEstabelecimento> vinculo =
                vinculoRepository.findByUsuarioIdAndEstabelecimentoId(usuarioId, estabelecimentoId);

        boolean permitido = vinculo.filter(VinculoUsuarioEstabelecimento::isAtivo)
                .map(VinculoUsuarioEstabelecimento::getEstabelecimento)
                .map(estabelecimento -> estabelecimento.getStatus() == StatusEstabelecimento.ATIVO)
                .orElse(false);

        if (!permitido) {
            session.removeAttribute(TenantSessao.ESTABELECIMENTO_ID);
            return Optional.empty();
        }

        return vinculo;
    }
}
