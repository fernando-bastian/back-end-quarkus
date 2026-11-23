# Gestão financeira — API

Projeto acadêmico em **Java 17, Quarkus 3.33 LTS e PostgreSQL 18** para gerenciar contas, categorias, receitas, despesas, transferências e saldos por usuário.

## Executar

Requisitos: JDK 17 e Docker com Compose 2.20.3 ou superior. Execute na raiz:

```sh
java scripts/PrepararAmbiente.java
```

O script gera as chaves JWT e o `.env`, sem sobrescrever arquivos existentes. Preencha `APP_BOOTSTRAP_EMAIL` e `APP_BOOTSTRAP_SENHA` (12 a 128 caracteres) no `.env` para criar o usuário inicial. Não há cadastro público. Remova essas variáveis após a criação.

```sh
docker compose up --build -d --wait
```

API: `http://localhost:8080`. PostgreSQL: `localhost:5433`. As tabelas são criadas automaticamente pelo Flyway.

- Acompanhar logs: `docker compose logs -f`.
- Encerrar: `docker compose down` (preserva os dados; `down -v` apaga o banco).
- Desenvolvimento com recarga automática: `./mvnw quarkus:dev` (inicia o banco pelo Docker; encerre o Compose anterior primeiro). Os modos usam volumes separados.
- No PowerShell, use `.\mvnw.cmd` no lugar de `./mvnw`.

## Autenticação e integração

1. Obtenha o token com `GET /api/auth/csrf`.
2. Faça `POST /api/auth/login` com `{"email":"seu-email@exemplo.com","senha":"sua-senha"}`.
3. Preserve os cookies e envie o token retornado no cabeçalho `X-XSRF-TOKEN` em todo `POST`, `PUT` e `DELETE`, inclusive login e logout.

O JWT fica em cookie HttpOnly e expira em uma hora. No Angular, use `withCredentials: true` e envie o cabeçalho CSRF explicitamente em URLs absolutas. A origem padrão é `http://localhost:4200`.

[Front-end Angular](https://github.com/fernando-bastian/front-end-angular). Em produção, configure `FRONTEND_ORIGIN`, use HTTPS com `COOKIE_SECURE=true` e mantenha front-end e API no mesmo site. Não versione `.env` nem as chaves de `config/`.

## Endpoints

Todos exigem autenticação, exceto CSRF e login.

| Método | Caminho | Função |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Obter token CSRF |
| POST | `/api/auth/login`, `/api/auth/logout` | Entrar ou sair |
| GET | `/api/auth/me` | Consultar usuário autenticado |
| GET, POST | `/api/contas`, `/api/categorias` | Listar ou criar |
| GET, PUT, DELETE | `/api/contas/{id}` | Consultar, renomear ou excluir conta sem lançamentos |
| GET, POST | `/api/transacoes` | Listar ou registrar receita/despesa; filtros opcionais `contaId` e `categoriaId` |
| POST | `/api/transferencias` | Transferir entre contas do usuário |
| GET | `/api/saldos` | Consultar saldos por conta e geral |

## Testes

```sh
./mvnw test
```

Com Docker ativo, os testes usam PostgreSQL descartável. Para banco externo, configure `TEST_DB_URL`, `TEST_DB_USERNAME` e `TEST_DB_PASSWORD` com um banco exclusivo de testes: a suíte limpa suas tabelas.
