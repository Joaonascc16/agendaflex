package br.com.agendaflex.config;

import br.com.agendaflex.entity.Estabelecimento;
import br.com.agendaflex.repository.EstabelecimentoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o estabelecimento legado e associa a ele os registros já existentes.
 *
 * A rotina é idempotente: só preenche relacionamentos nulos e pode ser executada
 * novamente sem duplicar o estabelecimento nem alterar vínculos já definidos.
 *
 * Esta é uma etapa de transição. O isolamento por usuário/estabelecimento será
 * aplicado nos serviços e na segurança antes de habilitar o cadastro público.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class TenantBootstrap implements CommandLineRunner {

    public static final String SLUG_ESTABELECIMENTO_LEGADO = "agendaflex-principal";
    private static final String NOME_ESTABELECIMENTO_LEGADO = "AgendaFlex Principal";

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        Estabelecimento estabelecimento = estabelecimentoRepository
                .findBySlug(SLUG_ESTABELECIMENTO_LEGADO)
                .orElseGet(() -> estabelecimentoRepository.saveAndFlush(
                        new Estabelecimento(NOME_ESTABELECIMENTO_LEGADO,
                                SLUG_ESTABELECIMENTO_LEGADO, null, null)));

        associarRegistrosLegados(estabelecimento);
    }

    private void associarRegistrosLegados(Estabelecimento estabelecimento) {
        associar("update Cliente e set e.estabelecimento = :estabelecimento where e.estabelecimento is null",
                estabelecimento);
        associar("update Profissional e set e.estabelecimento = :estabelecimento where e.estabelecimento is null",
                estabelecimento);
        associar("update Servico e set e.estabelecimento = :estabelecimento where e.estabelecimento is null",
                estabelecimento);
        associar("update HorarioTrabalho e set e.estabelecimento = :estabelecimento where e.estabelecimento is null",
                estabelecimento);
        associar("update Bloqueio e set e.estabelecimento = :estabelecimento where e.estabelecimento is null",
                estabelecimento);
        associar("update Agendamento e set e.estabelecimento = :estabelecimento where e.estabelecimento is null",
                estabelecimento);
    }

    private void associar(String jpql, Estabelecimento estabelecimento) {
        entityManager.createQuery(jpql)
                .setParameter("estabelecimento", estabelecimento)
                .executeUpdate();
    }
}
