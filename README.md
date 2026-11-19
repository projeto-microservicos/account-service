# account-service

Microsserviço de contas (Java 25 / Spring Boot 4.1). Dono do schema `accounts` no PostgreSQL. Serviço interno: só o `gateway-service` fica exposto; o `auth-service` usa `POST /accounts/login` para validar credenciais.

## Estado: implementado (17/11/2026)

- **E-mail** normalizado (trim e minúsculas): o `UNIQUE` da tabela barra duplicatas em qualquer caixa, inclusive em cadastros simultâneos (409).
- **Senha** com no mínimo 8 caracteres e no máximo 72 bytes UTF-8, o limite do BCrypt (400). Só o hash BCrypt é guardado.
- **Login:** o mesmo 401 para e-mail desconhecido e senha errada, com tempo parecido nos dois casos (há um BCrypt de mentira), para não revelar quais e-mails existem.
- **Dono da conta:** pelo gateway, o `id-account` sempre chega, e a pessoa só lê e apaga a própria conta; a de outra pessoa dá 404, como no order. `GET /accounts` devolve só a própria conta. Sem o header, só chamadas de dentro do cluster (o auth) veem qualquer conta.

[`AccountContractTest`](src/test/java/store/account/AccountContractTest.java): 8 testes, mais 5 no `AccountSmokeTest`.

## Contrato HTTP

| Método e caminho | Resultado |
|---|---|
| `POST /accounts` | 201 + `Location`; corpo `{"name","email","password"}` → `{"id","name","email"}`; inválido ou senha fora da política 400; e-mail repetido 409 |
| `GET /accounts` | 200; pelo gateway, só a própria conta; dentro do cluster, todas |
| `GET /accounts/{id}` | 200; inexistente ou de outra pessoa 404 |
| `DELETE /accounts/{id}` | 204; inexistente ou de outra pessoa 404 |
| `POST /accounts/login` | 200 com a conta; credenciais erradas 401. **Interno** (o gateway devolve 404) |
| `GET /accounts/health-check` | 200 |

Swagger em `/swagger-ui/index.html`. As probes de startup e liveness do Kubernetes chamam `GET /accounts/health-check`, como pede o `Overview.md`. A readiness e as métricas ficam na porta privada 9091 (`/actuator/health/readiness`, `/actuator/prometheus`).

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
- **deploy** (push em `main`): build `linux/amd64` → push no ECR `account-service` → `kubectl apply` no EKS pelo **digest** → espera o rollout. Só roda quando a variável de repositório `AWS_ROLE_ARN` existe; a autenticação é por OIDC (sem chave AWS no GitHub).

Variáveis do repositório (Settings → Secrets and variables → Actions → Variables): `AWS_ROLE_ARN`, `AWS_REGION` (`us-east-2`), `EKS_CLUSTER_NAME` (`eks-store`), `K8S_NAMESPACE` (`store`).

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

O boilerplate deste repositório (estrutura, configuração, camada HTTP, persistência, testes de contrato desabilitados, Docker/k8s/CI) foi gerado com assistência de IA (Claude) e revisado pela equipe. As regras de negócio (`AccountService`), a regra de dono da conta e os testes correspondentes foram implementados por decisão do João (D-01, 17/11/2026), escritas pelo agente do João (Claude) e revisadas pelo João antes do commit.
