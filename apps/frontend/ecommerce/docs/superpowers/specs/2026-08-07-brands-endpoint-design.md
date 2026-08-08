# Marcas dinâmicas (catalog-service) — Design

## Contexto

Marcas exibidas no ecommerce (`BrandsGrid` na home, `FilterSidebar` no catálogo) vêm de
lista hardcode em `apps/frontend/ecommerce/lib/data/brands.ts`. O banco do catalog-service
já tem entidade `Brand` (`domain/entity/Brand.java`) e migration de seed
(`V2__seed_brands.sql`) com ~20 marcas, mas não existe endpoint para lê-las. Objetivo:
expor essas marcas via API e trocar os componentes do frontend para consumi-las, seguindo
o mesmo padrão já usado por `Category`.

## Backend — catalog-service

Novo endpoint `GET /api/v1/catalog/brands`, público (sem `@SecurityRequirement`, igual GET
de categorias), retornando lista ordenada alfabeticamente por `name`. Sem `productCount`
(YAGNI — nenhuma tela usa contagem por marca hoje).

Arquivos novos, espelhando a fatia de `Category`:

- `repository/BrandRepository.java` — `JpaRepository<Brand, UUID>` com
  `List<Brand> findAllByOrderByNameAsc()`
- `domain/dto/response/BrandResponse.java` — `record BrandResponse(UUID id, String name, String slug)`
- `domain/mapper/BrandMapper.java` — `@Mapper` MapStruct, entity → response
- `service/BrandService.java` — `@Service`, `@RequiredArgsConstructor`, método
  `listBrands()` retornando `List<BrandResponse>`
- `controller/BrandController.java` — `@RestController`, `@RequestMapping("/api/v1/catalog/brands")`,
  implementa `BrandControllerDocs`, `@GetMapping` → `ResponseEntity<List<BrandResponse>>`
- `controller/docs/BrandControllerDocs.java` — `@Tag(name = "Brands", ...)`, `@Operation`
  documentando o único endpoint, mesmo estilo de `CategoryControllerDocs`

Sem criação/edição de marca nesta entrega — só leitura.

## Frontend

- `types/brand.ts` — `export type Brand = { id: string; name: string; slug: string }`
  (substitui o tipo atual em `types/product.ts`, que só tinha `id`/`name`)
- `lib/api/catalog.ts` — `fetchBrands()`, mesmo formato de `fetchCategories()`
- `hooks/useBrands.ts` — `useQuery({ queryKey: ['brands'], queryFn: fetchBrands })`
- Remove `lib/data/brands.ts`
- `BrandsGrid.tsx` e `FilterSidebar.tsx` trocam `import { brands } from '@/lib/data/brands'`
  por `useBrands()`, com fallback `?? []` (mesmo padrão de `useCategories` no
  `FilterSidebar` hoje — sem loading/error UI nova)

## Comportamento "mostrar 10 + expandir"

Aplica nos dois componentes (`BrandsGrid` e `FilterSidebar`), de forma independente:

- `useState(false)` local para "expanded" (sem hook/store compartilhado — só 2 usos, não
  justifica abstração)
- Lista exibida: `expanded ? brands : brands.slice(0, 10)`
- Se `brands.length > 10`, mostra botão abaixo da lista: `"Ver mais (+N)"` quando fechado,
  `"Ver menos"` quando aberto (N = `brands.length - 10`)
- Estilo do botão reaproveita variáveis CSS já existentes (`--color-primary`,
  `--font-rajdhani`) e o visual de botão-texto já usado em `filterBtn`/`labelText` —
  sem introduzir padrão visual novo
- `FilterSidebar`: botão abaixo de `.checkList`, dentro do grupo "MARCAS"
- `BrandsGrid`: botão centralizado abaixo do `.grid`

## Fora de escopo

- CRUD de marcas (create/update/delete)
- `productCount` por marca
- Corrigir o bug existente de `ProductResponse` mapeando `product.brand` para
  `categoryName` no frontend (`lib/api/catalog.ts:41`) — não faz parte do pedido, é usado
  em outros componentes (`ProductCard`, `ProductInfo`, `CartTable`, etc.) fora do escopo
  desta mudança
