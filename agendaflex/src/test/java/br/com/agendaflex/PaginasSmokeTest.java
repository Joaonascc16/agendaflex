package br.com.agendaflex;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Valida o renderizador Thymeleaf sem testar autenticação (coberta em SecurityAuthenticationTest).
 * Sobe a aplicação inteira (com os dados de exemplo) e abre cada tela.
 * Se algum template Thymeleaf tiver erro de sintaxe ou de variável, o teste acusa aqui.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class PaginasSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    private void abre(String url, String trechoEsperado) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(trechoEsperado)));
    }

    @Test
    @DisplayName("Painel")
    void painel() throws Exception {
        abre("/", "Próximos atendimentos");
    }

    @Test
    @DisplayName("Agenda da semana")
    void agenda() throws Exception {
        abre("/agenda", "Agenda da semana");
    }

    @Test
    @DisplayName("Lista de agendamentos e filtros")
    void agendamentos() throws Exception {
        abre("/agendamentos", "Agendamentos");
        abre("/agendamentos?status=CONFIRMADO", "Agendamentos");
    }

    @Test
    @DisplayName("Novo agendamento, com e sem horários")
    void novoAgendamento() throws Exception {
        abre("/agendamentos/novo", "Novo agendamento");
        abre("/agendamentos/novo?servicoId=1&profissionalId=1&data=2030-01-07", "Horários livres");
    }

    @Test
    @DisplayName("Detalhe e reagendamento de um agendamento")
    void detalheEReagendamento() throws Exception {
        abre("/agendamentos/1", "Status");
        abre("/agendamentos/5/reagendar", "Reagendar atendimento");
    }

    @Test
    @DisplayName("Clientes")
    void clientes() throws Exception {
        abre("/clientes", "Clientes");
        abre("/clientes/novo", "Novo cliente");
        abre("/clientes/1/editar", "Histórico de agendamentos");
    }

    @Test
    @DisplayName("Serviços")
    void servicos() throws Exception {
        abre("/servicos", "Serviços");
        abre("/servicos/novo", "Novo serviço");
        abre("/servicos/1/editar", "Editar serviço");
    }

    @Test
    @DisplayName("Profissionais, expediente e bloqueios")
    void profissionais() throws Exception {
        abre("/profissionais", "Profissionais");
        abre("/profissionais/novo", "Novo profissional");
        abre("/profissionais/1/editar", "Editar profissional");
        abre("/profissionais/1/disponibilidade", "Expediente semanal");
    }

    @Test
    @DisplayName("Recurso inexistente mostra a página de erro com status 404")
    void recursoInexistente() throws Exception {
        mockMvc.perform(get("/clientes/9999/editar"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Não encontramos")));
    }
}
