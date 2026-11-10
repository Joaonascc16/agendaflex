package br.com.agendaflex.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.agendaflex.entity.Cliente;
import br.com.agendaflex.entity.Estabelecimento;
import br.com.agendaflex.entity.Servico;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class TenantIsolationRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ServicoRepository servicoRepository;

    @Test
    void consultasPorEstabelecimentoNaoRetornamDadosDeOutroTenant() {
        Estabelecimento unidadeA = em.persist(new Estabelecimento("Unidade A", "unidade-a", null, null));
        Estabelecimento unidadeB = em.persist(new Estabelecimento("Unidade B", "unidade-b", null, null));

        Cliente clienteA = new Cliente("Ana", "ana.a@example.com", null);
        clienteA.setEstabelecimento(unidadeA);
        em.persist(clienteA);

        Cliente clienteB = new Cliente("Bruna", "bruna.b@example.com", null);
        clienteB.setEstabelecimento(unidadeB);
        em.persist(clienteB);

        Servico servicoA = new Servico("Corte", null, 30, new BigDecimal("50.00"));
        servicoA.setEstabelecimento(unidadeA);
        em.persist(servicoA);

        Servico servicoB = new Servico("Barba", null, 30, new BigDecimal("60.00"));
        servicoB.setEstabelecimento(unidadeB);
        em.persist(servicoB);

        em.flush();
        em.clear();

        assertThat(clienteRepository.findAllByEstabelecimentoIdOrderByNomeAsc(unidadeA.getId()))
                .extracting(Cliente::getEmail)
                .containsExactly("ana.a@example.com");
        assertThat(clienteRepository.findByIdAndEstabelecimentoId(clienteB.getId(), unidadeA.getId()))
                .isEmpty();

        assertThat(servicoRepository.findAllByEstabelecimentoIdOrderByNomeAsc(unidadeA.getId()))
                .hasSize(1)
                .allMatch(servico -> servico.getPreco().compareTo(new BigDecimal("50.00")) == 0);
        assertThat(servicoRepository.findByIdAndEstabelecimentoId(servicoB.getId(), unidadeA.getId()))
                .isEmpty();
    }
}
