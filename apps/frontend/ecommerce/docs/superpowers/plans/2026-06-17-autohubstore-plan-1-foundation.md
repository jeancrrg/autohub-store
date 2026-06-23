# AutoHubStore — Plan 1: Foundation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bootstrap Next.js 14 project with design system, TypeScript types, mock data, Zustand stores, and UI primitives — everything Plans 2–4 depend on.

**Architecture:** Greenfield Next.js 14 App Router project. No backend; all data is static hardcoded objects. Zustand stores for cart and auth state wrapped in a client StoreProvider. Tailwind with custom color tokens and Google Fonts via `next/font`.

**Tech Stack:** Next.js 14+, TypeScript 5+, Tailwind CSS 3+, Zustand 4+, clsx, tailwind-merge

---

### Task 1.1: Bootstrap Next.js project

**Files:**
- Create: `package.json`, `tsconfig.json`, `next.config.ts`, `.eslintrc.json`, `app/layout.tsx`, `app/page.tsx` (all via `create-next-app`)

- [ ] **Step 1: Run create-next-app**

```bash
cd C:\Users\jeanc\Documents\dev\workspace-frontend\ecommerce-autohubstore
npx create-next-app@latest . --typescript --tailwind --eslint --app --no-src-dir --import-alias "@/*"
```

When prompted:
- Would you like to use Turbopack? → **No**
- Overwrite docs/ directory? → **No** (keep the specs)

- [ ] **Step 2: Install additional dependencies**

```bash
npm install zustand react-hook-form zod @hookform/resolvers clsx tailwind-merge
```

- [ ] **Step 3: Verify dev server starts**

```bash
npm run dev
```

Expected: server at `http://localhost:3000` with Next.js default page.

- [ ] **Step 4: Remove boilerplate**

Replace `app/page.tsx` with:
```typescript
export default function RootPage() {
  return <div>AutoHubStore</div>
}
```

Replace `app/globals.css` with:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

---

### Task 1.2: Configure Tailwind design system + fonts

**Files:**
- Modify: `tailwind.config.ts`
- Modify: `app/layout.tsx`

- [ ] **Step 1: Update tailwind.config.ts**

```typescript
import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: '#E10600',
        'primary-dark': '#c50500',
        dark: '#0B0B0B',
        'dark-2': '#111111',
        'dark-3': '#141414',
        'dark-border': '#1a1a1a',
        'gray-border': '#ebebeb',
        'gray-light': '#f8f8f8',
      },
      fontFamily: {
        rajdhani: ['var(--font-rajdhani)', 'sans-serif'],
        exo2: ['var(--font-exo2)', 'sans-serif'],
        inter: ['var(--font-inter)', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
export default config
```

- [ ] **Step 2: Update app/layout.tsx with fonts**

```typescript
import type { Metadata } from 'next'
import { Rajdhani, Exo_2, Inter } from 'next/font/google'
import './globals.css'

const rajdhani = Rajdhani({
  subsets: ['latin'],
  weight: ['500', '600', '700'],
  variable: '--font-rajdhani',
})

const exo2 = Exo_2({
  subsets: ['latin'],
  weight: ['400', '500', '600', '700', '800'],
  variable: '--font-exo2',
})

const inter = Inter({
  subsets: ['latin'],
  weight: ['400', '500', '600'],
  variable: '--font-inter',
})

export const metadata: Metadata = {
  title: 'AutoHubStore',
  description: 'E-commerce automotivo',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="pt-BR" className={`${rajdhani.variable} ${exo2.variable} ${inter.variable}`}>
      <body className="font-inter antialiased">{children}</body>
    </html>
  )
}
```

- [ ] **Step 3: Verify fonts load**

```bash
npm run dev
```

Open `http://localhost:3000`. Open DevTools → Elements. Confirm `<html>` has `--font-rajdhani`, `--font-exo2`, `--font-inter` CSS variables.

---

### Task 1.3: TypeScript types

**Files:**
- Create: `types/product.ts`
- Create: `types/order.ts`
- Create: `types/cart.ts`

- [ ] **Step 1: Create types/product.ts**

```typescript
export type ProductTag = 'OFERTA' | 'NOVO'

export type Product = {
  id: number
  name: string
  brand: string
  price: number
  oldPrice?: number
  tag?: ProductTag
  tagColor: string
  stars: number
  reviews: number
  installments: number
  inStock: boolean
  description: string
  specs: Record<string, string>
  category: string
  images: string[]
}

export type Category = {
  id: string
  name: string
  icon: string
  count: number
}

export type Brand = {
  id: string
  name: string
}
```

- [ ] **Step 2: Create types/order.ts**

```typescript
export type OrderStatus =
  | 'Entregue'
  | 'Em trânsito'
  | 'Processando'
  | 'Cancelado'
  | 'Aguardando pagamento'

export type Order = {
  id: string
  date: string
  status: OrderStatus
  items: string
  total: string
}

export type AdminOrder = Order & {
  client: string
  statusColor: string
  statusBg: string
}

export type MockUser = {
  name: string
  email: string
  initials: string
  phone: string
  cpf: string
}

export type AdminUser = {
  id: number
  name: string
  email: string
  initials: string
  registeredAt: string
  active: boolean
}
```

- [ ] **Step 3: Create types/cart.ts**

```typescript
import type { Product } from './product'

export type CartItem = {
  product: Product
  qty: number
}
```

- [ ] **Step 4: Verify TypeScript compiles**

```bash
npx tsc --noEmit
```

Expected: no errors.

---

### Task 1.4: Mock data files

**Files:**
- Create: `lib/data/products.ts`
- Create: `lib/data/categories.ts`
- Create: `lib/data/brands.ts`
- Create: `lib/data/orders.ts`
- Create: `lib/data/admin.ts`

- [ ] **Step 1: Create lib/data/products.ts**

```typescript
import type { Product } from '@/types/product'

export const products: Product[] = [
  {
    id: 1,
    name: 'Rodas Esportivas Aro 18 BBS RS',
    brand: 'BBS',
    price: 4899.90,
    oldPrice: 5499.90,
    tag: 'OFERTA',
    tagColor: 'bg-primary text-white',
    stars: 5,
    reviews: 128,
    installments: 12,
    inStock: true,
    category: 'Rodas',
    description: 'Rodas esportivas BBS RS aro 18 com acabamento em prata. Fabricadas em liga de alumínio de alta resistência, ideais para uso esportivo e pista.',
    specs: {
      'Aro': '18"',
      'Largura': '8.5J',
      'PCD': '5x108 / 5x112',
      'ET': '35mm',
      'Material': 'Liga de alumínio',
      'Acabamento': 'Prata polido',
    },
    images: [
      'https://placehold.co/600x600/141414/E10600?text=BBS+RS+18',
      'https://placehold.co/600x600/141414/E10600?text=BBS+RS+18+2',
      'https://placehold.co/600x600/141414/E10600?text=BBS+RS+18+3',
      'https://placehold.co/600x600/141414/E10600?text=BBS+RS+18+4',
    ],
  },
  {
    id: 2,
    name: 'Pneu Michelin Pilot Sport 4 225/45R17',
    brand: 'Michelin',
    price: 899.90,
    tag: 'NOVO',
    tagColor: 'bg-blue-600 text-white',
    stars: 5,
    reviews: 256,
    installments: 10,
    inStock: true,
    category: 'Pneus',
    description: 'Pneu de alta performance Michelin Pilot Sport 4. Excelente aderência em piso seco e molhado, ideal para carros esportivos.',
    specs: {
      'Medida': '225/45R17',
      'Índice de carga': '91Y',
      'Índice de velocidade': 'Y (300km/h)',
      'Tipo': 'Verão',
      'Fabricante': 'Michelin',
    },
    images: [
      'https://placehold.co/600x600/141414/E10600?text=Michelin+PS4',
      'https://placehold.co/600x600/141414/E10600?text=Michelin+PS4+2',
      'https://placehold.co/600x600/141414/E10600?text=Michelin+PS4+3',
      'https://placehold.co/600x600/141414/E10600?text=Michelin+PS4+4',
    ],
  },
  {
    id: 3,
    name: 'Amortecedor Bilstein B6 Sport',
    brand: 'Bilstein',
    price: 1299.90,
    oldPrice: 1499.90,
    tag: 'OFERTA',
    tagColor: 'bg-primary text-white',
    stars: 4,
    reviews: 87,
    installments: 12,
    inStock: true,
    category: 'Suspensão',
    description: 'Amortecedor Bilstein B6 Sport para uso na rua e em pistas. Tecnologia monotubo de alta performance com resposta superior.',
    specs: {
      'Tipo': 'Monotubo',
      'Posição': 'Dianteiro',
      'Diâmetro do pistão': '46mm',
      'Gás': 'Nitrogênio',
      'Aplicação': 'Esportivo',
    },
    images: [
      'https://placehold.co/600x600/141414/E10600?text=Bilstein+B6',
      'https://placehold.co/600x600/141414/E10600?text=Bilstein+B6+2',
      'https://placehold.co/600x600/141414/E10600?text=Bilstein+B6+3',
      'https://placehold.co/600x600/141414/E10600?text=Bilstein+B6+4',
    ],
  },
  {
    id: 4,
    name: 'Kit Freios Brembo Sport GT',
    brand: 'Brembo',
    price: 3299.90,
    tag: undefined,
    tagColor: '',
    stars: 5,
    reviews: 64,
    installments: 12,
    inStock: true,
    category: 'Freios',
    description: 'Kit de freios esportivos Brembo GT com discos perfurados e pastilhas de alta performance. Para uso intenso em pista e rua.',
    specs: {
      'Diâmetro do disco': '330mm',
      'Espessura': '28mm',
      'Tipo de disco': 'Ventilado perfurado',
      'Pinça': '4 pistões',
      'Material das pastilhas': 'Composto cerâmico',
    },
    images: [
      'https://placehold.co/600x600/141414/E10600?text=Brembo+GT',
      'https://placehold.co/600x600/141414/E10600?text=Brembo+GT+2',
      'https://placehold.co/600x600/141414/E10600?text=Brembo+GT+3',
      'https://placehold.co/600x600/141414/E10600?text=Brembo+GT+4',
    ],
  },
  {
    id: 5,
    name: 'Escapamento Akrapovic Evolution Titanium',
    brand: 'Akrapovic',
    price: 7899.90,
    oldPrice: 8500.00,
    tag: 'OFERTA',
    tagColor: 'bg-primary text-white',
    stars: 5,
    reviews: 42,
    installments: 12,
    inStock: false,
    category: 'Performance',
    description: 'Escapamento Akrapovic Evolution linha completa em titânio. Redução de peso, aumento de potência e som esportivo inconfundível.',
    specs: {
      'Material': 'Titânio',
      'Tipo': 'Linha completa',
      'Ganho de potência': '+8cv',
      'Redução de peso': '-3.8kg',
      'Homologação': 'Euro 6',
    },
    images: [
      'https://placehold.co/600x600/141414/E10600?text=Akrapovic',
      'https://placehold.co/600x600/141414/E10600?text=Akrapovic+2',
      'https://placehold.co/600x600/141414/E10600?text=Akrapovic+3',
      'https://placehold.co/600x600/141414/E10600?text=Akrapovic+4',
    ],
  },
  {
    id: 6,
    name: 'Filtro de Ar K&N Alto Fluxo 33-2385',
    brand: 'K&N',
    price: 349.90,
    tag: 'NOVO',
    tagColor: 'bg-blue-600 text-white',
    stars: 4,
    reviews: 183,
    installments: 6,
    inStock: true,
    category: 'Motor',
    description: 'Filtro de ar de alto fluxo K&N reutilizável. Aumenta o fluxo de ar para o motor, melhorando a performance e economia de combustível.',
    specs: {
      'Código': '33-2385',
      'Tipo': 'Reutilizável',
      'Material': 'Algodão com óleo de proteção',
      'Fluxo de ar': '+15% vs. filtro original',
      'Vida útil': '80.000 km (com limpeza)',
    },
    images: [
      'https://placehold.co/600x600/141414/E10600?text=K%26N+33-2385',
      'https://placehold.co/600x600/141414/E10600?text=K%26N+2',
      'https://placehold.co/600x600/141414/E10600?text=K%26N+3',
      'https://placehold.co/600x600/141414/E10600?text=K%26N+4',
    ],
  },
  {
    id: 7,
    name: 'Suspensão Coilover KW Variant 3',
    brand: 'KW',
    price: 8499.90,
    tag: undefined,
    tagColor: '',
    stars: 5,
    reviews: 29,
    installments: 12,
    inStock: true,
    category: 'Suspensão',
    description: 'Coilover KW Variant 3 com regulagem independente de compressão e rebound. Para uso em pista e rua com ajuste de altura.',
    specs: {
      'Tipo': 'Coilover',
      'Regulagem': 'Altura + compressão + rebound',
      'Material mola': 'Aço temperado',
      'Amortecedor': 'Aço inox',
      'Ajuste de altura': '−20 a −70mm',
    },
    images: [
      'https://placehold.co/600x600/141414/E10600?text=KW+V3',
      'https://placehold.co/600x600/141414/E10600?text=KW+V3+2',
      'https://placehold.co/600x600/141414/E10600?text=KW+V3+3',
      'https://placehold.co/600x600/141414/E10600?text=KW+V3+4',
    ],
  },
  {
    id: 8,
    name: 'Pastilhas Brembo HP2000 Dianteira',
    brand: 'Brembo',
    price: 289.90,
    oldPrice: 329.90,
    tag: 'OFERTA',
    tagColor: 'bg-primary text-white',
    stars: 4,
    reviews: 215,
    installments: 6,
    inStock: true,
    category: 'Freios',
    description: 'Pastilhas de freio Brembo HP2000 para uso esportivo. Alta resistência ao fade térmico, ideais para uso misto rua/pista.',
    specs: {
      'Posição': 'Dianteira',
      'Temperatura máxima': '650°C',
      'Tipo': 'Esportivo',
      'Material': 'Semi-metálico',
      'Aplicação': 'Rua / pista leve',
    },
    images: [
      'https://placehold.co/600x600/141414/E10600?text=Brembo+HP2000',
      'https://placehold.co/600x600/141414/E10600?text=Brembo+HP2000+2',
      'https://placehold.co/600x600/141414/E10600?text=Brembo+HP2000+3',
      'https://placehold.co/600x600/141414/E10600?text=Brembo+HP2000+4',
    ],
  },
]
```

- [ ] **Step 2: Create lib/data/categories.ts**

```typescript
import type { Category } from '@/types/product'

export const categories: Category[] = [
  { id: 'rodas', name: 'Rodas', icon: '⚙️', count: 124 },
  { id: 'pneus', name: 'Pneus', icon: '🔵', count: 89 },
  { id: 'suspensao', name: 'Suspensão', icon: '🔧', count: 67 },
  { id: 'freios', name: 'Freios', icon: '🔴', count: 53 },
  { id: 'performance', name: 'Performance', icon: '⚡', count: 41 },
  { id: 'motor', name: 'Motor', icon: '🏎️', count: 78 },
]
```

- [ ] **Step 3: Create lib/data/brands.ts**

```typescript
import type { Brand } from '@/types/product'

export const brands: Brand[] = [
  { id: 'bbs', name: 'BBS' },
  { id: 'michelin', name: 'Michelin' },
  { id: 'bilstein', name: 'Bilstein' },
  { id: 'brembo', name: 'Brembo' },
  { id: 'akrapovic', name: 'Akrapovic' },
  { id: 'kn', name: 'K&N' },
  { id: 'kw', name: 'KW' },
  { id: 'eibach', name: 'Eibach' },
  { id: 'enkei', name: 'Enkei' },
  { id: 'pirelli', name: 'Pirelli' },
  { id: 'ohlins', name: 'Öhlins' },
  { id: 'recaro', name: 'Recaro' },
]
```

- [ ] **Step 4: Create lib/data/orders.ts**

```typescript
import type { Order, AdminOrder } from '@/types/order'

export const customerOrders: Order[] = [
  {
    id: '#AH-2024-001',
    date: '15/01/2024',
    status: 'Entregue',
    items: 'Rodas BBS RS 18" x2',
    total: 'R$ 9.799,80',
  },
  {
    id: '#AH-2024-002',
    date: '28/02/2024',
    status: 'Em trânsito',
    items: 'Pneu Michelin PS4 x4',
    total: 'R$ 3.599,60',
  },
  {
    id: '#AH-2024-003',
    date: '10/03/2024',
    status: 'Processando',
    items: 'Kit Freios Brembo GT x1',
    total: 'R$ 3.299,90',
  },
]

export const adminOrders: AdminOrder[] = [
  {
    id: '#AH-2024-001',
    client: 'Carlos Silva',
    date: '15/01/2024',
    status: 'Entregue',
    items: 'Rodas BBS RS 18"',
    total: 'R$ 9.799,80',
    statusColor: 'text-green-400',
    statusBg: 'bg-green-400/10',
  },
  {
    id: '#AH-2024-002',
    client: 'Ana Souza',
    date: '28/02/2024',
    status: 'Em trânsito',
    items: 'Pneu Michelin PS4',
    total: 'R$ 3.599,60',
    statusColor: 'text-blue-400',
    statusBg: 'bg-blue-400/10',
  },
  {
    id: '#AH-2024-003',
    client: 'Pedro Costa',
    date: '10/03/2024',
    status: 'Processando',
    items: 'Kit Freios Brembo GT',
    total: 'R$ 3.299,90',
    statusColor: 'text-yellow-400',
    statusBg: 'bg-yellow-400/10',
  },
  {
    id: '#AH-2024-004',
    client: 'Mariana Lima',
    date: '22/03/2024',
    status: 'Cancelado',
    items: 'Amortecedor Bilstein B6',
    total: 'R$ 1.299,90',
    statusColor: 'text-red-400',
    statusBg: 'bg-red-400/10',
  },
  {
    id: '#AH-2024-005',
    client: 'Rafael Oliveira',
    date: '05/04/2024',
    status: 'Aguardando pagamento',
    items: 'Suspensão KW V3',
    total: 'R$ 8.499,90',
    statusColor: 'text-orange-400',
    statusBg: 'bg-orange-400/10',
  },
]
```

- [ ] **Step 5: Create lib/data/admin.ts**

```typescript
import type { AdminUser } from '@/types/order'

export type Metric = {
  label: string
  value: string
  change: string
  changePositive: boolean
  icon: string
}

export type SalesBar = {
  month: string
  label: string
  value: number
}

export type TopProduct = {
  rank: number
  name: string
  brand: string
  revenue: string
  sales: number
}

export const MAX_SALES_VALUE = 120000

export const metrics: Metric[] = [
  { label: 'Receita Total', value: 'R$ 284.590', change: '+12.5%', changePositive: true, icon: '💰' },
  { label: 'Pedidos', value: '1.284', change: '+8.2%', changePositive: true, icon: '📦' },
  { label: 'Clientes', value: '3.847', change: '+15.3%', changePositive: true, icon: '👥' },
  { label: 'Ticket Médio', value: 'R$ 1.247', change: '-2.1%', changePositive: false, icon: '🎯' },
]

export const salesBars: SalesBar[] = [
  { month: 'Jan', label: 'R$ 38k', value: 38000 },
  { month: 'Fev', label: 'R$ 52k', value: 52000 },
  { month: 'Mar', label: 'R$ 45k', value: 45000 },
  { month: 'Abr', label: 'R$ 61k', value: 61000 },
  { month: 'Mai', label: 'R$ 78k', value: 78000 },
  { month: 'Jun', label: 'R$ 92k', value: 92000 },
  { month: 'Jul', label: 'R$ 88k', value: 88000 },
  { month: 'Ago', label: 'R$ 105k', value: 105000 },
  { month: 'Set', label: 'R$ 98k', value: 98000 },
  { month: 'Out', label: 'R$ 115k', value: 115000 },
  { month: 'Nov', label: 'R$ 120k', value: 120000 },
  { month: 'Dez', label: 'R$ 87k', value: 87000 },
]

export const topProducts: TopProduct[] = [
  { rank: 1, name: 'Rodas BBS RS Aro 18', brand: 'BBS', revenue: 'R$ 48.990', sales: 10 },
  { rank: 2, name: 'Suspensão KW Variant 3', brand: 'KW', revenue: 'R$ 33.999', sales: 4 },
  { rank: 3, name: 'Kit Freios Brembo GT', brand: 'Brembo', revenue: 'R$ 29.699', sales: 9 },
  { rank: 4, name: 'Escapamento Akrapovic', brand: 'Akrapovic', revenue: 'R$ 23.699', sales: 3 },
  { rank: 5, name: 'Pneu Michelin PS4', brand: 'Michelin', revenue: 'R$ 17.998', sales: 20 },
]

export const adminUsers: AdminUser[] = [
  { id: 1, name: 'Carlos Silva', email: 'carlos@email.com', initials: 'CS', registeredAt: '10/01/2024', active: true },
  { id: 2, name: 'Ana Souza', email: 'ana@email.com', initials: 'AS', registeredAt: '15/01/2024', active: true },
  { id: 3, name: 'Pedro Costa', email: 'pedro@email.com', initials: 'PC', registeredAt: '22/01/2024', active: false },
  { id: 4, name: 'Mariana Lima', email: 'mariana@email.com', initials: 'ML', registeredAt: '05/02/2024', active: true },
  { id: 5, name: 'Rafael Oliveira', email: 'rafael@email.com', initials: 'RO', registeredAt: '12/02/2024', active: true },
]
```

- [ ] **Step 6: Verify TypeScript compiles**

```bash
npx tsc --noEmit
```

Expected: no errors.

---

### Task 1.5: Zustand stores

**Files:**
- Create: `store/cartStore.ts`
- Create: `store/authStore.ts`

- [ ] **Step 1: Write failing test for cartStore**

Create `store/__tests__/cartStore.test.ts`:
```typescript
import { act, renderHook } from '@testing-library/react'
import { useCartStore } from '../cartStore'
import type { Product } from '@/types/product'

const mockProduct: Product = {
  id: 1, name: 'Test Product', brand: 'Brand', price: 100,
  tagColor: '', stars: 5, reviews: 10, installments: 3,
  inStock: true, description: '', specs: {}, category: 'Rodas',
  images: [],
}

describe('cartStore', () => {
  beforeEach(() => {
    useCartStore.setState({ items: [] })
  })

  it('starts empty', () => {
    const { result } = renderHook(() => useCartStore())
    expect(result.current.items).toHaveLength(0)
    expect(result.current.count).toBe(0)
    expect(result.current.total).toBe(0)
  })

  it('addItem adds new item', () => {
    const { result } = renderHook(() => useCartStore())
    act(() => result.current.addItem(mockProduct, 2))
    expect(result.current.items).toHaveLength(1)
    expect(result.current.items[0].qty).toBe(2)
    expect(result.current.count).toBe(2)
    expect(result.current.total).toBe(200)
  })

  it('addItem increments qty for existing item', () => {
    const { result } = renderHook(() => useCartStore())
    act(() => result.current.addItem(mockProduct, 1))
    act(() => result.current.addItem(mockProduct, 2))
    expect(result.current.items).toHaveLength(1)
    expect(result.current.items[0].qty).toBe(3)
  })

  it('removeItem removes by id', () => {
    const { result } = renderHook(() => useCartStore())
    act(() => result.current.addItem(mockProduct, 1))
    act(() => result.current.removeItem(1))
    expect(result.current.items).toHaveLength(0)
  })

  it('updateQty updates item quantity', () => {
    const { result } = renderHook(() => useCartStore())
    act(() => result.current.addItem(mockProduct, 1))
    act(() => result.current.updateQty(1, 5))
    expect(result.current.items[0].qty).toBe(5)
    expect(result.current.count).toBe(5)
  })

  it('clearCart empties the cart', () => {
    const { result } = renderHook(() => useCartStore())
    act(() => result.current.addItem(mockProduct, 3))
    act(() => result.current.clearCart())
    expect(result.current.items).toHaveLength(0)
  })
})
```

- [ ] **Step 2: Install testing dependencies and run failing test**

```bash
npm install -D @testing-library/react @testing-library/jest-dom jest jest-environment-jsdom @types/jest ts-jest
```

Add to `package.json` scripts:
```json
"test": "jest"
```

Create `jest.config.ts`:
```typescript
import type { Config } from 'jest'

const config: Config = {
  testEnvironment: 'jsdom',
  transform: { '^.+\\.tsx?$': ['ts-jest', { tsconfig: { jsx: 'react-jsx' } }] },
  moduleNameMapper: { '^@/(.*)$': '<rootDir>/$1' },
  setupFilesAfterFramework: ['@testing-library/jest-dom'],
}

export default config
```

```bash
npm test -- store/__tests__/cartStore.test.ts
```

Expected: FAIL — `useCartStore` not found.

- [ ] **Step 3: Create store/cartStore.ts**

```typescript
import { create } from 'zustand'
import type { Product } from '@/types/product'
import type { CartItem } from '@/types/cart'

type CartStore = {
  items: CartItem[]
  addItem: (product: Product, qty: number) => void
  removeItem: (id: number) => void
  updateQty: (id: number, qty: number) => void
  clearCart: () => void
  total: number
  count: number
}

export const useCartStore = create<CartStore>((set, get) => ({
  items: [],

  addItem: (product, qty) => {
    const existing = get().items.find((i) => i.product.id === product.id)
    if (existing) {
      set((s) => ({
        items: s.items.map((i) =>
          i.product.id === product.id ? { ...i, qty: i.qty + qty } : i
        ),
      }))
    } else {
      set((s) => ({ items: [...s.items, { product, qty }] }))
    }
  },

  removeItem: (id) =>
    set((s) => ({ items: s.items.filter((i) => i.product.id !== id) })),

  updateQty: (id, qty) =>
    set((s) => ({
      items: s.items.map((i) => (i.product.id === id ? { ...i, qty } : i)),
    })),

  clearCart: () => set({ items: [] }),

  get total() {
    return get().items.reduce((sum, i) => sum + i.product.price * i.qty, 0)
  },

  get count() {
    return get().items.reduce((sum, i) => sum + i.qty, 0)
  },
}))
```

- [ ] **Step 4: Run test — verify passes**

```bash
npm test -- store/__tests__/cartStore.test.ts
```

Expected: PASS (all 6 tests).

- [ ] **Step 5: Create store/authStore.ts**

```typescript
import { create } from 'zustand'
import type { MockUser } from '@/types/order'

const MOCK_USER: MockUser = {
  name: 'Carlos Silva',
  email: 'carlos@email.com',
  initials: 'CS',
  phone: '(11) 99999-9999',
  cpf: '123.456.789-00',
}

type AuthStore = {
  isLoggedIn: boolean
  user: MockUser | null
  login: () => void
  logout: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  isLoggedIn: false,
  user: null,
  login: () => set({ isLoggedIn: true, user: MOCK_USER }),
  logout: () => set({ isLoggedIn: false, user: null }),
}))
```

- [ ] **Step 6: Write and run authStore test**

Create `store/__tests__/authStore.test.ts`:
```typescript
import { act, renderHook } from '@testing-library/react'
import { useAuthStore } from '../authStore'

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.setState({ isLoggedIn: false, user: null })
  })

  it('starts logged out', () => {
    const { result } = renderHook(() => useAuthStore())
    expect(result.current.isLoggedIn).toBe(false)
    expect(result.current.user).toBeNull()
  })

  it('login sets isLoggedIn and user', () => {
    const { result } = renderHook(() => useAuthStore())
    act(() => result.current.login())
    expect(result.current.isLoggedIn).toBe(true)
    expect(result.current.user?.name).toBe('Carlos Silva')
  })

  it('logout clears state', () => {
    const { result } = renderHook(() => useAuthStore())
    act(() => result.current.login())
    act(() => result.current.logout())
    expect(result.current.isLoggedIn).toBe(false)
    expect(result.current.user).toBeNull()
  })
})
```

```bash
npm test
```

Expected: PASS (all 9 tests across both store files).

---

### Task 1.6: StoreProvider client component

**Files:**
- Create: `components/providers/StoreProvider.tsx`
- Modify: `app/layout.tsx`

Zustand stores in Next.js App Router work without a provider for client components, but a provider wrapper ensures stores reset correctly on navigation.

- [ ] **Step 1: Create components/providers/StoreProvider.tsx**

```typescript
'use client'

export function StoreProvider({ children }: { children: React.ReactNode }) {
  return <>{children}</>
}
```

- [ ] **Step 2: Wrap root layout with StoreProvider**

Update `app/layout.tsx`:
```typescript
import type { Metadata } from 'next'
import { Rajdhani, Exo_2, Inter } from 'next/font/google'
import { StoreProvider } from '@/components/providers/StoreProvider'
import './globals.css'

const rajdhani = Rajdhani({
  subsets: ['latin'],
  weight: ['500', '600', '700'],
  variable: '--font-rajdhani',
})

const exo2 = Exo_2({
  subsets: ['latin'],
  weight: ['400', '500', '600', '700', '800'],
  variable: '--font-exo2',
})

const inter = Inter({
  subsets: ['latin'],
  weight: ['400', '500', '600'],
  variable: '--font-inter',
})

export const metadata: Metadata = {
  title: 'AutoHubStore',
  description: 'E-commerce automotivo',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="pt-BR" className={`${rajdhani.variable} ${exo2.variable} ${inter.variable}`}>
      <body className="font-inter antialiased">
        <StoreProvider>{children}</StoreProvider>
      </body>
    </html>
  )
}
```

---

### Task 1.7: UI primitives

**Files:**
- Create: `lib/utils.ts`
- Create: `components/ui/Button.tsx`
- Create: `components/ui/Input.tsx`
- Create: `components/ui/Badge.tsx`

- [ ] **Step 1: Create lib/utils.ts**

```typescript
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

- [ ] **Step 2: Create components/ui/Button.tsx**

```typescript
import { cn } from '@/lib/utils'

type ButtonVariant = 'primary' | 'outline' | 'ghost'
type ButtonSize = 'sm' | 'md' | 'lg'

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  size?: ButtonSize
}

const variants: Record<ButtonVariant, string> = {
  primary: 'bg-primary hover:bg-primary-dark text-white',
  outline: 'border border-gray-border hover:border-dark text-dark',
  ghost: 'text-gray-500 hover:text-dark',
}

const sizes: Record<ButtonSize, string> = {
  sm: 'px-4 py-2 text-sm',
  md: 'px-6 py-3 text-sm',
  lg: 'px-8 py-4 text-base',
}

export function Button({
  variant = 'primary',
  size = 'md',
  className,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={cn(
        'font-rajdhani font-600 tracking-wider transition-colors disabled:opacity-50 disabled:cursor-not-allowed',
        variants[variant],
        sizes[size],
        className
      )}
      {...props}
    >
      {children}
    </button>
  )
}
```

- [ ] **Step 3: Create components/ui/Input.tsx**

```typescript
import { cn } from '@/lib/utils'
import { forwardRef } from 'react'

type InputProps = React.InputHTMLAttributes<HTMLInputElement> & {
  label?: string
  error?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, className, ...props }, ref) => {
    return (
      <div className="flex flex-col gap-1">
        {label && (
          <label className="font-inter text-sm font-500 text-dark">{label}</label>
        )}
        <input
          ref={ref}
          className={cn(
            'w-full border border-gray-border px-4 py-3 font-inter text-sm text-dark placeholder:text-gray-400 focus:outline-none focus:border-dark transition-colors',
            error && 'border-primary',
            className
          )}
          {...props}
        />
        {error && (
          <span className="font-inter text-xs text-primary">{error}</span>
        )}
      </div>
    )
  }
)

Input.displayName = 'Input'
```

- [ ] **Step 4: Create components/ui/Badge.tsx**

```typescript
import { cn } from '@/lib/utils'

type BadgeProps = {
  children: React.ReactNode
  className?: string
  bgClass?: string
}

export function Badge({ children, className, bgClass }: BadgeProps) {
  return (
    <span
      className={cn(
        'font-rajdhani font-600 text-xs tracking-wider px-2 py-0.5',
        bgClass,
        className
      )}
    >
      {children}
    </span>
  )
}
```

- [ ] **Step 5: Verify build**

```bash
npx tsc --noEmit
npm run build
```

Expected: both pass with no errors.

---

## Verification

After Plan 1 is complete, run:

```bash
npm test          # All store tests pass
npm run build     # Build succeeds with no TS errors
npm run dev       # Dev server starts at http://localhost:3000
```

Confirm in browser:
- `http://localhost:3000` renders "AutoHubStore" text
- Browser DevTools → Network → Fonts shows Rajdhani, Exo 2, and Inter loading

**Next:** Execute Plan 2 (Public Store) — depends on all Foundation artifacts from this plan.
