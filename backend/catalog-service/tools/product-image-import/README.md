# product-image-import

Script standalone que sobe as 60 imagens de `images/*.webp` pro catalog-service,
associando cada imagem ao produto correspondente via API REST já existente.
Não mexe direto em banco nem em MinIO — reusa a mesma lógica de upload que a
aplicação usa em produção (`ProductImageService`), então validação de
content-type, tamanho máximo (5MB) e definição da imagem primária já vêm de
graça.

## Como funciona

Cada arquivo em `images/` é nomeado igual ao `slug` do produto (ex.:
`vonixx-v-mol-shampoo-automotivo.webp` → produto com slug
`vonixx-v-mol-shampoo-automotivo`, criado em
`V4__seed_products.sql`). Pra cada imagem, o script:

1. Extrai o slug do nome do arquivo (sem a extensão `.webp`).
2. `GET /api/v1/catalog/products/slug/{slug}` — resolve o `id` do produto.
   - Se não existir produto com esse slug, marca como **não encontrado** e
     segue pro próximo arquivo (não interrompe o processo).
3. Confere se o produto retornado já tem alguma imagem (`images` não vazio).
   Se já tiver, marca como **pulado** — reexecutar o script não duplica
   upload (idempotente).
4. `POST /api/v1/catalog/products/{id}/images` — envia o arquivo como
   `multipart/form-data` (campo `files`). O `ProductImageService` no backend
   faz o `PutObject` no MinIO e grava a linha em `product_images`
   (primeira imagem do produto vira `is_primary = true` automaticamente).
5. No final, imprime um resumo: enviadas / puladas / não encontradas / falhas.

Não há chamada direta ao MinIO nem ao Postgres no script — tudo passa pela
API do catalog-service, então o comportamento é idêntico ao de um upload
manual feito por um usuário real.

## Pré-requisitos

- Infra local de pé: `cd infra && docker compose up -d postgres-catalog redis minio minio-init`
- `catalog-service` rodando (Flyway aplica `V1`–`V4` automaticamente no boot,
  criando as tabelas, marcas, categorias e os 60 produtos).
- Python 3.11+ instalado.

## Setup

```bash
cd backend/catalog-service/tools/product-image-import
python -m venv .venv
.venv\Scripts\activate      # Windows
# source .venv/bin/activate # Linux/Mac
pip install -r requirements.txt
copy .env.example .env      # Windows
# cp .env.example .env      # Linux/Mac
```

Ajuste `.env` se o catalog-service não estiver em `http://localhost:8004`
(porta configurada em `application.yml`, `server.port: 8004`).

## Rodando

```bash
python import_images.py
```

Saída esperada (exemplo):

```
[OK] vonixx-v-mol-shampoo-automotivo: imagem enviada
[OK] pneu-michelin-primacy-4-p-21565r16-102-h: imagem enviada
...

===== Resumo =====
Enviadas:        60
Puladas (ja OK): 0
Nao encontradas: 0
Falhas:          0
```

Rodar de novo depois que já subiu tudo só reporta `Puladas (ja OK): 60` —
não duplica imagem em nenhum produto.

## Validando o resultado

1. **Pelo resumo do script** — `Nao encontradas` e `Falhas` devem ficar em 0.
   Se `Nao encontradas` > 0, o slug do arquivo não bate com nenhum `slug`
   inserido em `V4__seed_products.sql` (checar nome do arquivo x migration).

2. **Pela API** — conferir um produto específico:
   ```bash
   curl http://localhost:8004/api/v1/catalog/products/slug/vonixx-v-mol-shampoo-automotivo
   ```
   O campo `images` deve vir com 1 item, `isPrimary: true`, e `url` no
   formato `/catalog-images/{productId}/{uuid}.webp`.

3. **Pelo MinIO Console** (`http://localhost:9001`, login
   `minio_admin` / `minio_pass`) — bucket `catalog-images` deve ter 60
   objetos, um por produto, cada um dentro de uma pasta `{productId}/`.

4. **Pelo banco** (opcional, direto no Postgres do catalog-service):
   ```sql
   SELECT COUNT(*) FROM product_images; -- deve dar 60
   ```

## Erros comuns

| Sintoma | Causa provável |
|---|---|
| `ERRO: nenhum .webp encontrado em images` | Rodou o script de outra pasta ou `IMAGES_DIR` errado no `.env` |
| Todos `[NAO ENCONTRADO]` | Migrations V2-V4 não rodaram ainda, ou catalog-service apontando pra outro banco |
| `[ERRO] ... falha ao consultar produto (Connection refused)` | catalog-service não está de pé, ou `CATALOG_API_BASE_URL` errado no `.env` |
| `[ERRO] ... falha ao enviar imagem` | MinIO fora do ar, bucket `catalog-images` não criado (checar `minio-init` no docker compose), ou arquivo maior que 5MB |
