# AutoHubStore — Plan 4: Admin Panel

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Prerequisite:** Plans 1 (Foundation), 2 (Public Store), and 3 (Customer Area) must be complete.

**Goal:** Build the admin panel — dark-themed layout with sidebar, dashboard with CSS-only revenue chart, and CRUD-style tables for products, orders, and users.

**Architecture:** Route group `(admin)` with its own layout — no Header/Footer from the public store. `AdminSidebar` is a client component using `usePathname`. Dashboard components are Server Components fed from `lib/data/admin.ts`. `ProductsTable` and `UsersTable` are Client Components owning local `useState` for mock CRUD/toggle.

**Tech Stack:** Next.js 14 App Router, Tailwind CSS, Zustand-free (admin has no cart/auth dependency)

---

### Task 4.1: Admin layout + AdminSidebar

**Files:**
- Create: `components/layout/AdminSidebar.tsx`
- Create: `app/(admin)/layout.tsx`

- [ ] **Step 1: Create components/layout/AdminSidebar.tsx (Client)**

```typescript
'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'

const NAV_ITEMS = [
  { href: '/admin/dashboard', icon: '📊', label: 'Dashboard' },
  { href: '/admin/products', icon: '📦', label: 'Produtos' },
  { href: '/admin/orders', icon: '🛒', label: 'Pedidos' },
  { href: '/admin/users', icon: '👥', label: 'Usuários' },
]

export function AdminSidebar() {
  const pathname = usePathname()

  return (
    <aside className="w-56 bg-[#080808] min-h-screen flex flex-col flex-shrink-0">
      {/* Logo */}
      <div className="p-6 border-b border-dark-border">
        <p className="font-rajdhani font-700 text-white text-sm tracking-widest">AUTOHUB</p>
        <p className="font-rajdhani font-600 text-primary text-xs tracking-widest">ADMIN PANEL</p>
      </div>

      {/* Nav */}
      <nav className="flex-1 p-4 space-y-1">
        {NAV_ITEMS.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={`flex items-center gap-3 px-4 py-3 font-inter text-sm transition-colors ${
              pathname === item.href
                ? 'bg-primary text-white'
                : 'text-gray-400 hover:text-white hover:bg-dark-border'
            }`}
          >
            <span>{item.icon}</span>
            {item.label}
          </Link>
        ))}
      </nav>

      {/* Footer */}
      <div className="p-4 border-t border-dark-border">
        <Link
          href="/"
          className="flex items-center gap-2 font-inter text-sm text-gray-400 hover:text-primary transition-colors"
        >
          ← VER LOJA
        </Link>
      </div>
    </aside>
  )
}
```

- [ ] **Step 2: Create app/(admin)/layout.tsx**

```typescript
import { AdminSidebar } from '@/components/layout/AdminSidebar'

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="flex min-h-screen bg-dark">
      <AdminSidebar />

      <div className="flex-1 flex flex-col">
        {/* Topbar */}
        <header className="bg-dark-2 border-b border-dark-border px-8 py-4 flex items-center justify-between">
          <div className="flex-1 max-w-xs">
            <input
              type="text"
              placeholder="Buscar..."
              className="w-full bg-dark-3 border border-dark-border text-white font-inter text-sm px-4 py-2 placeholder:text-gray-600 focus:outline-none focus:border-primary transition-colors"
            />
          </div>

          {/* Admin avatar */}
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-primary rounded-full flex items-center justify-center text-white font-rajdhani font-700 text-sm">
              AD
            </div>
            <span className="font-inter text-sm text-gray-400">Admin</span>
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 p-8">{children}</main>
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Create app/(admin)/admin/page.tsx (redirect)**

```typescript
import { redirect } from 'next/navigation'

export default function AdminIndexPage() {
  redirect('/admin/dashboard')
}
```

- [ ] **Step 4: Verify layout**

```bash
npm run dev
```

Open `http://localhost:3000/admin`. Confirm redirect to `/admin/dashboard`. Confirm dark sidebar with AUTOHUB ADMIN PANEL logo, 4 nav items, topbar with search and "AD" avatar.

---

### Task 4.2: Dashboard page

**Files:**
- Create: `components/admin/MetricsCards.tsx`
- Create: `components/admin/RevenueChart.tsx`
- Create: `components/admin/TopProducts.tsx`
- Create: `components/admin/RecentOrders.tsx`
- Create: `app/(admin)/admin/dashboard/page.tsx`

- [ ] **Step 1: Create components/admin/MetricsCards.tsx (Server)**

```typescript
import { metrics } from '@/lib/data/admin'

export function MetricsCards() {
  return (
    <div className="grid grid-cols-4 gap-4 mb-8">
      {metrics.map((m) => (
        <div key={m.label} className="bg-dark-2 border border-dark-border p-6">
          <div className="flex items-center justify-between mb-4">
            <span className="text-2xl">{m.icon}</span>
            <span
              className={`font-inter text-xs font-500 px-2 py-1 rounded ${
                m.changePositive
                  ? 'text-green-400 bg-green-400/10'
                  : 'text-red-400 bg-red-400/10'
              }`}
            >
              {m.change}
            </span>
          </div>
          <p className="font-rajdhani font-700 text-2xl text-white">{m.value}</p>
          <p className="font-inter text-xs text-gray-500 mt-1">{m.label}</p>
        </div>
      ))}
    </div>
  )
}
```

- [ ] **Step 2: Create components/admin/RevenueChart.tsx (Server)**

Pure CSS bar chart — no charting library. Bars grow upward from baseline using `flex items-end` parent with explicit height.

```typescript
import { salesBars, MAX_SALES_VALUE } from '@/lib/data/admin'

export function RevenueChart() {
  return (
    <div className="bg-dark-2 border border-dark-border p-6 mb-8">
      <h3 className="font-rajdhani font-600 text-white mb-6 tracking-wider">RECEITA POR MÊS</h3>
      <div className="flex items-end gap-2 h-48">
        {salesBars.map((bar) => {
          const heightPercent = Math.round((bar.value / MAX_SALES_VALUE) * 100)
          return (
            <div key={bar.month} className="flex-1 flex flex-col items-center gap-1">
              <span className="font-inter text-xs text-gray-500 mb-1">{bar.label}</span>
              <div
                className="w-full bg-primary hover:bg-primary-dark transition-colors cursor-pointer"
                style={{ height: `${heightPercent}%` }}
                title={`${bar.month}: ${bar.label}`}
              />
              <span className="font-inter text-xs text-gray-500 mt-1">{bar.month}</span>
            </div>
          )
        })}
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Create components/admin/TopProducts.tsx (Server)**

```typescript
import { topProducts } from '@/lib/data/admin'

export function TopProducts() {
  return (
    <div className="bg-dark-2 border border-dark-border p-6">
      <h3 className="font-rajdhani font-600 text-white mb-6 tracking-wider">TOP PRODUTOS</h3>
      <div className="space-y-4">
        {topProducts.map((p) => (
          <div key={p.rank} className="flex items-center gap-4">
            <span className="font-rajdhani font-700 text-primary text-lg w-6">#{p.rank}</span>
            <div className="flex-1">
              <p className="font-inter text-sm text-white">{p.name}</p>
              <p className="font-inter text-xs text-gray-500">{p.brand}</p>
            </div>
            <div className="text-right">
              <p className="font-rajdhani font-600 text-white text-sm">{p.revenue}</p>
              <p className="font-inter text-xs text-gray-500">{p.sales} vendas</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Create components/admin/RecentOrders.tsx (Server)**

```typescript
import { adminOrders } from '@/lib/data/orders'

export function RecentOrders() {
  const recent = adminOrders.slice(0, 4)

  return (
    <div className="bg-dark-2 border border-dark-border p-6">
      <h3 className="font-rajdhani font-600 text-white mb-6 tracking-wider">PEDIDOS RECENTES</h3>
      <div className="space-y-3">
        {recent.map((order) => (
          <div
            key={order.id}
            className="flex items-center gap-4 py-2 border-b border-dark-border last:border-0"
          >
            <span className="font-rajdhani font-600 text-white text-sm w-28">{order.id}</span>
            <span className="font-inter text-sm text-gray-400 flex-1">{order.client}</span>
            <span className={`font-inter text-xs px-2 py-1 ${order.statusBg} ${order.statusColor}`}>
              {order.status}
            </span>
            <span className="font-rajdhani font-600 text-white text-sm">{order.total}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
```

- [ ] **Step 5: Create app/(admin)/admin/dashboard/page.tsx (Server)**

```typescript
import { MetricsCards } from '@/components/admin/MetricsCards'
import { RevenueChart } from '@/components/admin/RevenueChart'
import { TopProducts } from '@/components/admin/TopProducts'
import { RecentOrders } from '@/components/admin/RecentOrders'

export default function DashboardPage() {
  return (
    <div>
      <h1 className="font-rajdhani font-700 text-2xl text-white mb-8 tracking-wider">DASHBOARD</h1>
      <MetricsCards />
      <RevenueChart />
      <div className="grid grid-cols-2 gap-8">
        <TopProducts />
        <RecentOrders />
      </div>
    </div>
  )
}
```

- [ ] **Step 6: Verify dashboard**

```bash
npm run dev
```

Open `http://localhost:3000/admin/dashboard`. Confirm:
- 4 metric cards with icons, values, and green/red percentage badges
- CSS bar chart: 12 bars of proportional heights (November bar tallest at 100%)
- Top 5 products with rank numbers in red
- 4 recent orders with colored status badges

---

### Task 4.3: Admin Products table (mock delete)

**Files:**
- Create: `components/admin/ProductsTable.tsx`
- Create: `app/(admin)/admin/products/page.tsx`

- [ ] **Step 1: Create components/admin/ProductsTable.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'
import { products as initialProducts } from '@/lib/data/products'
import type { Product } from '@/types/product'

export function ProductsTable() {
  const [rows, setRows] = useState<Product[]>(initialProducts)

  function handleDelete(id: number) {
    setRows((prev) => prev.filter((p) => p.id !== id))
  }

  return (
    <div className="bg-dark-2 border border-dark-border">
      {/* Header */}
      <div className="grid grid-cols-[auto_2fr_1fr_1fr_1fr_1fr_auto] gap-4 px-4 py-3 border-b border-dark-border">
        {['IMG', 'PRODUTO', 'CATEGORIA', 'PREÇO', 'ESTOQUE', 'STATUS', 'AÇÕES'].map((h) => (
          <span key={h} className="font-rajdhani font-600 text-xs text-gray-500 tracking-wider">{h}</span>
        ))}
      </div>

      {rows.map((product) => (
        <div
          key={product.id}
          className="grid grid-cols-[auto_2fr_1fr_1fr_1fr_1fr_auto] gap-4 px-4 py-4 border-b border-dark-border items-center last:border-0"
        >
          {/* Image placeholder */}
          <div className="w-10 h-10 bg-dark-3 border border-dark-border flex items-center justify-center text-lg">
            🔧
          </div>

          {/* Name + brand */}
          <div>
            <p className="font-inter text-sm text-white">{product.name}</p>
            <p className="font-inter text-xs text-gray-500">{product.brand}</p>
          </div>

          {/* Category */}
          <span className="font-inter text-sm text-gray-400">{product.category}</span>

          {/* Price */}
          <span className="font-rajdhani font-600 text-white text-sm">
            R$ {product.price.toFixed(2).replace('.', ',')}
          </span>

          {/* Stock */}
          <span className={`font-inter text-xs ${product.inStock ? 'text-green-400' : 'text-red-400'}`}>
            {product.inStock ? 'Em estoque' : 'Esgotado'}
          </span>

          {/* Status badge */}
          <span
            className={`font-rajdhani font-600 text-xs px-2 py-0.5 tracking-wider w-fit ${
              product.inStock
                ? 'text-green-400 bg-green-400/10'
                : 'text-red-400 bg-red-400/10'
            }`}
          >
            {product.inStock ? 'ATIVO' : 'INATIVO'}
          </span>

          {/* Actions */}
          <div className="flex items-center gap-2">
            <button className="font-inter text-xs text-gray-400 hover:text-white transition-colors px-2 py-1 border border-dark-border hover:border-gray-600">
              Editar
            </button>
            <button
              onClick={() => handleDelete(product.id)}
              className="font-inter text-xs text-red-400 hover:text-red-300 transition-colors px-2 py-1 border border-dark-border hover:border-red-400"
            >
              Excluir
            </button>
          </div>
        </div>
      ))}

      {rows.length === 0 && (
        <div className="px-4 py-12 text-center">
          <p className="font-inter text-sm text-gray-500">Nenhum produto cadastrado.</p>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Create app/(admin)/admin/products/page.tsx**

```typescript
import { ProductsTable } from '@/components/admin/ProductsTable'

export default function AdminProductsPage() {
  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="font-rajdhani font-700 text-2xl text-white tracking-wider">PRODUTOS</h1>
        <button className="bg-primary hover:bg-primary-dark text-white font-rajdhani font-600 px-6 py-2 text-sm tracking-wider transition-colors">
          + NOVO PRODUTO
        </button>
      </div>
      <ProductsTable />
    </div>
  )
}
```

- [ ] **Step 3: Verify products table**

```bash
npm run dev
```

Open `http://localhost:3000/admin/products`. Confirm:
- 8 product rows with emoji placeholder, name, brand, category, price, stock, status badge
- Click "Excluir" on product 1 → row disappears from table without page reload
- Delete all rows → "Nenhum produto cadastrado." message appears

---

### Task 4.4: Admin Orders table

**Files:**
- Create: `components/admin/OrdersTable.tsx`
- Create: `app/(admin)/admin/orders/page.tsx`

- [ ] **Step 1: Create components/admin/OrdersTable.tsx (Server)**

```typescript
import { adminOrders } from '@/lib/data/orders'

export function OrdersTable() {
  return (
    <div className="bg-dark-2 border border-dark-border">
      {/* Header */}
      <div className="grid grid-cols-[1fr_1fr_1fr_1fr_1fr_auto] gap-4 px-4 py-3 border-b border-dark-border">
        {['PEDIDO', 'CLIENTE', 'DATA', 'STATUS', 'TOTAL', 'AÇÃO'].map((h) => (
          <span key={h} className="font-rajdhani font-600 text-xs text-gray-500 tracking-wider">{h}</span>
        ))}
      </div>

      {adminOrders.map((order) => (
        <div
          key={order.id}
          className="grid grid-cols-[1fr_1fr_1fr_1fr_1fr_auto] gap-4 px-4 py-4 border-b border-dark-border items-center last:border-0"
        >
          <span className="font-rajdhani font-600 text-white text-sm">{order.id}</span>
          <span className="font-inter text-sm text-gray-300">{order.client}</span>
          <span className="font-inter text-sm text-gray-400">{order.date}</span>
          <span className={`font-inter text-xs px-2 py-1 w-fit ${order.statusBg} ${order.statusColor}`}>
            {order.status}
          </span>
          <span className="font-rajdhani font-600 text-white text-sm">{order.total}</span>
          <button className="font-inter text-xs text-primary hover:text-primary-dark transition-colors">
            Ver
          </button>
        </div>
      ))}
    </div>
  )
}
```

- [ ] **Step 2: Create app/(admin)/admin/orders/page.tsx**

```typescript
import { OrdersTable } from '@/components/admin/OrdersTable'

export default function AdminOrdersPage() {
  return (
    <div>
      <h1 className="font-rajdhani font-700 text-2xl text-white mb-8 tracking-wider">PEDIDOS</h1>
      <OrdersTable />
    </div>
  )
}
```

- [ ] **Step 3: Verify orders table**

```bash
npm run dev
```

Open `http://localhost:3000/admin/orders`. Confirm 5 admin orders with correct colored status badges (green/blue/yellow/red/orange).

---

### Task 4.5: Admin Users table (active toggle)

**Files:**
- Create: `components/admin/UsersTable.tsx`
- Create: `app/(admin)/admin/users/page.tsx`

- [ ] **Step 1: Create components/admin/UsersTable.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'
import { adminUsers, type AdminUser } from '@/lib/data/admin'

export function UsersTable() {
  const [users, setUsers] = useState<AdminUser[]>(adminUsers)

  function toggleActive(id: number) {
    setUsers((prev) =>
      prev.map((u) => (u.id === id ? { ...u, active: !u.active } : u))
    )
  }

  return (
    <div className="bg-dark-2 border border-dark-border">
      {/* Header */}
      <div className="grid grid-cols-[auto_2fr_2fr_1fr_1fr_auto] gap-4 px-4 py-3 border-b border-dark-border">
        {['', 'NOME', 'E-MAIL', 'CADASTRO', 'STATUS', 'ATIVO'].map((h) => (
          <span key={h} className="font-rajdhani font-600 text-xs text-gray-500 tracking-wider">{h}</span>
        ))}
      </div>

      {users.map((user) => (
        <div
          key={user.id}
          className="grid grid-cols-[auto_2fr_2fr_1fr_1fr_auto] gap-4 px-4 py-4 border-b border-dark-border items-center last:border-0"
        >
          {/* Avatar */}
          <div className="w-9 h-9 bg-primary rounded-full flex items-center justify-center text-white font-rajdhani font-700 text-sm">
            {user.initials}
          </div>

          <span className="font-inter text-sm text-white">{user.name}</span>
          <span className="font-inter text-sm text-gray-400">{user.email}</span>
          <span className="font-inter text-sm text-gray-500">{user.registeredAt}</span>

          {/* Status badge */}
          <span
            className={`font-inter text-xs px-2 py-1 w-fit ${
              user.active
                ? 'text-green-400 bg-green-400/10'
                : 'text-red-400 bg-red-400/10'
            }`}
          >
            {user.active ? 'Ativo' : 'Inativo'}
          </span>

          {/* Toggle switch */}
          <button
            onClick={() => toggleActive(user.id)}
            className={`relative w-10 h-5 rounded-full transition-colors ${
              user.active ? 'bg-primary' : 'bg-dark-border'
            }`}
            aria-label={user.active ? 'Desativar usuário' : 'Ativar usuário'}
          >
            <span
              className={`absolute top-0.5 w-4 h-4 bg-white rounded-full transition-transform ${
                user.active ? 'translate-x-5' : 'translate-x-0.5'
              }`}
            />
          </button>
        </div>
      ))}
    </div>
  )
}
```

- [ ] **Step 2: Create app/(admin)/admin/users/page.tsx**

```typescript
import { UsersTable } from '@/components/admin/UsersTable'

export default function AdminUsersPage() {
  return (
    <div>
      <h1 className="font-rajdhani font-700 text-2xl text-white mb-8 tracking-wider">USUÁRIOS</h1>
      <UsersTable />
    </div>
  )
}
```

- [ ] **Step 3: Verify users table**

```bash
npm run dev
```

Open `http://localhost:3000/admin/users`. Confirm:
- 5 users with initials avatars in red circles
- User "Pedro Costa" shows Inativo status badge (red) and toggle in OFF position
- Click toggle for Pedro → badge changes to "Ativo", toggle moves to right
- Click again → reverts to Inativo

---

## Verification

After Plan 4 is complete, run full build + manual check:

```bash
npm run build
```

Expected: build completes with no TypeScript errors. All 13 routes pre-rendered:
- `/` `/catalog` `/products/[id]` `/login` `/search`
- `/cart` `/checkout` `/account` `/orders`
- `/admin` `/admin/dashboard` `/admin/products` `/admin/orders` `/admin/users`

Manual admin flow:
1. `/admin` → redirects to `/admin/dashboard`
2. Dashboard: 4 metric cards, 12 CSS bars (November tallest), 5 top products, 4 recent orders
3. Sidebar active state highlights current page on each navigation
4. `/admin/products`: delete rows → table shrinks, empty message at 0 rows
5. `/admin/orders`: 5 rows with correct status color coding
6. `/admin/users`: toggle Pedro Costa → Ativo; toggle back → Inativo
7. "VER LOJA" sidebar link → returns to `/`

---

## Cross-Plan Notes

- `RevenueChart` uses `style={{ height: '${percent}%' }}` inline. The parent `div` must have explicit height (`h-48`); percentage heights on flex children need a fixed-height parent.
- `AdminSidebar` uses `usePathname()` — it's a Client Component. The rest of the admin dashboard components are Server Components.
- `ProductsTable` and `UsersTable` own local `useState` — mutations are ephemeral and reset on page refresh. This is intentional per the spec.
- The `(admin)` route group has its own layout with no `<Header>` or `<Footer>`. The `bg-dark` on `<body>` from root layout does not conflict because admin sets `bg-dark` on its own flex container.
