# account-service

Microsserviço de contas (Java 25 / Spring Boot 4.1). Dono do schema `accounts` no PostgreSQL. Serviço interno: só o `gateway-service` fica exposto; o `auth-service` usa `POST /accounts/login` para validar credenciais.

## Estado: boilerplate

| Pronto | Seu (`TODO(account)`) |
|---|---|
| Rotas HTTP, status, `Location`, validação dos DTOs | Regras de negócio em [`AccountService`](src/main/java/store/account/AccountService.java) |
| Tabela + migração Flyway, entidade JPA, repositório | Política de senha, e-mail duplicado (409), login sem revelar e-mails (401) |
| BCrypt configurado (`PasswordEncoder`) | Decidir se `GET /accounts` (lista todas) deve existir |
| Actuator, Prometheus, OpenAPI, Docker, Compose, k8s, CI/CD | |

Enquanto não implementados, os métodos respondem **501 Not Implemented**. Os testes em [`AccountContractTest`](src/test/java/store/account/AccountContractTest.java) descrevem o comportamento esperado e estão com `@Disabled`: remova a anotação de cada um conforme implementar.

## Contrato HTTP

| Método e caminho | Resultado |
|---|---|
| `POST /accounts` | 201 + `Location`; corpo `{"name","email","password"}` → `{"id","name","email"}`; inválido 400; e-mail repetido 409 |
| `GET /accounts` | 200; lista de contas |
| `GET /accounts/{id}` | 200; inexistente 404 |
| `DELETE /accounts/{id}` | 204 |
| `POST /accounts/login` | 200 com a conta; credenciais erradas 401. **Interno** (o gateway devolve 404) |
| `GET /accounts/health-check` | 200 |

Swagger em `/swagger-ui/index.html`. Saúde e métricas na porta privada 9091 (`/actuator/health`, `/actuator/prometheus`).

## Rodar

Testes (H2 em memória, sem Postgres). Sem Maven/JDK 25 local, use o Docker:

```bash
mvn -B -ntp verify
# ou
docker run --rm -v "$PWD":/w -w /w maven:3.9-eclipse-temurin-25 mvn -B -ntp verify
```

Ambiente local com Postgres próprio:

```bash
python3 scripts/init-env.py          # cria .env com senha aleatória (uma vez)
docker network create store-dev      # uma vez; rede compartilhada entre os serviços
docker compose up --build -d --wait
curl -i localhost:8083/accounts/health-check
```

## CI/CD (GitHub Actions)

[`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml):

- **verify** (todo push/PR): `mvn verify`, build da imagem, validação do Compose.
- **deploy** (push em `main`): build `linux/amd64` → push no ECR `account-service` → `kubectl apply` no EKS pelo **digest** → espera o rollout. Só roda quando a variável da organização `AWS_ROLE_ARN` existe; a autenticação é por OIDC (sem chave AWS no GitHub).

Variáveis da organização usadas: `AWS_ROLE_ARN`, `AWS_REGION` (`us-east-2`), `EKS_CLUSTER_NAME` (`eks-store`), `K8S_NAMESPACE` (`store`).

### Preparação única no cluster

O pipeline não cria nem sobrescreve configuração ou segredos:

```bash
kubectl -n store apply -f k8s/deployer-rbac.yaml
cp k8s/config.example.yaml k8s/config.local.yaml   # preencha o endpoint do RDS
kubectl -n store apply -f k8s/config.local.yaml
read -rsp 'Senha do RDS: ' DB_PASSWORD; echo
# printf é builtin do shell: a senha não aparece na lista de processos
printf 'DATABASE_USERNAME=store\nDATABASE_PASSWORD=%s\n' "$DB_PASSWORD" |
  kubectl -n store create secret generic account-secrets --from-env-file=/dev/stdin
unset DB_PASSWORD
```

O banco `store` precisa existir; o Flyway cria o schema e a tabela.

## Uso de IA

O boilerplate deste repositório (estrutura, configuração, camada HTTP, persistência, testes de contrato desabilitados, Docker/k8s/CI) foi gerado com assistência de IA (Claude) e revisado pela equipe. As regras de negócio marcadas `TODO(account)` são implementadas pela equipe. Registre aqui o que cada integrante fez.
