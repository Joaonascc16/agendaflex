package br.com.agendaflex.service;

import br.com.agendaflex.entity.Estabelecimento;
import br.com.agendaflex.repository.EstabelecimentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolve o estabelecimento ativo da execução atual.
 *
 * A seleção dinâmica será preenchida pelo filtro de autenticação na próxima etapa.
 * Enquanto a aplicação ainda não possui login, utiliza apenas o tenant legado
 * configurado, preservando o funcionamento local sem abrir seleção por parâmetro HTTP.
 */
@Component
@RequiredArgsConstructor
public class TenantContext {

    private final EstabelecimentoRepository estabelecimentoRepository;

    @Value("${agendaflex.tenant.estabelecimento-padrao-slug:agendaflex-principal}")
    private String slugPadrao;

    private final ThreadLocal<Long> estabelecimentoIdAtual = new ThreadLocal<>();

    public Long obterEstabelecimentoId() {
        Long atual = estabelecimentoIdAtual.get();
        if (atual != null) {
            return atual;
        }

        return estabelecimentoRepository.findBySlug(slugPadrao)
                .map(Estabelecimento::getId)
                .orElseThrow(() -> new IllegalStateException(
                        "Estabelecimento padrão não encontrado: " + slugPadrao));
    }

    public Estabelecimento obterEstabelecimento() {
        return estabelecimentoRepository.findById(obterEstabelecimentoId())
                .orElseThrow(() -> new IllegalStateException("Estabelecimento ativo não encontrado."));
    }

    /**
     * Uso exclusivo pela integração de autenticação/seleção de tenant.
     * Não expor este método em controllers nem aceitar o ID diretamente do navegador.
     */
    public void definirEstabelecimento(Long estabelecimentoId) {
        if (estabelecimentoId == null || estabelecimentoId <= 0) {
            throw new IllegalArgumentException("ID de estabelecimento inválido.");
        }
        estabelecimentoIdAtual.set(estabelecimentoId);
    }

    public void limpar() {
        estabelecimentoIdAtual.remove();
    }
}
