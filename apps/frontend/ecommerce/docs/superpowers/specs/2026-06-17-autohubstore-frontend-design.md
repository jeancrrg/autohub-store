# AutoHubStore Frontend — Design Spec
**Data:** 2026-06-17  
**Status:** Aprovado

---

## Contexto

E-commerce automotivo fictício para estudo de Backend Java. Frontend existe para suportar os fluxos do e-commerce. Backend em status "Planejado" — frontend usa mock data estático.

---

## Decisões

| Decisão | Escolha | Motivo |
|---------|---------|--------|
| Mock data | Estático (objetos hardcoded) | Backend não existe |
| Autenticação | Sem validação — login redireciona direto para conta | Foco no layout/UI |
| Admin | Todas as 4 abas com mock data | Pedido do usuário |
| Arquitetura | Next.js App Router, Server + Client Components | Alinhado com spec do Notion |

---

## Stack

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| Next.js | 14+ (App Router) | Framework SSR/SSG |
| TypeScript | 5+ | Tipagem estática |
| Tailwind CSS | 3+ | Estilização via utilitários |
| Zustand | 4+ | Estado global (carrinho, auth fake) |
| React Hook Form | 7+ | Formulários checkout/login |
| Zod | 3+ | Validação de schemas |

**Sem:** React Query, Axios (sem chamadas HTTP com mock estático)

---

## Design System

### Cores (Tailwind config)
```
primary:      #E10600   (vermelho — CTAs, destaques, hover)
primary-dark: #c50500   (hover do primary)
dark:         #0B0B0B   (fundo dark, texto escuro)
dark-2:       #111111
dark-3:       #141414
dark-border:  #1a1a1a   (bordas admin)
gray-border:  #ebebeb   (bordas store)
gray-light:   #f8f8f8   (fundo seções alternadas)
```

### Fontes
- **Rajdhani** — headings, nav, badges, preços (weight 500/600/700)
- **Exo 2** — nomes de produtos (weight 400/500/600/700/800)
- **Inter** — corpo de texto, labels (weight 400/500/600)

### Temas
- **Store** (público + customer): fundo branco/`gray-light`, texto `dark`
- **Login**: fundo branco/`gray-light`, card `dark-2`
- **Admin**: fundo `dark` 100vh, sidebar `#080808`

---

## Estrutura de Diretórios

```
ecommerce-autohubstore/
├── app/
│   ├── layout.tsx                    # Root layout (fontes, providers Zustand)
│   ├── (public)/
│   │   ├── layout.tsx                # Header + Footer
│   │   ├── page.tsx                  # Home /
│   │   ├── catalog/page.tsx          # /catalog
│   │   ├── products/[id]/page.tsx    # /products/[id]
│   │   ├── login/page.tsx            # /login
│   │   └── search/page.tsx           # /search?q=...
│   ├── (customer)/
│   │   ├── layout.tsx                # Header + Footer
│   │   ├── cart/page.tsx             # /cart
│   │   ├── checkout/page.tsx         # /checkout (4 steps)
│   │   ├── account/page.tsx          # /account
│   │   └── orders/page.tsx           # /orders
│   └── (admin)/
│       ├── layout.tsx                # Layout admin (sem Header/Footer da loja)
│       └── admin/
│           ├── page.tsx              # redirect → /admin/dashboard
│           ├── dashboard/page.tsx
│           ├── products/page.tsx
│           ├── orders/page.tsx
│           └── users/page.tsx
├── components/
│   ├── layout/
│   │   ├── Header.tsx               # Client — topbar, nav, search, cart badge
│   │   ├── Footer.tsx               # Server
│   │   └── AdminSidebar.tsx         # Client — tabs ativas
│   ├── home/
│   │   ├── HeroBanner.tsx           # Server
│   │   ├── TrustBadges.tsx          # Server
│   │   ├── CategoryGrid.tsx         # Server
│   │   ├── ProductSection.tsx       # Server — reusável (Destaque + Mais Vendidos)
│   │   ├── PromoBanner.tsx          # Server
│   │   ├── BrandsGrid.tsx           # Server
│   │   └── Newsletter.tsx           # Client (input email)
│   ├── catalog/
│   │   ├── FilterSidebar.tsx        # Client — checkboxes, price range, toggle
│   │   ├── ProductGrid.tsx          # Client — recebe products filtrados
│   │   ├── ProductCard.tsx          # Server
│   │   ├── SortBar.tsx              # Client — ordenação, view toggle
│   │   └── Pagination.tsx           # Client
│   ├── product/
│   │   ├── ImageGallery.tsx         # Client — thumbnail select
│   │   ├── ProductInfo.tsx          # Client — qty, add to cart (Zustand)
│   │   └── ProductTabs.tsx          # Client — desc/specs/compat/reviews
│   ├── cart/
│   │   ├── CartTable.tsx            # Client — qty editable, remove
│   │   └── OrderSummary.tsx         # Client — totais Zustand
│   ├── checkout/
│   │   ├── CheckoutStepper.tsx      # Client — controla step atual
│   │   ├── AddressForm.tsx          # React Hook Form + Zod
│   │   ├── DeliveryStep.tsx
│   │   ├── PaymentStep.tsx
│   │   └── ConfirmationStep.tsx     # Tela de sucesso
│   ├── account/
│   │   ├── AccountSidebar.tsx       # Client — tab nav
│   │   ├── OrdersTab.tsx
│   │   └── ProfileTab.tsx
│   ├── admin/
│   │   ├── MetricsCards.tsx
│   │   ├── RevenueChart.tsx         # Barras CSS puras (sem lib de chart)
│   │   ├── TopProducts.tsx
│   │   ├── RecentOrders.tsx
│   │   ├── ProductsTable.tsx        # Mock CRUD (sem persist)
│   │   ├── OrdersTable.tsx
│   │   └── UsersTable.tsx
│   └── ui/
│       ├── Button.tsx
│       ├── Input.tsx
│       └── Badge.tsx
├── lib/
│   └── data/
│       ├── products.ts              # 8 produtos (idênticos ao HTML)
│       ├── categories.ts            # 6 categorias com ícone e contagem
│       ├── brands.ts                # 12 marcas
│       ├── orders.ts                # 3 pedidos customer + 5 admin
│       └── admin.ts                 # métricas, sales bars, top produtos
├── store/
│   ├── cartStore.ts                 # items, add, remove, updateQty, total, count
│   └── authStore.ts                 # isLoggedIn, user mock, login(), logout()
└── types/
    ├── product.ts
    ├── order.ts
    └── cart.ts
```

---

## Rotas

| Tela | Rota | Grupo |
|------|------|-------|
| Home | `/` | (public) |
| Catálogo | `/catalog` | (public) |
| Produto | `/products/[id]` | (public) |
| Busca | `/search` | (public) |
| Login | `/login` | (public) |
| Carrinho | `/cart` | (customer) |
| Checkout | `/checkout` | (customer) |
| Minha Conta | `/account` | (customer) |
| Meus Pedidos | `/orders` | (customer) |
| Admin Dashboard | `/admin/dashboard` | (admin) |
| Admin Produtos | `/admin/products` | (admin) |
| Admin Pedidos | `/admin/orders` | (admin) |
| Admin Usuários | `/admin/users` | (admin) |

---

## Data Layer

### Types principais
```typescript
type Product = {
  id: number; name: string; brand: string;
  price: number; oldPrice?: number;
  tag?: 'OFERTA' | 'NOVO'; tagColor: string;
  stars: number; reviews: number;
  installments: number; inStock: boolean;
  description: string; specs: Record<string, string>;
  category: string;
}

type Order = {
  id: string; date: string; status: OrderStatus;
  items: string; total: string;
}

type AdminOrder = Order & {
  client: string; statusColor: string; statusBg: string;
}
```

### Zustand — CartStore
```typescript
type CartStore = {
  items: CartItem[]
  addItem: (product: Product, qty: number) => void
  removeItem: (id: number) => void
  updateQty: (id: number, qty: number) => void
  total: number        // derivado de items
  count: number        // total de unidades
  clearCart: () => void
}
```

### Zustand — AuthStore
```typescript
type AuthStore = {
  isLoggedIn: boolean
  user: MockUser | null   // { name, email, initials } hardcoded
  login: () => void       // isLoggedIn = true, user = mock
  logout: () => void      // isLoggedIn = false, user = null
}
```

---

## Comportamentos Client-Side

| Componente | Comportamento |
|-----------|---------------|
| Header | Badge carrinho via `cartStore.count`, atualiza em tempo real |
| FilterSidebar | Filtra array local por categoria, marca, faixa de preço, estoque |
| SortBar | Ordena array local por relevância, menor/maior preço, mais vendidos |
| ProductInfo | Qty +/−, "Adicionar ao Carrinho" → `cartStore.addItem` → push `/cart` |
| CartTable | `updateQty`, `removeItem` via Zustand |
| CheckoutStepper | 4 steps via `useState`, formulário React Hook Form, submit → step+1 |
| AccountSidebar | `useState` para tab ativa (Pedidos / Dados / Endereços / Segurança) |
| AdminSidebar | `useState` para tab ativa (Dashboard / Produtos / Pedidos / Usuários) |
| Login button | `authStore.login()` → `router.push('/account')` |
| Logout link | `authStore.logout()` → `router.push('/login')` |
| Newsletter | `useState` email, submit limpa campo (sem API) |
| RevenueChart | Barras CSS com `height` proporcional ao valor (sem biblioteca) |
| ProductsTable | Simulação CRUD: delete remove do array local via `useState` |

---

## Telas — Especificação de Conteúdo

### Home (`/`)
Seções em ordem:
1. HeroBanner — "PERFORMANCE SEM LIMITES", stats (50k+, 200+, 24/7), 2 CTAs
2. TrustBadges — 4 badges: Frete Grátis, Compra Segura, Troca Fácil, Suporte 24/7
3. CategoryGrid — 6 categorias: Rodas, Pneus, Suspensão, Freios, Performance, Motor
4. ProductSection "Produtos em Destaque" — 8 produtos em grid 4 colunas
5. PromoBanner — "ATÉ 40% OFF EM SUSPENSÃO E FREIOS"
6. ProductSection "Mais Vendidos" — 4 produtos com rank #1–#4
7. BrandsGrid — 12 marcas em grid 6 colunas
8. Newsletter — campo email + botão

### Catálogo (`/catalog`)
- Breadcrumb
- Sidebar: categorias (7 opções), faixa de preço (slider visual), marcas (checkboxes), toggle estoque
- Grid 3 colunas com ProductCard
- SortBar: contagem resultados, select ordenação, toggle grid/list
- Pagination: ‹ 1 2 3 … 26 ›

### Produto (`/products/[id]`)
- Breadcrumb
- Galeria: imagem principal + 4 thumbnails (client)
- Info: badge, título, rating + reviews, marca/SKU, preço/parcelas/PIX, select compatibilidade, qty, "Adicionar ao Carrinho" + "Comprar Agora", bullets (frete grátis, garantia, original)
- Tabs: Descrição, Especificações (tabela), Compatibilidade, Avaliações

### Carrinho (`/cart`)
- Tabela: produto (imagem+nome+marca), preço unit, qty editável, subtotal, remover
- Input cupom + botão Aplicar
- Sidebar: subtotal, frete, desconto (PROMO10), total, parcelas, "Finalizar Pedido" → `/checkout`, "Continuar Comprando" → `/catalog`

### Checkout (`/checkout`)
- Step 1 — Endereço: Nome, CPF, CEP, Logradouro, Número, Complemento, Bairro, Cidade, UF
- Step 2 — Entrega: opções de frete (Sedex, PAC)
- Step 3 — Pagamento: cartão (número, nome, validade, CVV) ou PIX (QR code placeholder)
- Step 4 — Confirmação: número do pedido, resumo, "Ver Meus Pedidos" → `/orders`
- Mini-resumo sticky à direita em todos os steps

### Login (`/login`)
- Tema light full-screen
- Logo AUTOHUB STORE
- Campos email + senha
- Botão "ENTRAR" → `authStore.login()` → `/account`
- Link "Criar conta grátis" → `/login` (placeholder)

### Minha Conta (`/account`)
- Sidebar: avatar (iniciais CS), nome, email, nav (Meus Pedidos, Dados Pessoais, Endereços, Segurança)
- Tab Pedidos: tabela com 3 pedidos mock (status badge colorido, "VER DETALHES")
- Tab Dados: form Nome, Email, Telefone, CPF, botão Salvar

### Admin (`/admin/*`)
- Sidebar escura: logo AUTOHUB ADMIN PANEL, nav 4 itens, "VER LOJA" → `/`
- Topbar: título da seção, busca, avatar AD
- Dashboard: 4 métricas, gráfico receita por mês, top produtos, tabela pedidos recentes
- Produtos: tabela com imagem/nome/categoria/preço/estoque/status + botões Editar/Excluir
- Pedidos: tabela com ID/cliente/data/status/total + botão Ver
- Usuários: tabela com nome/email/data cadastro/status + toggle ativo/inativo

---

## Fora do Escopo

- Integração real com APIs
- Autenticação JWT/cookies
- Upload de imagens (Admin)
- Página `/search` com debounce real (placeholder com mock)
- Página `/orders/[id]` detalhes do pedido
- Página `/register` e `/forgot-password`
- Responsividade mobile (desktop-first, fiel ao HTML)
