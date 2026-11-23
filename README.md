# Gestão financeira — API

Projeto acadêmico em **Java 21, Quarkus 3.33 LTS e PostgreSQL 18**. Requisitos: Docker com Compose e JDK 21 para preparar o ambiente ou desenvolver pela IDE. O Maven Wrapper está incluído. Execute os comandos na raiz; no Windows use `./mvnw.cmd` no lugar de `./mvnw`.

## Configurar e executar

```sh
java scripts/PrepararAmbiente.java
```

O script gera o par RSA em `config/` e copia `.env.example` para `.env`, preenchendo senha do banco e segredo CSRF aleatórios. Não sobrescreve arquivos existentes. **Antes de iniciar a API**, edite `.env`: preencha `APP_BOOTSTRAP_EMAIL` e `APP_BOOTSTRAP_SENHA` (12 a 128 caracteres), opcionalmente `APP_BOOTSTRAP_NOME`. O startup cria esse usuário com hash PBKDF2-HMAC-SHA256, salt aleatório e 600.000 iterações; um e-mail existente não tem sua senha alterada. Depois da criação, remova as duas variáveis de bootstrap. Não existe cadastro público.

Para iniciar a API e o PostgreSQL juntos:

```sh
docker compose up --build -d --wait
```

O `docker-compose.yaml` compila a API em uma imagem Java 21, aguarda o PostgreSQL ficar saudável e inicia a aplicação em `http://localhost:8080`. O banco fica em `localhost:5433`. As chaves são montadas em modo somente leitura, e os segredos vêm do `.env`, fora da imagem. O Flyway cria as tabelas automaticamente. Esse Compose é para uso local com HTTP e `COOKIE_SECURE=false`; com HTTPS, defina `COOKIE_SECURE=true`.

Para acompanhar e desligar os serviços:

```sh
docker compose logs -f
docker compose down
```

O volume `postgres_data` mantém os dados ao desligar. `docker compose down -v` apaga o banco; use somente para reiniciar o ambiente do zero.

Para desenvolver na IDE com recarga automática:

```sh
./mvnw quarkus:dev
```

Nesse modo, o Quarkus usa `docker-compose.dev.yaml` para iniciar somente o PostgreSQL. O Compose principal também inclui esse arquivo, reutilizando a definição do banco. Ao encerrar normalmente o Quarkus, ele desliga os serviços que iniciou e preserva o volume. Não é necessário executar `docker compose up` antes. Use um modo por vez: pare a execução anterior para liberar as portas 8080 e 5433. Os modos usam volumes separados. A inclusão de arquivos exige [Docker Compose 2.20.3 ou superior](https://docs.docker.com/compose/how-tos/multiple-compose-files/include/).

Se a IDE encerrar o Java à força, desligue o banco pelo Docker Desktop ou pelo comando abaixo. A limpeza forçada do Quarkus fica desativada em desenvolvimento para proteger os dados:

```sh
docker compose -p gestao-financeira-dev -f docker-compose.dev.yaml down
```

Com PostgreSQL já instalado, crie um banco vazio `financeiro`, ajuste `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` no `.env` e defina `QUARKUS_COMPOSE_DEVSERVICES_ENABLED=false` para dispensar os contêineres. Nunca use um banco com dados pessoais para executar os testes.

API em `http://localhost:8080`. Para empacotar: `./mvnw package`, depois `java -jar target/quarkus-app/quarkus-run.jar`. O perfil de produção usa cookie `Secure`, portanto exige HTTPS no acesso do navegador. Configure a origem real em `FRONTEND_ORIGIN`, `CSRF_SECRET` (mínimo 32 caracteres) e os caminhos `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY` se usar chaves externas. `.env`, `config/`, `target/` e arquivos locais estão ignorados pelo Git.

## Autenticação e Angular

1. Faça `GET /api/auth/csrf` com credenciais: retorna `{"token":"..."}` e cookie `XSRF-TOKEN`.
2. Envie o `token` retornado no cabeçalho **`X-XSRF-TOKEN` em todo POST, PUT e DELETE**, inclusive login e logout. Preserve os cookies.
3. Faça login com `{"email":"seu-email@exemplo.com","senha":"sua-senha"}`. A resposta contém apenas `id`, `nome`, `email`; o JWT fica em `AUTH_TOKEN`, HttpOnly, SameSite=Lax, caminho `/api`, validade de uma hora.

No Angular use `provideHttpClient()` e `withCredentials: true`. Para chamadas absolutas a `http://localhost:8080/api`, envie explicitamente `X-XSRF-TOKEN` (o interceptor XSRF padrão não o adiciona em URLs absolutas); um interceptor próprio pode guardar o token retornado no passo 1 em memória. CORS aceita `http://localhost:4200` por padrão e inclui credenciais. Em desenvolvimento, use `localhost` nos dois lados. Em produção, mantenha front-end e API no mesmo site, idealmente com proxy `/api`; SameSite=Lax não atende domínios de sites diferentes.

O logout limpa o cookie. Como o JWT é stateless, uma cópia previamente obtida permanece válida até expirar; não há refresh token nem lista de revogação neste escopo. A integração do [front-end Angular](https://github.com/fernando-bastian/front-end-angular) usa URLs `/api` e um proxy de desenvolvimento para esta API. Consulte o README do front-end para iniciar as telas.

## Endpoints e JSON

Todos os endpoints abaixo, exceto CSRF/login, exigem autenticação. Criações retornam `201`, consultas/login `200`, logout `204`. Contas e categorias retornam listas de `{"id":1,"nome":"..."}`.

| Método | Caminho | Corpo/exemplo |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Sem corpo; obtém o token CSRF |
| POST | `/api/auth/login` | `{"email":"seu-email@exemplo.com","senha":"sua-senha"}` |
| POST | `/api/auth/logout` | Sem corpo |
| GET | `/api/auth/me` | Usuário autenticado |
| POST | `/api/contas` | `{"nome":"Carteira"}` |
| GET | `/api/contas` | Contas do usuário |
| GET | `/api/contas/{id}` | Detalhes de uma conta do usuário |
| PUT | `/api/contas/{id}` | `{"nome":"Reserva"}`; atualiza o nome |
| DELETE | `/api/contas/{id}` | Exclui conta sem lançamentos; retorna `204` |
| POST | `/api/categorias` | `{"nome":"Alimentação"}` |
| GET | `/api/categorias` | Categorias do usuário |
| POST | `/api/transacoes` | Exemplo abaixo |
| GET | `/api/transacoes?contaId=1&categoriaId=2` | Filtros opcionais, separados ou combinados |
| POST | `/api/transferencias` | Exemplo abaixo |
| GET | `/api/saldos` | Saldos por conta e geral |

Receita ou despesa (`tipo`: `RECEITA` ou `DESPESA`; descrição opcional):

```json
{"contaId":1,"categoriaId":2,"tipo":"DESPESA","valor":25.50,"data":"2026-11-16","descricao":"Almoço"}
```

Transferência (contas diferentes, ambas do usuário; descrição opcional):

```json
{"contaOrigemId":1,"contaDestinoId":2,"valor":10.00,"data":"2026-11-16","descricao":"Reserva"}
```

A resposta tem `id` UUID e os dados da transferência. A operação grava `TRANSFERENCIA_SAIDA` e `TRANSFERENCIA_ENTRADA`, com o mesmo `transferenciaId` e sem categoria, em uma única transação. Esses movimentos aparecem na consulta de transações.

Exemplo de resposta de `/api/saldos`:

```json
{"contas":[{"contaId":1,"nome":"Carteira","saldo":74.50},{"contaId":2,"nome":"Banco","saldo":10.00}],"saldoGeral":84.50}
```

Contas começam em zero. Para saldo inicial, lance uma receita. Todos os valores são em BRL, positivos, com até duas casas decimais, armazenados como `BigDecimal`/`NUMERIC(19,2)`; saldos negativos são permitidos. O saldo soma todos os lançamentos, inclusive os com data futura; a data é informativa, sem agendamento. Transferências preservam o saldo geral.

Editar uma conta preserva seus lançamentos e saldo. Excluir uma conta com lançamentos retorna `409`, mesmo quando o saldo é zero, para preservar o histórico financeiro. Contas de outro usuário não podem ser consultadas, alteradas ou excluídas.

Erros usam `{"mensagem":"Dados inválidos.","detalhes":["..."]}`: `400` para dados/JSON/CSRF inválidos, `401` para autenticação, `403` para acesso negado, `404` para conta/categoria inexistente ou de outro usuário. Campos desconhecidos, IDs fracionários e tipos de transação numéricos são rejeitados.

## Testes

```sh
./mvnw test
```

Com Docker ativo, o Quarkus inicia um PostgreSQL descartável. Alternativamente, aponte `TEST_DB_URL`, `TEST_DB_USERNAME` e `TEST_DB_PASSWORD` para **um banco PostgreSQL exclusivo de testes**; a suíte limpa suas tabelas. As chaves JWT dos testes são temporárias. A suíte cobre login/logout, cookies, JWT inválido/expirado, CSRF, CORS, isolamento, validação, filtros, saldos exatos e rollback de transferência quando o segundo lançamento falha.
