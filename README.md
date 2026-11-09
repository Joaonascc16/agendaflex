# AgendaFlex

Sistema web de gerenciamento de agendamentos, desenvolvido com Java e Spring Boot. O projeto permite administrar clientes, profissionais, serviços e horários de atendimento, aplicando regras de negócio para garantir a consistência da agenda.

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Tecnologias utilizadas](#tecnologias-utilizadas)
- [Arquitetura](#arquitetura)
- [Funcionalidades](#funcionalidades)
- [Regras de negócio](#regras-de-negócio)
- [Testes automatizados](#testes-automatizados)
- [Pré-requisitos](#pré-requisitos)
- [Como executar](#como-executar)
- [Acesso à aplicação](#acesso-à-aplicação)

## Sobre o projeto

O AgendaFlex é uma aplicação web desenvolvida para centralizar o gerenciamento de agendamentos e organizar a disponibilidade dos profissionais.

O sistema busca reduzir conflitos de horários, controlar a disponibilidade dos profissionais e manter os dados de clientes, serviços e atendimentos organizados.

A aplicação utiliza o padrão MVC com separação de responsabilidades entre as camadas de apresentação, controle, serviços e persistência.

## Tecnologias utilizadas

| Tecnologia | Aplicação |
|---|---|
| Java 17 | Linguagem principal |
| Spring Boot 3.5.9 | Framework da aplicação |
| Spring MVC | Controle das requisições web |
| Spring Data JPA | Persistência e acesso a dados |
| Hibernate | Mapeamento objeto-relacional |
| Thymeleaf | Renderização das páginas HTML |
| HTML5 | Estrutura das páginas |
| CSS3 | Estilização da interface |
| MySQL / H2 | Configuração de persistência |
| Maven | Gerenciamento de dependências |
| JUnit 5 | Testes automatizados |
| Mockito | Simulação de dependências nos testes |
| Git e GitHub | Versionamento do código |

## Arquitetura

O projeto utiliza uma arquitetura em camadas, buscando separar as responsabilidades e facilitar a manutenção.

### Organização dos pacotes

```text
src/
├── main/
│   ├── java/
│   │   └── .../
│   │       ├── controllers/
│   │       ├── dtos/
│   │       ├── entity/
│   │       ├── repository/
│   │       └── service/
│   │
│   └── resources/
│       ├── static/
│       │   └── css/
│       ├── templates/
│       └── application.properties
│
└── test/
    └── java/
        └── .../
```

### Responsabilidades das camadas

**Controllers**
- Recebem as requisições HTTP.
- Processam as interações com as páginas.
- Encaminham operações para os serviços.
- Retornam páginas e respostas apropriadas.

**DTOs (Data Transfer Objects)**
- Transportam dados entre as camadas.
- Organizam os dados recebidos pelos formulários.
- Evitam expor diretamente as entidades em operações que utilizam objetos de transferência.

**Entities**
- Representam as entidades persistidas no banco de dados.
- Definem os atributos e os mapeamentos relacionais.

**Repositories**
- Realizam operações de acesso e persistência de dados.
- Utilizam os recursos do Spring Data JPA.

**Services**
- Concentram as regras de negócio.
- Validam operações antes da persistência.
- Coordenam as operações entre diferentes repositórios.

### Fluxo de processamento

```text
Usuário
   |
   v
Controller
   |
   v
Service
   |
   v
Repository
   |
   v
Banco de dados
```

O resultado retorna pelas camadas até ser apresentado ao usuário pela interface Thymeleaf.

## Funcionalidades

### Gerenciamento de clientes
- Cadastro de clientes.
- Consulta de clientes.
- Edição de informações cadastrais.

### Gerenciamento de profissionais
- Cadastro e manutenção de profissionais.
- Consulta dos profissionais disponíveis.
- Gerenciamento de horários de trabalho.

### Gerenciamento de serviços
- Cadastro e manutenção de serviços.
- Definição de valores e duração.
- Consulta dos serviços cadastrados.

### Gerenciamento de agendamentos
- Criação de novos agendamentos.
- Consulta de agendamentos.
- Reagendamento.
- Cancelamento com motivo.
- Visualização da agenda semanal.
- Controle de disponibilidade.

### Gerenciamento de disponibilidade
- Configuração dos horários de trabalho.
- Bloqueio de horários.
- Verificação de disponibilidade dos profissionais.

## Regras de negócio

O sistema possui validações para preservar a integridade dos agendamentos.

Entre as principais regras estão:

1. **Disponibilidade profissional:** somente horários disponíveis devem ser apresentados para novos agendamentos.
2. **Conflito de horários:** um profissional não deve possuir dois agendamentos conflitantes.
3. **Sobreposição:** a validação considera a duração dos serviços, não apenas o horário inicial.
4. **Horário de trabalho:** os agendamentos devem respeitar os períodos de atendimento cadastrados.
5. **Bloqueios de agenda:** horários bloqueados não devem ser disponibilizados.
6. **Reagendamento:** a alteração de horário deve passar novamente pelas validações de disponibilidade.
7. **Integridade cadastral:** os dados obrigatórios devem ser validados antes de operações de cadastro e atualização.
8. **Cancelamento:** o cancelamento permite registrar o motivo da operação.

As validações são concentradas principalmente na camada de serviços, evitando depender exclusivamente da interface para proteger as regras do sistema.

## Testes automatizados

O projeto possui uma suíte de testes automatizados utilizando JUnit e Mockito.

Os testes verificam comportamentos das classes de serviço e regras de negócio, incluindo cenários válidos e inválidos.

**Última execução validada:**

| Indicador | Resultado |
|---|---:|
| Testes executados | 127 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 0 |
| Resultado | BUILD SUCCESS |

O projeto também possui integração contínua com GitHub Actions para executar os testes durante o fluxo de atualização do repositório.

> A aprovação dos testes representa os cenários cobertos pela suíte automatizada, não uma garantia absoluta de ausência de defeitos.

## Pré-requisitos

Para executar o projeto, é necessário ter:

- JDK 17 ou compatível com a configuração do projeto.
- Maven.
- IntelliJ IDEA ou outra IDE compatível com Java.
- Banco de dados configurado conforme o ambiente de execução.

## Como executar

### 1. Clonar o repositório

```bash
git clone https://github.com/Joaonascc16/agendaflex.git
```

### 2. Acessar o diretório do projeto

Entre na pasta que contém o arquivo `pom.xml`.

### 3. Configurar o banco de dados

Verifique as configurações em:

```text
src/main/resources/application.properties
```

Configure a conexão com o banco de dados de acordo com o ambiente utilizado. Não compartilhe credenciais reais no repositório.

### 4. Executar os testes

```bash
mvn test
```

### 5. Iniciar a aplicação

```bash
mvn spring-boot:run
```

Também é possível executar a classe principal da aplicação diretamente pela IDE.

## Acesso à aplicação

Após iniciar o Spring Boot, acesse:

```text
http://localhost:8080
```

## Integração contínua

O projeto utiliza GitHub Actions para automatizar a execução dos testes.

O workflow está localizado em:

```text
.github/workflows/testes.yml
```

Essa automação permite verificar se as alterações enviadas ao repositório continuam passando pela suíte de testes.

## Autor

**João Pedro do Nascimento Morais**

- GitHub: [@Joaonascc16](https://github.com/Joaonascc16)
- Repositório: [AgendaFlex](https://github.com/Joaonascc16/agendaflex)

---

Projeto desenvolvido com foco no aprendizado e aplicação prática de Java, Spring Boot, arquitetura em camadas, persistência de dados e testes automatizados.
