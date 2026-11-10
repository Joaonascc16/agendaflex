package br.com.agendaflex.security;

import br.com.agendaflex.config.TenantBootstrap;
import br.com.agendaflex.entity.Estabelecimento;
import br.com.agendaflex.entity.Usuario;
import br.com.agendaflex.entity.VinculoUsuarioEstabelecimento;
import br.com.agendaflex.enums.PapelEstabelecimento;
import br.com.agendaflex.repository.EstabelecimentoRepository;
import br.com.agendaflex.repository.UsuarioRepository;
import br.com.agendaflex.repository.VinculoUsuarioEstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro SUPER_ADMIN somente quando as três variáveis de ambiente
 * de bootstrap forem informadas. Nenhuma senha padrão é criada ou versionada.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class AdminInicialBootstrap implements ApplicationRunner {

    @Value("${agendaflex.seguranca.admin-inicial.email:}")
    private String emailConfigurado;

    @Value("${agendaflex.seguranca.admin-inicial.senha:}")
    private String senhaConfigurada;

    @Value("${agendaflex.seguranca.admin-inicial.nome:Administrador AgendaFlex}")
    private String nomeConfigurado;

    private final UsuarioRepository usuarioRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final VinculoUsuarioEstabelecimentoRepository vinculoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean emailInformado = emailConfigurado != null && !emailConfigurado.isBlank();
        boolean senhaInformada = senhaConfigurada != null && !senhaConfigurada.isBlank();

        if (!emailInformado && !senhaInformada) {
            return;
        }
        if (!emailInformado || !senhaInformada) {
            throw new IllegalStateException(
                    "Configure AGENDAFLEX_ADMIN_EMAIL e AGENDAFLEX_ADMIN_PASSWORD juntos.");
        }
        if (senhaConfigurada.length() < 12) {
            throw new IllegalStateException(
                    "A senha inicial do SUPER_ADMIN deve ter pelo menos 12 caracteres.");
        }

        String email = emailConfigurado.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);

        if (usuario == null) {
            usuario = new Usuario();
            usuario.setNome(nomeConfigurado == null || nomeConfigurado.isBlank()
                    ? "Administrador AgendaFlex" : nomeConfigurado.trim());
            usuario.setEmail(email);
            usuario.setSenhaHash(passwordEncoder.encode(senhaConfigurada));
            usuario.setAtivo(true);
            usuario.setEmailVerificado(true);
            usuario.setAdministradorGeral(true);
            usuario = usuarioRepository.save(usuario);
        } else if (!usuario.isAdministradorGeral() || !usuario.isAtivo()) {
            throw new IllegalStateException(
                    "O e-mail configurado já pertence a uma conta que não é SUPER_ADMIN ativo.");
        }

        Estabelecimento estabelecimento = estabelecimentoRepository
                .findBySlug(TenantBootstrap.SLUG_ESTABELECIMENTO_LEGADO)
                .orElseThrow(() -> new IllegalStateException(
                        "Estabelecimento principal não foi inicializado."));

        VinculoUsuarioEstabelecimento vinculo = vinculoRepository
                .findByUsuarioIdAndEstabelecimentoId(usuario.getId(), estabelecimento.getId())
                .orElse(null);
        if (vinculo == null) {
            vinculo = new VinculoUsuarioEstabelecimento(
                    usuario, estabelecimento, PapelEstabelecimento.ADMIN);
        }
        vinculo.setPapel(PapelEstabelecimento.ADMIN);
        vinculo.setAtivo(true);
        vinculoRepository.save(vinculo);
    }
}
