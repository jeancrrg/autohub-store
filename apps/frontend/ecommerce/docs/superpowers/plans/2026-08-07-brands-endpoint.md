# Marcas dinâmicas (catalog-service) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose catalog-service brands via a public `GET /api/v1/catalog/brands` endpoint and replace the hardcoded frontend brand list with data from that endpoint, adding a "show 10 + expand" truncation to `BrandsGrid` and `FilterSidebar`.

**Architecture:** Backend adds a read-only vertical slice (Repository → Mapper → Service → Controller + Swagger docs interface) mirroring the existing `Category` slice exactly, since `Brand` entity and seed data already exist. Frontend adds a `fetchBrands`/`useBrands` data-fetching pair mirroring `fetchCategories`/`useCategories`, then swaps two components from the static `lib/data/brands.ts` array to the hook, adding local expand/collapse state to each.

**Tech Stack:** Java 25, Spring Boot 3.x, Gradle, MapStruct, Springdoc OpenAPI (backend/catalog-service). Next.js 16, React 19, TypeScript, TanStack Query, Vitest (apps/frontend/ecommerce).

## Global Constraints

- Package base: `com.autohubstore.catalogservice`
- Every JPA-adjacent/API field mirrors existing `Category` slice patterns exactly (naming, layering, doc style)
- Checkstyle: 4-space indent, no tabs, blank line after class `{` and before final `}`, line length ≤130 chars, no magic numbers, braces required on all control structures
- `@RequiredArgsConstructor` + `private final` fields for DI — never field `@Autowired`
- No `@Override` on controller methods implementing docs interfaces
- MapStruct for all entity↔DTO mapping — never manual field mapping
- GET brands endpoint is public — no `@SecurityRequirement`, matching `CategoryController.listCategories()`
- No backend automated tests exist yet for catalog-service (verified: `src/test` has no Category tests either) — this plan follows that existing convention and verifies the backend slice manually via `curl`, not JUnit. Do not introduce a new backend test pattern unilaterally.
- Frontend hook tests use Vitest + `axios-mock-adapter`, mirroring `hooks/__tests__/useCategories.test.tsx` exactly
- No component-level tests exist for `BrandsGrid`/`FilterSidebar` (verified: `components/**/__tests__` is empty) — this plan does not add new component test infrastructure, matching the existing convention

---

### Task 1: Backend — Brand read slice (Repository, Response, Mapper, Service, Controller, Docs)

**Files:**
- Create: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/repository/BrandRepository.java`
- Create: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/domain/dto/response/BrandResponse.java`
- Create: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/domain/mapper/BrandMapper.java`
- Create: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/service/BrandService.java`
- Create: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/controller/BrandController.java`
- Create: `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/controller/docs/BrandControllerDocs.java`
- Reference (read-only, do not modify): `backend/catalog-service/src/main/java/com/autohubstore/catalogservice/domain/entity/Brand.java` (fields: `id: UUID`, `name: String`, `slug: String`, `createdAt: Instant`)

**Interfaces:**
- Produces: `BrandResponse(UUID id, String name, String slug)` — consumed by frontend `BrandResponse` shape in Task 2
- Produces: `GET /api/v1/catalog/brands` → `200 List<BrandResponse>`, alphabetical by `name`, no auth required

- [ ] **Step 1: Create `BrandRepository`**

```java
package com.autohubstore.catalogservice.repository;

import com.autohubstore.catalogservice.domain.entity.Brand;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    List<Brand> findAllByOrderByNameAsc();

}
```

- [ ] **Step 2: Create `BrandResponse`**

```java
package com.autohubstore.catalogservice.domain.dto.response;

import java.util.UUID;

public record BrandResponse(
        UUID id,
        String name,
        String slug
) {}
```

- [ ] **Step 3: Create `BrandMapper`**

```java
package com.autohubstore.catalogservice.domain.mapper;

import com.autohubstore.catalogservice.domain.dto.response.BrandResponse;
import com.autohubstore.catalogservice.domain.entity.Brand;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    BrandResponse toResponse(Brand brand);

}
```

- [ ] **Step 4: Create `BrandService`**

```java
package com.autohubstore.catalogservice.service;

import com.autohubstore.catalogservice.domain.dto.response.BrandResponse;
import com.autohubstore.catalogservice.domain.mapper.BrandMapper;
import com.autohubstore.catalogservice.repository.BrandRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Transactional(readOnly = true)
    public List<BrandResponse> listBrands() {
        return brandRepository.findAllByOrderByNameAsc().stream()
                .map(brandMapper::toResponse)
                .collect(Collectors.toList());
    }

}
```

- [ ] **Step 5: Create `BrandControllerDocs`**

```java
package com.autohubstore.catalogservice.controller.docs;

import com.autohubstore.catalogservice.domain.dto.response.BrandResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Brands", description = """
        Marcas de produtos. Leitura pública, sem escrita nesta versão.
        """)
public interface BrandControllerDocs {

    @Operation(
            summary = "Listar marcas",
            description = "Retorna todas as marcas cadastradas, ordenadas alfabeticamente por `name`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de marcas retornada com sucesso")
    })
    ResponseEntity<List<BrandResponse>> listBrands();

}
```

- [ ] **Step 6: Create `BrandController`**

```java
package com.autohubstore.catalogservice.controller;

import com.autohubstore.catalogservice.controller.docs.BrandControllerDocs;
import com.autohubstore.catalogservice.domain.dto.response.BrandResponse;
import com.autohubstore.catalogservice.service.BrandService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog/brands")
@RequiredArgsConstructor
public class BrandController implements BrandControllerDocs {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<List<BrandResponse>> listBrands() {
        return ResponseEntity.ok(brandService.listBrands());
    }

}
```

- [ ] **Step 7: Compile and verify manually**

Run: `cd backend/catalog-service && ./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`, no checkstyle violations reported for the 6 new files.

With the service running locally (`./gradlew bootRun`, requires `postgres-catalog` up per `infra/docker-compose.yml`):

Run: `curl http://localhost:8003/api/v1/catalog/brands`
Expected: `200` with a JSON array of `{id, name, slug}` objects, ~20 entries, sorted alphabetically (`Akrapovič` before `BBS`, etc — matches names seeded in `V2__seed_brands.sql`).

Run: `curl http://localhost:8003/swagger-ui.html` (or check `/v3/api-docs`)
Expected: "Brands" tag present with the `GET /api/v1/catalog/brands` operation documented.

- [ ] **Step 8: Commit**

```bash
git add backend/catalog-service/src/main/java/com/autohubstore/catalogservice/repository/BrandRepository.java \
        backend/catalog-service/src/main/java/com/autohubstore/catalogservice/domain/dto/response/BrandResponse.java \
        backend/catalog-service/src/main/java/com/autohubstore/catalogservice/domain/mapper/BrandMapper.java \
        backend/catalog-service/src/main/java/com/autohubstore/catalogservice/service/BrandService.java \
        backend/catalog-service/src/main/java/com/autohubstore/catalogservice/controller/BrandController.java \
        backend/catalog-service/src/main/java/com/autohubstore/catalogservice/controller/docs/BrandControllerDocs.java
git commit -m "feat (catalog-service): Adicionado endpoint GET /api/v1/catalog/brands"
```

---

### Task 2: Frontend — `Brand` type, `fetchBrands`, `useBrands` hook

**Files:**
- Create: `apps/frontend/ecommerce/types/brand.ts`
- Modify: `apps/frontend/ecommerce/types/product.ts` (remove the `Brand` type — moved to `types/brand.ts`)
- Modify: `apps/frontend/ecommerce/lib/api/catalog.ts` (add `fetchBrands`)
- Create: `apps/frontend/ecommerce/hooks/useBrands.ts`
- Test: `apps/frontend/ecommerce/hooks/__tests__/useBrands.test.tsx`

**Interfaces:**
- Consumes: `apiClient` from `@/lib/api/client` (already used by `fetchCategories`)
- Produces: `export type Brand = { id: string; name: string; slug: string }` from `@/types/brand`
- Produces: `export async function fetchBrands(): Promise<Brand[]>` from `@/lib/api/catalog`
- Produces: `export function useBrands()` from `@/hooks/useBrands` — TanStack Query hook, `queryKey: ['brands']`, `data` typed `Brand[] | undefined`

- [ ] **Step 1: Create `types/brand.ts`**

```typescript
export type Brand = {
    id: string
    name: string
    slug: string
}
```

- [ ] **Step 2: Remove `Brand` type from `types/product.ts`**

In `apps/frontend/ecommerce/types/product.ts`, remove lines 29-32:

```typescript
export type Brand = {
    id: string
    name: string
}
```

(No other file imports `Brand` from `@/types/product` — only `lib/data/brands.ts`, which Task 5 deletes.)

- [ ] **Step 3: Write the failing test for `fetchBrands`/`useBrands`**

```typescript
import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '@/lib/api/client'
import { useBrands } from '../useBrands'

function wrapper({ children }: { children: React.ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useBrands', () => {
    it('retorna a lista de marcas com id, name e slug', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onGet('/api/v1/catalog/brands').reply(200, [
            { id: '3fa85f64-5717-4562-b3fc-2c963f66afa6', name: 'Akrapovič', slug: 'akrapovic' },
            { id: '4fa85f64-5717-4562-b3fc-2c963f66afa7', name: 'BBS', slug: 'bbs' },
        ])

        const { result } = renderHook(() => useBrands(), { wrapper })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data).toHaveLength(2)
        expect(result.current.data?.[0].name).toBe('Akrapovič')
        expect(result.current.data?.[1].slug).toBe('bbs')
    })
})
```

Save this to `apps/frontend/ecommerce/hooks/__tests__/useBrands.test.tsx`.

- [ ] **Step 4: Run test to verify it fails**

Run: `cd apps/frontend/ecommerce && npx vitest run hooks/__tests__/useBrands.test.tsx`
Expected: FAIL — `useBrands` module not found (doesn't exist yet).

- [ ] **Step 5: Add `fetchBrands` to `lib/api/catalog.ts`**

In `apps/frontend/ecommerce/lib/api/catalog.ts`, add near the bottom of the file (after `fetchCategories`):

```typescript
import type { Brand } from '@/types/brand'

type BrandResponse = {
    id: string
    name: string
    slug: string
}

export async function fetchBrands(): Promise<Brand[]> {
    const { data } = await apiClient.get<BrandResponse[]>('/api/v1/catalog/brands')
    return data
}
```

(Add the `import type { Brand } from '@/types/brand'` line to the existing import block at the top of the file, not as a second import statement.)

- [ ] **Step 6: Create `hooks/useBrands.ts`**

```typescript
import { useQuery } from '@tanstack/react-query'
import { fetchBrands } from '@/lib/api/catalog'

export function useBrands() {
    return useQuery({
        queryKey: ['brands'],
        queryFn: fetchBrands,
    })
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `cd apps/frontend/ecommerce && npx vitest run hooks/__tests__/useBrands.test.tsx`
Expected: PASS

- [ ] **Step 8: Type-check**

Run: `cd apps/frontend/ecommerce && npm run type-check`
Expected: no errors (confirms `types/product.ts` edit didn't break other files — none should reference `Brand` from there anymore).

- [ ] **Step 9: Commit**

```bash
git add apps/frontend/ecommerce/types/brand.ts \
        apps/frontend/ecommerce/types/product.ts \
        apps/frontend/ecommerce/lib/api/catalog.ts \
        apps/frontend/ecommerce/hooks/useBrands.ts \
        apps/frontend/ecommerce/hooks/__tests__/useBrands.test.tsx
git commit -m "feat (frontend): Adicionado fetchBrands e useBrands consumindo endpoint de marcas"
```

---

### Task 3: Frontend — `FilterSidebar` consumes `useBrands`, adds show-10-and-expand

**Files:**
- Modify: `apps/frontend/ecommerce/components/catalog/FilterSidebar/FilterSidebar.tsx`
- Modify: `apps/frontend/ecommerce/components/catalog/FilterSidebar/FilterSidebar.module.css`

**Interfaces:**
- Consumes: `useBrands()` from `@/hooks/useBrands` (Task 2), returns `{ data: Brand[] | undefined }`
- Consumes: `Brand` type `{ id: string; name: string; slug: string }` from `@/types/brand`

- [ ] **Step 1: Replace the static import and brand list with `useBrands`, add expand state**

In `apps/frontend/ecommerce/components/catalog/FilterSidebar/FilterSidebar.tsx`, replace:

```typescript
import { useCategories } from '@/hooks/useCategories'
import { brands } from '@/lib/data/brands'
import styles from './FilterSidebar.module.css'
```

with:

```typescript
import { useState } from 'react'
import { useCategories } from '@/hooks/useCategories'
import { useBrands } from '@/hooks/useBrands'
import styles from './FilterSidebar.module.css'
```

Replace:

```typescript
export function FilterSidebar({ filters, onChange }: FilterSidebarProps) {
    const { data: categories } = useCategories()
```

with:

```typescript
const VISIBLE_BRANDS_LIMIT = 10

export function FilterSidebar({ filters, onChange }: FilterSidebarProps) {
    const { data: categories } = useCategories()
    const { data: allBrands } = useBrands()
    const [brandsExpanded, setBrandsExpanded] = useState(false)

    const brands = allBrands ?? []
    const visibleBrands = brandsExpanded ? brands : brands.slice(0, VISIBLE_BRANDS_LIMIT)
    const hiddenBrandsCount = brands.length - VISIBLE_BRANDS_LIMIT
```

Replace the MARCAS block:

```typescript
            <div className={styles.group}>
                <h3 className={styles.groupTitle}>MARCAS</h3>
                <div className={styles.checkList}>
                    {brands.map((brand) => (
                        <label key={brand.id} className={styles.checkLabel}>
                            <input
                                type="checkbox"
                                checked={filters.brands.includes(brand.id)}
                                onChange={() => toggleBrand(brand.id)}
                                style={{ accentColor: 'var(--color-primary)' }}
                            />
                            <span className={styles.checkText}>{brand.name}</span>
                        </label>
                    ))}
                </div>
            </div>
```

with:

```typescript
            <div className={styles.group}>
                <h3 className={styles.groupTitle}>MARCAS</h3>
                <div className={styles.checkList}>
                    {visibleBrands.map((brand) => (
                        <label key={brand.id} className={styles.checkLabel}>
                            <input
                                type="checkbox"
                                checked={filters.brands.includes(brand.id)}
                                onChange={() => toggleBrand(brand.id)}
                                style={{ accentColor: 'var(--color-primary)' }}
                            />
                            <span className={styles.checkText}>{brand.name}</span>
                        </label>
                    ))}
                </div>
                {hiddenBrandsCount > 0 && (
                    <button
                        type="button"
                        onClick={() => setBrandsExpanded((expanded) => !expanded)}
                        className={styles.expandBtn}
                    >
                        {brandsExpanded ? 'Ver menos' : `Ver mais (+${hiddenBrandsCount})`}
                    </button>
                )}
            </div>
```

- [ ] **Step 2: Add `.expandBtn` style to `FilterSidebar.module.css`**

Append to `apps/frontend/ecommerce/components/catalog/FilterSidebar/FilterSidebar.module.css`:

```css
.expandBtn {
    display: block;
    margin-top: 8px;
    font-family: var(--font-rajdhani);
    font-weight: 600;
    font-size: 0.8125rem;
    letter-spacing: 0.05em;
    color: var(--color-primary);
    background: none;
    border: none;
    padding: 0;
    cursor: pointer;
    transition: opacity 0.2s;
}

.expandBtn:hover {
    opacity: 0.75;
}
```

- [ ] **Step 3: Type-check**

Run: `cd apps/frontend/ecommerce && npm run type-check`
Expected: no errors.

- [ ] **Step 4: Manual verification**

Run: `cd apps/frontend/ecommerce && npm run dev`, open the catalog page in a browser.
Expected: MARCAS section shows at most 10 checkboxes plus a "Ver mais (+N)" button (N = total brands from the API minus 10); clicking it reveals the rest and the button changes to "Ver menos"; clicking again collapses back to 10. Checkbox filtering (`toggleBrand`) still works on any visible brand.

- [ ] **Step 5: Commit**

```bash
git add apps/frontend/ecommerce/components/catalog/FilterSidebar/FilterSidebar.tsx \
        apps/frontend/ecommerce/components/catalog/FilterSidebar/FilterSidebar.module.css
git commit -m "feat (frontend): FilterSidebar consome marcas da API com limite de 10 e expandir"
```

---

### Task 4: Frontend — `BrandsGrid` consumes `useBrands`, adds show-10-and-expand

**Files:**
- Modify: `apps/frontend/ecommerce/components/home/BrandsGrid/BrandsGrid.tsx`
- Modify: `apps/frontend/ecommerce/components/home/BrandsGrid/BrandsGrid.module.css`

**Interfaces:**
- Consumes: `useBrands()` from `@/hooks/useBrands` (Task 2), returns `{ data: Brand[] | undefined }`

- [ ] **Step 1: Replace the static import with `useBrands`, add expand state**

Replace the full contents of `apps/frontend/ecommerce/components/home/BrandsGrid/BrandsGrid.tsx` with:

```typescript
'use client'

import { useState } from 'react'
import { useBrands } from '@/hooks/useBrands'
import styles from './BrandsGrid.module.css'

const VISIBLE_BRANDS_LIMIT = 10

export function BrandsGrid() {
    const { data: allBrands } = useBrands()
    const [expanded, setExpanded] = useState(false)

    const brands = allBrands ?? []
    const visibleBrands = expanded ? brands : brands.slice(0, VISIBLE_BRANDS_LIMIT)
    const hiddenCount = brands.length - VISIBLE_BRANDS_LIMIT

    return (
        <section className={styles.section}>
            <div className={styles.inner}>
                <div className={styles.headerCenter}>
                    <div className={styles.labelRow}>
                        <div className={styles.labelBar} />
                        <span className={styles.labelText}>Parceiros</span>
                        <div className={styles.labelBar} />
                    </div>
                    <h2 className={styles.sectionTitle}>Marcas Oficiais</h2>
                </div>
                <div className={styles.grid}>
                    {visibleBrands.map((brand) => (
                        <div key={brand.id} className={styles.brandCard}>
                            <span className={styles.brandName}>{brand.name}</span>
                        </div>
                    ))}
                </div>
                {hiddenCount > 0 && (
                    <div className={styles.expandRow}>
                        <button
                            type="button"
                            onClick={() => setExpanded((current) => !current)}
                            className={styles.expandBtn}
                        >
                            {expanded ? 'Ver menos' : `Ver mais (+${hiddenCount})`}
                        </button>
                    </div>
                )}
            </div>
        </section>
    )
}
```

`BrandsGrid` becomes a client component (`'use client'`) because `useBrands` uses TanStack Query hooks — required since `app/(public)/page.tsx` currently renders it as presumably a server component.

- [ ] **Step 2: Add `.expandRow`/`.expandBtn` styles to `BrandsGrid.module.css`**

Append to `apps/frontend/ecommerce/components/home/BrandsGrid/BrandsGrid.module.css`:

```css
.expandRow {
    display: flex;
    justify-content: center;
    margin-top: 24px;
}

.expandBtn {
    font-family: var(--font-rajdhani);
    font-weight: 700;
    font-size: 13px;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: var(--color-primary);
    background: none;
    border: none;
    padding: 8px 0;
    cursor: pointer;
    transition: opacity 0.2s;
}

.expandBtn:hover {
    opacity: 0.75;
}
```

- [ ] **Step 3: Type-check**

Run: `cd apps/frontend/ecommerce && npm run type-check`
Expected: no errors.

- [ ] **Step 4: Manual verification**

Run: `cd apps/frontend/ecommerce && npm run dev`, open the home page.
Expected: "Marcas Oficiais" grid shows at most 10 cards plus a centered "Ver mais (+N)" button under the grid; clicking expands to show all brands and the button becomes "Ver menos".

- [ ] **Step 5: Commit**

```bash
git add apps/frontend/ecommerce/components/home/BrandsGrid/BrandsGrid.tsx \
        apps/frontend/ecommerce/components/home/BrandsGrid/BrandsGrid.module.css
git commit -m "feat (frontend): BrandsGrid consome marcas da API com limite de 10 e expandir"
```

---

### Task 5: Frontend — remove hardcoded brand list

**Files:**
- Delete: `apps/frontend/ecommerce/lib/data/brands.ts`

**Interfaces:**
- Consumes: nothing (this task only removes dead code after Tasks 3 and 4 have removed all references)

- [ ] **Step 1: Confirm no remaining references**

Run: `cd apps/frontend/ecommerce && grep -rn "lib/data/brands" --include="*.ts*" .`
Expected: no matches (both `BrandsGrid.tsx` and `FilterSidebar.tsx` were updated in Tasks 3–4).

- [ ] **Step 2: Delete the file**

Run: `rm apps/frontend/ecommerce/lib/data/brands.ts`

- [ ] **Step 3: Full verification pass**

Run: `cd apps/frontend/ecommerce && npm run type-check && npx vitest run`
Expected: type-check passes; all Vitest suites pass, including the new `useBrands.test.tsx` and the untouched `useCategories.test.tsx`/`cartStore.test.ts`.

- [ ] **Step 4: Commit**

```bash
git add -u apps/frontend/ecommerce/lib/data/brands.ts
git commit -m "feat (frontend): Removido lista hardcode de marcas, substituída pelo endpoint do catalog-service"
```

---

## Self-Review Notes

- **Spec coverage:** endpoint (Task 1) ✓, doc parity with `CategoryControllerDocs` (Task 1 Step 5) ✓, frontend fetch/hook (Task 2) ✓, both hardcoded call sites replaced (Tasks 3–4) ✓, 10-item cap + expand in both places (Tasks 3–4) ✓, hardcoded list removed (Task 5) ✓, out-of-scope items (CRUD, productCount, `categoryName`-as-brand bug) untouched ✓.
- **Type consistency:** `BrandResponse` backend record `(id, name, slug)` matches frontend `BrandResponse`/`Brand` shape `{id, name, slug}` used in Task 2 Step 5 and the Task 2 Step 3 test fixture. `useBrands()` return shape (`{data: Brand[] | undefined}`) used consistently in Tasks 3 and 4.
- **No placeholders:** every step has literal code, exact file paths, and concrete verification commands/expected output.
