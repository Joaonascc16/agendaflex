package br.com.agendaflex;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.agendaflex.entity.Estabelecimento;
import br.com.agendaflex.entity.Usuario;
import br.com.agendaflex.entity.VinculoUsuarioEstabelecimento;
import br.com.agendaflex.enums.PapelEstabelecimento;
import br.com.agendaflex.repository.EstabelecimentoRepository;
import br.com.agendaflex.repository.UsuarioRepository;
import br.com.agendaflex.repository.VinculoUsuarioEstabelecimentoRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "agendaflex.dados-demo=false")
@AutoConfigureMockMvc
class SecurityAuthenticationTest {

    private static final String SENHA = "SenhaSeguraAgendaFlex2026!";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired private VinculoUsuarioEstabelecimentoRepository vinculoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void redirecionaVisitanteParaLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void administradorEntraEVisualizaPainelDoSeuEstabelecimento() throws Exception {
        Estabelecimento estabelecimento = estabelecimentoPrincipal();
        Usuario usuario = criarUsuario();
        vincular(usuario, estabelecimento, PapelEstabelecimento.ADMIN);

        MockHttpSession session = entrar(usuario.getEmail(), SENHA, "/");

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Próximos atendimentos")));
    }

    @Test
    void clienteNaoPodeAcessarPainelAdministrativo() throws Exception {
        Estabelecimento estabelecimento = estabelecimentoPrincipal();
        Usuario usuario = criarUsuario();
        vincular(usuario, estabelecimento, PapelEstabelecimento.CLIENTE);

        MockHttpSession session = entrar(usuario.getEmail(), SENHA, "/");

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void papelEhValidadoNoEstabelecimentoSelecionado() throws Exception {
        Estabelecimento principal = estabelecimentoPrincipal();
        Estabelecimento outro = estabelecimentoRepository.saveAndFlush(
                new Estabelecimento("Unidade de teste " + UUID.randomUUID(),
                        "unidade-" + UUID.randomUUID(), null, null));

        Usuario usuario = criarUsuario();
        vincular(usuario, principal, PapelEstabelecimento.ADMIN);
        vincular(usuario, outro, PapelEstabelecimento.CLIENTE);

        MockHttpSession session = entrar(usuario.getEmail(), SENHA, "/selecionar-estabelecimento");

        mockMvc.perform(get("/selecionar-estabelecimento").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Onde você deseja entrar?")));

        mockMvc.perform(post("/selecionar-estabelecimento")
                        .session(session)
                        .with(csrf())
                        .param("estabelecimentoId", outro.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/selecionar-estabelecimento")
                        .session(session)
                        .with(csrf())
                        .param("estabelecimentoId", principal.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Próximos atendimentos")));
    }

    @Test
    void rejeitaSenhaIncorreta() throws Exception {
        Usuario usuario = criarUsuario();

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", usuario.getEmail())
                        .param("senha", "senha-incorreta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?erro"));
    }

    private MockHttpSession entrar(String email, String senha, String destino) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("senha", senha))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(destino))
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Usuario criarUsuario() {
        String identificador = UUID.randomUUID().toString().replace("-", "");
        Usuario usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail("teste-" + identificador + "@example.com");
        usuario.setSenhaHash(passwordEncoder.encode(SENHA));
        usuario.setAtivo(true);
        usuario.setEmailVerificado(true);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private Estabelecimento estabelecimentoPrincipal() {
        return estabelecimentoRepository.findBySlug("agendaflex-principal").orElseThrow();
    }

    private void vincular(Usuario usuario, Estabelecimento estabelecimento, PapelEstabelecimento papel) {
        vinculoRepository.saveAndFlush(
                new VinculoUsuarioEstabelecimento(usuario, estabelecimento, papel));
    }
}
