# AgendaFlex

Sistema de agendamento feito com **Spring Boot 3.5**, **Thymeleaf**, **Spring Data JPA** e testes com **JUnit 5**.
O foco está na interface e nas **regras de negócio**: quem pode agendar, quando, com quem e o que acontece depois.

## Como rodar

Requisitos: **JDK 17 ou superior** e **Maven** (ou a sua IDE).

```bash
mvn spring-boot:run
```

Abra http://localhost:8080. Na primeira execução o sistema carrega dados de exemplo
(profissionais, serviços, clientes e alguns agendamentos). Para desligar, use `agendaflex.dados-demo=false`.

Por padrão o banco é o **H2 em memória** (não precisa instalar nada; console em `/h2`).
Para usar **MySQL**, ajuste usuário e senha em `application-mysql.properties` e rode:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

## Testes

```bash
mvn test
```

| Camada | Classes | O que cobre |
|---|---|---|
| Entidades | `ClienteTest`, `ServicoTest`, `ProfissionalTest`, `HorarioTrabalhoTest`, `BloqueioTest`, `AgendamentoTest`, `StatusAgendamentoTest` | Regras de domínio de cada entidade, sem Spring e sem banco |
| Serviço | `AgendamentoServiceTest` | Todas as regras de agendamento com repositórios simulados (Mockito) e relógio fixo |
| Repository | `AgendamentoRepositoryTest` | Consultas de conflito e indicadores no banco (`@DataJpaTest`) |
| Telas | `PaginasSmokeTest` | Abre cada página com dados reais para detectar erro em template |

## Estrutura

```
br.com.agendaflex
├── entity        Cliente, Servico, Profissional, HorarioTrabalho, Bloqueio, Agendamento
├── dtos          objetos de formulário e de exibição
├── repository    Spring Data JPA, com as consultas de conflito
├── service       regras de negócio (AgendamentoService é o centro)
├── controllers   telas Thymeleaf
├── enums         StatusAgendamento (com a máquina de estados)
├── exception     RegraNegocioException, RecursoNaoEncontradoException
├── config        regras configuráveis, relógio, dados de exemplo
└── util          pequenos utilitários de texto e tempo
```

## Regras de negócio

**Ao criar um agendamento**

1. Cliente ativo e com menos faltas que o limite (padrão: 3).
2. Serviço e profissional ativos, e o profissional realiza aquele serviço.
3. Horário no futuro, com antecedência mínima (2 h) e máxima (60 dias).
4. Horário na grade da agenda (de 30 em 30 minutos).
5. Atendimento inteiro dentro de uma janela de trabalho do profissional, no mesmo dia.
6. Sem bloqueio do profissional (férias, folgas) e sem conflito com outro agendamento dele.
7. Sem conflito com outro agendamento do próprio cliente.
8. Limite de agendamentos ativos por cliente (3) e por dia (2).

**Depois de criado**

- Ciclo de vida: `Agendado → Confirmado → Concluído`, com `Cancelado` e `Faltou` como saídas.
- Cancelar exige **motivo** e antecedência mínima (2 h).
- Reagendar respeita as mesmas regras de horário, tem limite (2 vezes) e volta para `Agendado` até nova confirmação.
- Concluir ou registrar falta só é possível depois do horário de início.
- Registrar falta soma uma falta ao cliente; ao atingir o limite ele fica **bloqueado** até alguém zerar as faltas.
- O preço fica gravado no agendamento: reajustar o serviço não altera o histórico.

**Cadastros**

- Cliente, profissional e serviço com histórico de agendamentos não são excluídos, apenas desativados.
- Não é possível desativar um profissional com agendamentos futuros.
- Não é possível bloquear uma agenda que já tenha atendimentos ativos no período.
- Janelas de trabalho do mesmo dia não podem se sobrepor (para o almoço, cadastre duas janelas).

Todos os limites ficam em `application.properties` (prefixo `agendaflex.regras`), então dá para mudar o comportamento sem alterar código.

## Telas

- **Painel**: atendimentos de hoje, pendentes de confirmação, próximos 7 dias, faltas e receita do mês.
- **Agenda da semana**: grade por profissional com atendimentos e bloqueios; clique para abrir.
- **Novo agendamento**: serviço, profissional e data mostram apenas os horários realmente livres.
- **Agendamentos**: filtros por status e profissional; detalhe com as ações liberadas pelas regras.
- **Clientes, Serviços e Profissionais**: cadastros, histórico do cliente, expediente e bloqueios.

## Ideias para evoluir

- Login e perfis (recepção, profissional, administrador) com Spring Security.
- API REST com JWT e documentação Swagger sobre os mesmos serviços.
- Lista de espera quando não há horários livres.
- Agendamentos recorrentes (toda terça às 14h).
- Envio de lembretes por e-mail.
