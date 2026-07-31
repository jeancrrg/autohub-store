# Integração Frontend ↔ Backend

**Status:** definido, aguardando implementação
**Gate:** nenhum microsserviço além de API Gateway, Auth Service, User Service e Catalog Service
deve ser criado antes desta integração estar concluída (ver `action-plan.md` Fase 2.5).

Este documento reúne todas as decisões necessárias pra sair do frontend 100% mock
(`lib/data/*.ts`, `authStore` fake) pra um frontend que consome os microsserviços reais via
API Gateway. Cada seção resolve um ponto que não estava definido nas specs originais.

---

## 1. Arquitetura de comunicação

```
Frontend (Next.js, :3000) → API Gateway (:8001) → {auth,user,catalog}-service
```

Frontend nunca chama microsserviço diretamente — sempre via Gateway. Gateway resolve rota por
prefixo de path (`/api/v1/auth/**`, `/api/v1/catalog/**`, etc.) e valida JWT antes de rotear.

---

## 2. Autenticação — cookie httpOnly

**Decisão:** tokens JWT nunca ficam acessíveis a JavaScript no browser.

- `POST /api/v1/auth/login` — Auth Service autentica e devolve resposta **sem token no body**;
  o Gateway repassa `Set-Cookie` do Auth Service pro cliente:
  - `access_token` — httpOnly, Secure, SameSite=Lax, maxAge=1h
  - `refresh_token` — httpOnly, Secure, SameSite=Lax, path=`/api/v1/auth/refresh`, maxAge=7d
- Frontend faz todo request com `withCredentials:true` (axios) — cookie vai automático, sem
  header `Authorization` manual.
- Gateway extrai JWT do cookie `access_token` (não de header) pra validar claims/roles.
- `POST /api/v1/auth/refresh` — chamado automaticamente pelo interceptor do axios quando um
  request retorna 401; reseta os dois cookies (rotation).
- `POST /api/v1/auth/logout` — responde `Set-Cookie` com `maxAge=0`, limpando os cookies.

**CORS (Gateway):**
- `Access-Control-Allow-Origin`: origin explícita (`ALLOWED_ORIGINS` env) — nunca `*`
- `Access-Control-Allow-Credentials: true`

**Frontend (`store/authStore.ts`):** deixa de setar `MockUser` sincronamente. Vira estado
derivado de uma query React Query (`useSession`) que chama um endpoint tipo
`GET /api/v1/users/me` pra obter o usuário logado (cookie já autentica a chamada). `login()`/
`logout()` viram mutations assíncronas.

---

## 3. Imagens de produto — MinIO

**Decisão:** MinIO (S3-compatible) local, self-hosted, adicionado ao
`infra/docker-compose.yml` como serviço `minio` (API `:9000`, console `:9001`), bucket
`catalog-images` com policy de leitura anônima (download público).

### Fluxo de cadastro (2 passos)

1. Admin preenche form de produto (sem imagem) → `POST /api/v1/catalog/products` → retorna
   `id` (UUID).
2. Admin envia imagem(ns) na tela seguinte → `POST /api/v1/catalog/products/{id}/images`
   (multipart/form-data, campo `files`, 1..N arquivos).
3. Catalog Service envia cada arquivo pro MinIO (chave `{productId}/{uuid}.{ext}`), grava a
   `url` pública em `product_images` — primeiro arquivo enviado vira `is_primary=true`.
4. Frontend consome a `url` do objeto MinIO diretamente em `<Image>` — não passa pelo Catalog
   Service pra servir o binário.

### Validação de upload

- Tipos aceitos: `image/jpeg`, `image/png`, `image/webp`
- Tamanho máx.: 5MB por arquivo
- Fora disso: `413`/`415` como Problem Details (ver seção 5)

### Remoção

`DELETE /api/v1/catalog/products/{id}/images/{imageId}` — remove do MinIO e do banco.

### Por que não URL externa ou disco local

URL externa exigiria admin hospedar em serviço terceiro manualmente antes de cadastrar — ruim
pra fluxo real de admin. Disco local em volume Docker não escala multi-instância nem migra bem
pra produção/k8s. MinIO resolve os dois problemas e tem migração direta pra S3 real depois.

---

## 4. Modelo de dados — alinhamento frontend/backend

**Decisão:** `types/product.ts` migra `id: number` → `id: string` (UUID) **antes** de integrar,
propagando a mudança por todos os consumidores (`ProductCard`, `ProductGrid`, `CartItem`,
`Order`, etc.). Evita bugs de comparação de tipo e retrabalho.

**Campos só-UI sem equivalente no backend hoje** (`stars`, `reviews`, `tag`, `tagColor`,
`installments`): ficam mock/hardcoded no frontend por enquanto. Não entram no schema do
Catalog Service nesta fase — exigiriam sistema de reviews (não planejado) e regra financeira de
parcelamento (fora de escopo do MVP de integração). Revisitar depois que o fluxo real estiver
funcionando.

Campo `Product` real após integração:

```ts
export type Product = {
    id: string              // UUID do Catalog Service
    name: string
    description: string
    price: number
    stockQuantity: number
    categoryId: string
    categoryName: string
    status: 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK'
    images: { id: string; url: string; isPrimary: boolean }[]

    // campos só-UI, mock por enquanto — não vêm do backend
    stars?: number
    reviews?: number
    tag?: 'OFERTA' | 'NOVO'
    tagColor?: string
    installments?: number
}
```

---

## 5. Contrato de erro — RFC 7807 Problem Details

Todos os serviços (via `@ControllerAdvice` usando `ProblemDetail` do Spring 6) devolvem erro
neste formato:

```json
{
  "type": "https://autohubstore.com/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Campo 'price' deve ser maior ou igual a zero",
  "instance": "/api/v1/catalog/products"
}
```

Frontend trata erro de forma uniforme: intercepta qualquer resposta não-2xx, lê `detail`/`title`
pra mensagem de UI, usa `status` pra decidir ação (401 → tenta refresh, 403 → redireciona,
demais → toast de erro).

---

## 6. Contrato de paginação — Spring Data Page

Toda listagem paginada (produtos, pedidos, usuários admin) devolve o shape padrão do
`Pageable`/`Page` do Spring Data — zero código extra no backend:

```json
{
  "content": [ /* itens */ ],
  "totalElements": 128,
  "totalPages": 13,
  "number": 0,
  "size": 10
}
```

Frontend: hook genérico `usePaginatedQuery` mapeia esse shape pra paginação da UI
(`components/catalog/Pagination`).

---

## 7. Client HTTP do frontend

**Novo arquivo:** `lib/api/client.ts`

- Axios instance, `baseURL = process.env.NEXT_PUBLIC_API_URL` (aponta pro Gateway, ex.
  `http://localhost:8001`)
- `withCredentials: true` (obrigatório por causa do cookie httpOnly)
- Interceptor de response: em 401, chama `POST /api/v1/auth/refresh` uma vez e repete o
  request original; se refresh também falhar, desloga e redireciona pro login.

**Variável de ambiente nova (frontend):**

```
NEXT_PUBLIC_API_URL=http://localhost:8001
```

---

## 8. React Query desde o início

Adotado junto com essa integração, não depois. Todo hook novo de dado server-side já nasce
com React Query:

- `useSession()` — usuário logado atual
- `useProducts(params)` — listagem paginada
- `useProduct(id)` — detalhe
- `useCreateProduct()`, `useUploadProductImages(productId)` — mutations admin

Substituem os imports estáticos de `lib/data/products.ts` etc. Ganha cache, loading/error state
e invalidação automática (ex: criar produto invalida `useProducts`).

---

## 9. Infraestrutura — MinIO no docker-compose

Serviços adicionados em `infra/docker-compose.yml`:

| Serviço | Porta | Descrição |
|---|---|---|
| `minio` | 9000 (API) / 9001 (console) | Storage S3-compatible pra imagens de produto |
| `minio-init` | — | Job one-shot: cria bucket `catalog-images` + policy de download público |

Variáveis de ambiente do Catalog Service:

```
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minio_admin
MINIO_SECRET_KEY=<secret>
MINIO_BUCKET=catalog-images
```

---

## 10. Escopo explicitamente fora desta fase

- Cart Service real — carrinho continua client-side (zustand `cartStore`) até Fase 4
- Order/Payment Service — front só integra quando essas fases chegarem
- Sistema de reviews/rating real — `stars`/`reviews` seguem mock
- Regra de parcelamento (`installments`) real — segue mock
- Kafka no frontend — frontend nunca consome evento diretamente, sempre via REST

---

## 11. Checklist de conclusão da Fase 2.5

- [ ] MinIO rodando no docker-compose, bucket criado
- [ ] Catalog Service: endpoints de upload/remoção de imagem implementados
- [ ] Auth Service + Gateway: cookies httpOnly funcionando (login/refresh/logout)
- [ ] CORS com credentials configurado e testado
- [ ] `lib/api/client.ts` criado com interceptor de refresh
- [ ] React Query configurado (`QueryClientProvider` no root)
- [ ] `types/product.ts` com `id: string`, propagado em todos os componentes
- [ ] `authStore` consumindo sessão real via `useSession`
- [ ] Erro (RFC 7807) e paginação (Spring `Page`) tratados de forma genérica no front
- [ ] Fluxo completo testado manualmente: login → listar produtos → admin cria produto → admin
      sobe imagem → produto aparece com imagem na loja
