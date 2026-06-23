# AutoHubStore — Plan 2: Public Store

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Prerequisite:** Plan 1 (Foundation) must be complete.

**Goal:** Build the public-facing store pages — Header/Footer shell, Home (8 sections), Catalog with filters, Product detail, Login, and Search placeholder.

**Architecture:** Next.js App Router route group `(public)` with shared Header+Footer layout. All components are Server Components by default except those with interactivity (`'use client'`). Data comes from `lib/data/` static mock files.

**Tech Stack:** Next.js 14 App Router, Tailwind CSS, Zustand (cart badge in Header), React

---

### Task 2.1: (public) layout — Header + Footer

**Files:**
- Create: `app/(public)/layout.tsx`
- Create: `components/layout/Header.tsx`
- Create: `components/layout/Footer.tsx`

- [ ] **Step 1: Create components/layout/Footer.tsx (Server Component)**

```typescript
import Link from 'next/link'

export function Footer() {
  return (
    <footer className="bg-dark text-white mt-auto">
      <div className="max-w-7xl mx-auto px-6 py-12 grid grid-cols-4 gap-8">
        {/* Brand */}
        <div>
          <p className="font-rajdhani font-700 text-xl tracking-widest">AUTOHUB</p>
          <p className="font-rajdhani font-600 text-primary text-sm tracking-widest mb-4">STORE</p>
          <p className="font-inter text-sm text-gray-400">
            O melhor em peças e acessórios automotivos de alta performance.
          </p>
        </div>

        {/* Links */}
        <div>
          <p className="font-rajdhani font-600 text-sm tracking-wider mb-4">CATEGORIAS</p>
          {['Rodas', 'Pneus', 'Suspensão', 'Freios', 'Performance', 'Motor'].map((cat) => (
            <Link
              key={cat}
              href="/catalog"
              className="block font-inter text-sm text-gray-400 hover:text-primary mb-2 transition-colors"
            >
              {cat}
            </Link>
          ))}
        </div>

        <div>
          <p className="font-rajdhani font-600 text-sm tracking-wider mb-4">INFORMAÇÕES</p>
          {['Sobre Nós', 'Política de Frete', 'Trocas e Devoluções', 'Contato'].map((link) => (
            <Link
              key={link}
              href="#"
              className="block font-inter text-sm text-gray-400 hover:text-primary mb-2 transition-colors"
            >
              {link}
            </Link>
          ))}
        </div>

        <div>
          <p className="font-rajdhani font-600 text-sm tracking-wider mb-4">CONTATO</p>
          <p className="font-inter text-sm text-gray-400 mb-2">contato@autohubstore.com</p>
          <p className="font-inter text-sm text-gray-400 mb-2">(11) 3000-0000</p>
          <p className="font-inter text-sm text-gray-400">Seg–Sex 9h–18h</p>
        </div>
      </div>

      <div className="border-t border-dark-border">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <p className="font-inter text-xs text-gray-600">
            © 2024 AutoHubStore. Todos os direitos reservados.
          </p>
          <p className="font-inter text-xs text-gray-600">
            CNPJ: 00.000.000/0001-00
          </p>
        </div>
      </div>
    </footer>
  )
}
```

- [ ] **Step 2: Create components/layout/Header.tsx (Client Component)**

```typescript
'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useCartStore } from '@/store/cartStore'
import { useAuthStore } from '@/store/authStore'

export function Header() {
  const [searchValue, setSearchValue] = useState('')
  const router = useRouter()
  const count = useCartStore((s) => s.count)
  const { isLoggedIn, user } = useAuthStore()

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (searchValue.trim()) {
      router.push(`/search?q=${encodeURIComponent(searchValue.trim())}`)
    }
  }

  return (
    <header className="bg-white border-b border-gray-border sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-6">
        {/* Top bar */}
        <div className="flex items-center justify-between py-4 gap-6">
          {/* Logo */}
          <Link href="/" className="flex-shrink-0">
            <p className="font-rajdhani font-700 text-xl text-dark tracking-widest leading-none">AUTOHUB</p>
            <p className="font-rajdhani font-600 text-primary text-xs tracking-widest leading-none">STORE</p>
          </Link>

          {/* Search */}
          <form onSubmit={handleSearch} className="flex-1 max-w-xl">
            <div className="flex border border-gray-border">
              <input
                type="text"
                value={searchValue}
                onChange={(e) => setSearchValue(e.target.value)}
                placeholder="Buscar peças, acessórios, marcas..."
                className="flex-1 px-4 py-2.5 font-inter text-sm text-dark placeholder:text-gray-400 focus:outline-none"
              />
              <button
                type="submit"
                className="bg-primary hover:bg-primary-dark text-white px-4 transition-colors"
                aria-label="Buscar"
              >
                🔍
              </button>
            </div>
          </form>

          {/* Actions */}
          <div className="flex items-center gap-4">
            {isLoggedIn ? (
              <Link href="/account" className="font-inter text-sm text-dark hover:text-primary transition-colors">
                Olá, {user?.name.split(' ')[0]}
              </Link>
            ) : (
              <Link href="/login" className="font-inter text-sm text-dark hover:text-primary transition-colors">
                Entrar
              </Link>
            )}

            <Link href="/cart" className="relative flex items-center gap-1 font-inter text-sm text-dark hover:text-primary transition-colors">
              🛒
              {count > 0 && (
                <span className="absolute -top-2 -right-2 w-5 h-5 bg-primary text-white font-rajdhani font-700 text-xs rounded-full flex items-center justify-center">
                  {count > 9 ? '9+' : count}
                </span>
              )}
              <span>Carrinho</span>
            </Link>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex items-center gap-6 pb-3 border-t border-gray-border pt-3">
          {[
            { label: 'Início', href: '/' },
            { label: 'Catálogo', href: '/catalog' },
            { label: 'Rodas', href: '/catalog' },
            { label: 'Pneus', href: '/catalog' },
            { label: 'Suspensão', href: '/catalog' },
            { label: 'Freios', href: '/catalog' },
            { label: 'Performance', href: '/catalog' },
            { label: 'Motor', href: '/catalog' },
          ].map((item) => (
            <Link
              key={item.label}
              href={item.href}
              className="font-rajdhani font-600 text-sm text-dark hover:text-primary transition-colors tracking-wide"
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </div>
    </header>
  )
}
```

- [ ] **Step 3: Create app/(public)/layout.tsx**

```typescript
import { Header } from '@/components/layout/Header'
import { Footer } from '@/components/layout/Footer'

export default function PublicLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="flex flex-col min-h-screen bg-white">
      <Header />
      <main className="flex-1">{children}</main>
      <Footer />
    </div>
  )
}
```

- [ ] **Step 4: Create placeholder app/(public)/page.tsx**

```typescript
export default function HomePage() {
  return <div className="max-w-7xl mx-auto px-6 py-8">Home page</div>
}
```

- [ ] **Step 5: Verify layout renders**

```bash
npm run dev
```

Open `http://localhost:3000`. Confirm Header (logo, search, cart icon) and Footer render. Cart badge should show nothing (cart is empty).

---

### Task 2.2: Home page — all 8 sections

**Files:**
- Create: `components/home/HeroBanner.tsx`
- Create: `components/home/TrustBadges.tsx`
- Create: `components/home/CategoryGrid.tsx`
- Create: `components/home/ProductSection.tsx`
- Create: `components/home/PromoBanner.tsx`
- Create: `components/home/BrandsGrid.tsx`
- Create: `components/home/Newsletter.tsx`
- Modify: `app/(public)/page.tsx`

- [ ] **Step 1: Create components/home/HeroBanner.tsx**

```typescript
import Link from 'next/link'

export function HeroBanner() {
  return (
    <section className="bg-dark text-white">
      <div className="max-w-7xl mx-auto px-6 py-20 flex items-center justify-between">
        <div className="max-w-xl">
          <p className="font-rajdhani font-600 text-primary text-sm tracking-widest mb-4">
            PEÇAS DE ALTA PERFORMANCE
          </p>
          <h1 className="font-rajdhani font-700 text-5xl tracking-wider leading-tight mb-6">
            PERFORMANCE<br />
            <span className="text-primary">SEM LIMITES</span>
          </h1>
          <p className="font-inter text-gray-400 mb-8">
            As melhores peças e acessórios automotivos para seu carro. Qualidade e performance em um só lugar.
          </p>

          {/* Stats */}
          <div className="flex gap-8 mb-10">
            {[
              { value: '50k+', label: 'Produtos' },
              { value: '200+', label: 'Marcas' },
              { value: '24/7', label: 'Suporte' },
            ].map((stat) => (
              <div key={stat.label}>
                <p className="font-rajdhani font-700 text-2xl text-primary">{stat.value}</p>
                <p className="font-inter text-xs text-gray-500">{stat.label}</p>
              </div>
            ))}
          </div>

          {/* CTAs */}
          <div className="flex gap-4">
            <Link
              href="/catalog"
              className="bg-primary hover:bg-primary-dark text-white font-rajdhani font-600 px-8 py-4 tracking-wider transition-colors"
            >
              VER CATÁLOGO
            </Link>
            <Link
              href="/catalog"
              className="border border-gray-600 hover:border-white text-white font-rajdhani font-600 px-8 py-4 tracking-wider transition-colors"
            >
              OFERTAS
            </Link>
          </div>
        </div>

        {/* Hero image placeholder */}
        <div className="w-96 h-64 bg-dark-2 border border-dark-border flex items-center justify-center">
          <p className="font-rajdhani font-600 text-gray-600 text-sm tracking-wider">AUTOHUB STORE</p>
        </div>
      </div>
    </section>
  )
}
```

- [ ] **Step 2: Create components/home/TrustBadges.tsx**

```typescript
export function TrustBadges() {
  const badges = [
    { icon: '🚚', title: 'Frete Grátis', subtitle: 'Acima de R$ 299' },
    { icon: '🔒', title: 'Compra Segura', subtitle: 'SSL e criptografia' },
    { icon: '↩️', title: 'Troca Fácil', subtitle: '30 dias para trocar' },
    { icon: '🎧', title: 'Suporte 24/7', subtitle: 'Sempre disponível' },
  ]

  return (
    <section className="border-b border-gray-border">
      <div className="max-w-7xl mx-auto px-6 py-6 grid grid-cols-4 gap-4">
        {badges.map((b) => (
          <div key={b.title} className="flex items-center gap-3">
            <span className="text-2xl">{b.icon}</span>
            <div>
              <p className="font-rajdhani font-600 text-sm text-dark tracking-wide">{b.title}</p>
              <p className="font-inter text-xs text-gray-500">{b.subtitle}</p>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
```

- [ ] **Step 3: Create components/home/CategoryGrid.tsx**

```typescript
import Link from 'next/link'
import { categories } from '@/lib/data/categories'

export function CategoryGrid() {
  return (
    <section className="py-12 bg-gray-light">
      <div className="max-w-7xl mx-auto px-6">
        <h2 className="font-rajdhani font-700 text-2xl text-dark tracking-wider mb-8">
          CATEGORIAS
        </h2>
        <div className="grid grid-cols-6 gap-4">
          {categories.map((cat) => (
            <Link
              key={cat.id}
              href="/catalog"
              className="bg-white border border-gray-border hover:border-primary p-6 text-center transition-colors group"
            >
              <span className="text-3xl block mb-3">{cat.icon}</span>
              <p className="font-rajdhani font-600 text-sm text-dark group-hover:text-primary tracking-wide transition-colors">
                {cat.name}
              </p>
              <p className="font-inter text-xs text-gray-400 mt-1">{cat.count} produtos</p>
            </Link>
          ))}
        </div>
      </div>
    </section>
  )
}
```

- [ ] **Step 4: Create components/home/ProductSection.tsx**

This component is reused for both "Produtos em Destaque" and "Mais Vendidos".

```typescript
import Link from 'next/link'
import type { Product } from '@/types/product'
import { Badge } from '@/components/ui/Badge'

type ProductSectionProps = {
  title: string
  products: Product[]
  showRank?: boolean
}

function ProductCard({ product, rank }: { product: Product; rank?: number }) {
  return (
    <div className="bg-white border border-gray-border hover:border-primary transition-colors group">
      {/* Image */}
      <div className="relative aspect-square bg-gray-light border-b border-gray-border flex items-center justify-center">
        {rank && (
          <span className="absolute top-3 left-3 font-rajdhani font-700 text-4xl text-gray-200">
            #{rank}
          </span>
        )}
        {product.tag && (
          <span className={`absolute top-3 right-3 font-rajdhani font-600 text-xs px-2 py-0.5 tracking-wider ${product.tagColor}`}>
            {product.tag}
          </span>
        )}
        <div className="w-32 h-32 bg-dark-2 flex items-center justify-center text-gray-600 text-xs font-inter">
          {product.brand}
        </div>
      </div>

      {/* Info */}
      <div className="p-4">
        <p className="font-inter text-xs text-gray-500 mb-1">{product.brand}</p>
        <p className="font-exo2 font-600 text-sm text-dark leading-snug mb-2 line-clamp-2">
          {product.name}
        </p>

        {/* Stars */}
        <div className="flex items-center gap-1 mb-3">
          {'★'.repeat(product.stars)}{'☆'.repeat(5 - product.stars)}
          <span className="font-inter text-xs text-gray-400 ml-1">({product.reviews})</span>
        </div>

        {/* Price */}
        <div className="mb-3">
          {product.oldPrice && (
            <p className="font-inter text-xs text-gray-400 line-through">
              R$ {product.oldPrice.toFixed(2).replace('.', ',')}
            </p>
          )}
          <p className="font-rajdhani font-700 text-xl text-primary">
            R$ {product.price.toFixed(2).replace('.', ',')}
          </p>
          <p className="font-inter text-xs text-gray-500">
            ou {product.installments}x de R$ {(product.price / product.installments).toFixed(2).replace('.', ',')}
          </p>
        </div>

        <Link
          href={`/products/${product.id}`}
          className="block w-full bg-dark hover:bg-primary text-white font-rajdhani font-600 text-sm text-center py-2.5 tracking-wider transition-colors"
        >
          VER PRODUTO
        </Link>
      </div>
    </div>
  )
}

export function ProductSection({ title, products, showRank = false }: ProductSectionProps) {
  return (
    <section className="py-12">
      <div className="max-w-7xl mx-auto px-6">
        <div className="flex items-center justify-between mb-8">
          <h2 className="font-rajdhani font-700 text-2xl text-dark tracking-wider">{title}</h2>
          <Link
            href="/catalog"
            className="font-rajdhani font-600 text-sm text-primary hover:text-primary-dark tracking-wider transition-colors"
          >
            VER TODOS →
          </Link>
        </div>
        <div className="grid grid-cols-4 gap-6">
          {products.map((product, i) => (
            <ProductCard key={product.id} product={product} rank={showRank ? i + 1 : undefined} />
          ))}
        </div>
      </div>
    </section>
  )
}
```

- [ ] **Step 5: Create components/home/PromoBanner.tsx**

```typescript
import Link from 'next/link'

export function PromoBanner() {
  return (
    <section className="bg-primary py-16">
      <div className="max-w-7xl mx-auto px-6 text-center">
        <p className="font-rajdhani font-600 text-white text-sm tracking-widest mb-3">
          OFERTA ESPECIAL
        </p>
        <h2 className="font-rajdhani font-700 text-4xl text-white tracking-wider mb-4">
          ATÉ 40% OFF EM SUSPENSÃO E FREIOS
        </h2>
        <p className="font-inter text-white/80 mb-8">
          Aproveite as melhores ofertas em peças de alta performance. Por tempo limitado.
        </p>
        <Link
          href="/catalog"
          className="bg-white text-primary font-rajdhani font-700 px-10 py-4 tracking-wider hover:bg-gray-light transition-colors"
        >
          APROVEITAR OFERTA
        </Link>
      </div>
    </section>
  )
}
```

- [ ] **Step 6: Create components/home/BrandsGrid.tsx**

```typescript
import { brands } from '@/lib/data/brands'

export function BrandsGrid() {
  return (
    <section className="py-12 bg-gray-light">
      <div className="max-w-7xl mx-auto px-6">
        <h2 className="font-rajdhani font-700 text-2xl text-dark tracking-wider mb-8 text-center">
          MARCAS PARCEIRAS
        </h2>
        <div className="grid grid-cols-6 gap-4">
          {brands.map((brand) => (
            <div
              key={brand.id}
              className="bg-white border border-gray-border hover:border-primary py-6 px-4 flex items-center justify-center transition-colors"
            >
              <span className="font-rajdhani font-700 text-dark text-sm tracking-wider">
                {brand.name}
              </span>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
```

- [ ] **Step 7: Create components/home/Newsletter.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'

export function Newsletter() {
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (email.trim()) {
      setSubmitted(true)
      setEmail('')
    }
  }

  return (
    <section className="py-16 bg-dark text-white">
      <div className="max-w-2xl mx-auto px-6 text-center">
        <h2 className="font-rajdhani font-700 text-2xl tracking-wider mb-3">
          RECEBA OFERTAS EXCLUSIVAS
        </h2>
        <p className="font-inter text-gray-400 mb-8">
          Cadastre seu e-mail e receba as melhores promoções em primeira mão.
        </p>

        {submitted ? (
          <p className="font-inter text-primary">E-mail cadastrado com sucesso!</p>
        ) : (
          <form onSubmit={handleSubmit} className="flex gap-0">
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="seu@email.com"
              required
              className="flex-1 px-4 py-3 bg-dark-2 border border-dark-border text-white font-inter text-sm placeholder:text-gray-600 focus:outline-none focus:border-primary transition-colors"
            />
            <button
              type="submit"
              className="bg-primary hover:bg-primary-dark text-white font-rajdhani font-600 px-8 tracking-wider transition-colors"
            >
              CADASTRAR
            </button>
          </form>
        )}
      </div>
    </section>
  )
}
```

- [ ] **Step 8: Assemble app/(public)/page.tsx**

```typescript
import { products } from '@/lib/data/products'
import { HeroBanner } from '@/components/home/HeroBanner'
import { TrustBadges } from '@/components/home/TrustBadges'
import { CategoryGrid } from '@/components/home/CategoryGrid'
import { ProductSection } from '@/components/home/ProductSection'
import { PromoBanner } from '@/components/home/PromoBanner'
import { BrandsGrid } from '@/components/home/BrandsGrid'
import { Newsletter } from '@/components/home/Newsletter'

export default function HomePage() {
  const bestSellers = products.slice(0, 4)

  return (
    <>
      <HeroBanner />
      <TrustBadges />
      <CategoryGrid />
      <ProductSection title="PRODUTOS EM DESTAQUE" products={products} />
      <PromoBanner />
      <ProductSection title="MAIS VENDIDOS" products={bestSellers} showRank />
      <BrandsGrid />
      <Newsletter />
    </>
  )
}
```

- [ ] **Step 9: Verify home page**

```bash
npm run dev
```

Open `http://localhost:3000`. Confirm all 8 sections render in order: hero, trust badges, categories, 8 products in 4-col grid, promo banner, 4 best sellers with rank numbers, 12 brands, newsletter input.

---

### Task 2.3: Catalog page

**Files:**
- Create: `components/catalog/ProductCard.tsx`
- Create: `components/catalog/FilterSidebar.tsx`
- Create: `components/catalog/SortBar.tsx`
- Create: `components/catalog/Pagination.tsx`
- Create: `components/catalog/ProductGrid.tsx`
- Create: `app/(public)/catalog/page.tsx`

- [ ] **Step 1: Create components/catalog/ProductCard.tsx**

```typescript
import Link from 'next/link'
import type { Product } from '@/types/product'

export function ProductCard({ product }: { product: Product }) {
  return (
    <div className="bg-white border border-gray-border hover:border-primary transition-colors group">
      <div className="relative aspect-square bg-gray-light border-b border-gray-border flex items-center justify-center">
        {product.tag && (
          <span className={`absolute top-3 left-3 font-rajdhani font-600 text-xs px-2 py-0.5 tracking-wider ${product.tagColor}`}>
            {product.tag}
          </span>
        )}
        {!product.inStock && (
          <span className="absolute top-3 right-3 font-rajdhani font-600 text-xs px-2 py-0.5 bg-gray-400 text-white tracking-wider">
            ESGOTADO
          </span>
        )}
        <div className="w-28 h-28 bg-dark-2 flex items-center justify-center text-gray-600 text-xs font-inter">
          {product.brand}
        </div>
      </div>

      <div className="p-4">
        <p className="font-inter text-xs text-gray-500 mb-1">{product.brand}</p>
        <Link href={`/products/${product.id}`}>
          <p className="font-exo2 font-600 text-sm text-dark leading-snug mb-2 hover:text-primary transition-colors line-clamp-2">
            {product.name}
          </p>
        </Link>

        <div className="flex items-center gap-1 mb-3 text-primary text-sm">
          {'★'.repeat(product.stars)}{'☆'.repeat(5 - product.stars)}
          <span className="font-inter text-xs text-gray-400 ml-1">({product.reviews})</span>
        </div>

        <div className="mb-4">
          {product.oldPrice && (
            <p className="font-inter text-xs text-gray-400 line-through">
              R$ {product.oldPrice.toFixed(2).replace('.', ',')}
            </p>
          )}
          <p className="font-rajdhani font-700 text-xl text-primary">
            R$ {product.price.toFixed(2).replace('.', ',')}
          </p>
          <p className="font-inter text-xs text-gray-500">
            ou {product.installments}x de R$ {(product.price / product.installments).toFixed(2).replace('.', ',')}
          </p>
        </div>

        <Link
          href={`/products/${product.id}`}
          className="block w-full bg-dark hover:bg-primary text-white font-rajdhani font-600 text-sm text-center py-2.5 tracking-wider transition-colors"
        >
          VER PRODUTO
        </Link>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Create components/catalog/FilterSidebar.tsx (Client)**

```typescript
'use client'

import { categories } from '@/lib/data/categories'
import { brands } from '@/lib/data/brands'

type Filters = {
  category: string
  brands: string[]
  minPrice: number
  maxPrice: number
  inStock: boolean
}

type FilterSidebarProps = {
  filters: Filters
  onChange: (filters: Filters) => void
}

export function FilterSidebar({ filters, onChange }: FilterSidebarProps) {
  function toggleBrand(brandId: string) {
    const next = filters.brands.includes(brandId)
      ? filters.brands.filter((b) => b !== brandId)
      : [...filters.brands, brandId]
    onChange({ ...filters, brands: next })
  }

  return (
    <aside className="w-56 flex-shrink-0">
      {/* Category */}
      <div className="mb-6">
        <h3 className="font-rajdhani font-600 text-sm text-dark tracking-wider mb-3">CATEGORIA</h3>
        <div className="space-y-1">
          <button
            onClick={() => onChange({ ...filters, category: '' })}
            className={`block w-full text-left font-inter text-sm py-1 transition-colors ${
              filters.category === '' ? 'text-primary font-500' : 'text-gray-600 hover:text-dark'
            }`}
          >
            Todos os produtos
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => onChange({ ...filters, category: cat.name })}
              className={`block w-full text-left font-inter text-sm py-1 transition-colors ${
                filters.category === cat.name ? 'text-primary font-500' : 'text-gray-600 hover:text-dark'
              }`}
            >
              {cat.name}
              <span className="text-gray-400 ml-1">({cat.count})</span>
            </button>
          ))}
        </div>
      </div>

      {/* Price range */}
      <div className="mb-6">
        <h3 className="font-rajdhani font-600 text-sm text-dark tracking-wider mb-3">FAIXA DE PREÇO</h3>
        <div className="flex gap-2 items-center">
          <input
            type="number"
            value={filters.minPrice}
            onChange={(e) => onChange({ ...filters, minPrice: Number(e.target.value) })}
            placeholder="Min"
            className="w-full border border-gray-border px-3 py-2 font-inter text-xs focus:outline-none focus:border-dark"
          />
          <span className="text-gray-400 text-xs">–</span>
          <input
            type="number"
            value={filters.maxPrice}
            onChange={(e) => onChange({ ...filters, maxPrice: Number(e.target.value) })}
            placeholder="Max"
            className="w-full border border-gray-border px-3 py-2 font-inter text-xs focus:outline-none focus:border-dark"
          />
        </div>
      </div>

      {/* Brands */}
      <div className="mb-6">
        <h3 className="font-rajdhani font-600 text-sm text-dark tracking-wider mb-3">MARCAS</h3>
        <div className="space-y-2">
          {brands.map((brand) => (
            <label key={brand.id} className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={filters.brands.includes(brand.id)}
                onChange={() => toggleBrand(brand.id)}
                className="accent-primary"
              />
              <span className="font-inter text-sm text-gray-600">{brand.name}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Stock */}
      <div>
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={filters.inStock}
            onChange={(e) => onChange({ ...filters, inStock: e.target.checked })}
            className="accent-primary"
          />
          <span className="font-inter text-sm text-gray-600">Somente em estoque</span>
        </label>
      </div>
    </aside>
  )
}
```

- [ ] **Step 3: Create components/catalog/SortBar.tsx (Client)**

```typescript
'use client'

type SortBarProps = {
  count: number
  sortBy: string
  onSortChange: (sort: string) => void
}

export function SortBar({ count, sortBy, onSortChange }: SortBarProps) {
  return (
    <div className="flex items-center justify-between mb-6">
      <p className="font-inter text-sm text-gray-500">
        <span className="font-500 text-dark">{count}</span> produtos encontrados
      </p>
      <div className="flex items-center gap-3">
        <span className="font-inter text-sm text-gray-500">Ordenar por:</span>
        <select
          value={sortBy}
          onChange={(e) => onSortChange(e.target.value)}
          className="border border-gray-border px-3 py-2 font-inter text-sm text-dark focus:outline-none focus:border-dark"
        >
          <option value="relevance">Relevância</option>
          <option value="price-asc">Menor preço</option>
          <option value="price-desc">Maior preço</option>
          <option value="reviews">Mais vendidos</option>
        </select>
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Create components/catalog/Pagination.tsx (Client)**

```typescript
'use client'

type PaginationProps = {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  const pages = Array.from({ length: Math.min(totalPages, 5) }, (_, i) => i + 1)

  return (
    <div className="flex items-center justify-center gap-1 mt-10">
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
        className="w-9 h-9 flex items-center justify-center border border-gray-border font-rajdhani font-600 text-dark hover:border-primary hover:text-primary transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
      >
        ‹
      </button>

      {pages.map((p) => (
        <button
          key={p}
          onClick={() => onPageChange(p)}
          className={`w-9 h-9 flex items-center justify-center border font-rajdhani font-600 text-sm transition-colors ${
            p === currentPage
              ? 'bg-primary border-primary text-white'
              : 'border-gray-border text-dark hover:border-primary hover:text-primary'
          }`}
        >
          {p}
        </button>
      ))}

      {totalPages > 5 && (
        <>
          <span className="font-inter text-gray-400 px-1">…</span>
          <button
            onClick={() => onPageChange(totalPages)}
            className="w-9 h-9 flex items-center justify-center border border-gray-border font-rajdhani font-600 text-sm text-dark hover:border-primary hover:text-primary transition-colors"
          >
            {totalPages}
          </button>
        </>
      )}

      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
        className="w-9 h-9 flex items-center justify-center border border-gray-border font-rajdhani font-600 text-dark hover:border-primary hover:text-primary transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
      >
        ›
      </button>
    </div>
  )
}
```

- [ ] **Step 5: Create components/catalog/ProductGrid.tsx (Client — owns filter/sort state)**

```typescript
'use client'

import { useState } from 'react'
import type { Product } from '@/types/product'
import { ProductCard } from './ProductCard'
import { FilterSidebar } from './FilterSidebar'
import { SortBar } from './SortBar'
import { Pagination } from './Pagination'

const ITEMS_PER_PAGE = 6

type Filters = {
  category: string
  brands: string[]
  minPrice: number
  maxPrice: number
  inStock: boolean
}

const DEFAULT_FILTERS: Filters = {
  category: '',
  brands: [],
  minPrice: 0,
  maxPrice: 999999,
  inStock: false,
}

export function ProductGrid({ allProducts }: { allProducts: Product[] }) {
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS)
  const [sortBy, setSortBy] = useState('relevance')
  const [page, setPage] = useState(1)

  const filtered = allProducts
    .filter((p) => !filters.category || p.category === filters.category)
    .filter((p) => filters.brands.length === 0 || filters.brands.includes(p.brand.toLowerCase().replace(/[^a-z0-9]/g, '')))
    .filter((p) => p.price >= filters.minPrice && (filters.maxPrice === 999999 || p.price <= filters.maxPrice))
    .filter((p) => !filters.inStock || p.inStock)

  const sorted = [...filtered].sort((a, b) => {
    if (sortBy === 'price-asc') return a.price - b.price
    if (sortBy === 'price-desc') return b.price - a.price
    if (sortBy === 'reviews') return b.reviews - a.reviews
    return 0
  })

  const totalPages = Math.max(1, Math.ceil(sorted.length / ITEMS_PER_PAGE))
  const paginated = sorted.slice((page - 1) * ITEMS_PER_PAGE, page * ITEMS_PER_PAGE)

  function handleFilterChange(next: Filters) {
    setFilters(next)
    setPage(1)
  }

  return (
    <div className="flex gap-8">
      <FilterSidebar filters={filters} onChange={handleFilterChange} />

      <div className="flex-1">
        <SortBar count={sorted.length} sortBy={sortBy} onSortChange={setSortBy} />

        {paginated.length === 0 ? (
          <div className="py-20 text-center">
            <p className="font-inter text-gray-500">Nenhum produto encontrado com esses filtros.</p>
          </div>
        ) : (
          <div className="grid grid-cols-3 gap-6">
            {paginated.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </div>
        )}

        <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
      </div>
    </div>
  )
}
```

- [ ] **Step 6: Create app/(public)/catalog/page.tsx**

```typescript
import { products } from '@/lib/data/products'
import { ProductGrid } from '@/components/catalog/ProductGrid'
import Link from 'next/link'

export default function CatalogPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 font-inter text-sm text-gray-500 mb-6">
        <Link href="/" className="hover:text-primary transition-colors">Início</Link>
        <span>/</span>
        <span className="text-dark">Catálogo</span>
      </nav>

      <h1 className="font-rajdhani font-700 text-2xl text-dark tracking-wider mb-8">CATÁLOGO</h1>

      <ProductGrid allProducts={products} />
    </div>
  )
}
```

- [ ] **Step 7: Verify catalog**

```bash
npm run dev
```

Open `http://localhost:3000/catalog`. Confirm: filter sidebar renders, products grid shows, category filter works, sort dropdown re-orders, pagination updates.

---

### Task 2.4: Product detail page

**Files:**
- Create: `components/product/ImageGallery.tsx`
- Create: `components/product/ProductInfo.tsx`
- Create: `components/product/ProductTabs.tsx`
- Create: `app/(public)/products/[id]/page.tsx`

- [ ] **Step 1: Create components/product/ImageGallery.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'

export function ImageGallery({ images, name }: { images: string[]; name: string }) {
  const [selected, setSelected] = useState(0)

  return (
    <div className="flex gap-4">
      {/* Thumbnails */}
      <div className="flex flex-col gap-2">
        {images.map((src, i) => (
          <button
            key={i}
            onClick={() => setSelected(i)}
            className={`w-16 h-16 border-2 transition-colors ${
              i === selected ? 'border-primary' : 'border-gray-border hover:border-dark'
            } bg-gray-light flex items-center justify-center`}
          >
            <span className="font-inter text-xs text-gray-400">{i + 1}</span>
          </button>
        ))}
      </div>

      {/* Main image */}
      <div className="flex-1 aspect-square bg-gray-light border border-gray-border flex items-center justify-center">
        <p className="font-rajdhani font-600 text-gray-400 text-sm tracking-wider">
          {name} — Imagem {selected + 1}
        </p>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Create components/product/ProductInfo.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useCartStore } from '@/store/cartStore'
import type { Product } from '@/types/product'

export function ProductInfo({ product }: { product: Product }) {
  const [qty, setQty] = useState(1)
  const addItem = useCartStore((s) => s.addItem)
  const router = useRouter()

  function handleAddToCart() {
    addItem(product, qty)
    router.push('/cart')
  }

  function handleBuyNow() {
    addItem(product, qty)
    router.push('/checkout')
  }

  return (
    <div className="flex flex-col gap-4">
      {/* Tag */}
      {product.tag && (
        <span className={`w-fit font-rajdhani font-600 text-xs px-3 py-1 tracking-wider ${product.tagColor}`}>
          {product.tag}
        </span>
      )}

      <h1 className="font-exo2 font-700 text-2xl text-dark leading-snug">{product.name}</h1>

      {/* Rating */}
      <div className="flex items-center gap-2">
        <span className="text-primary">{'★'.repeat(product.stars)}{'☆'.repeat(5 - product.stars)}</span>
        <span className="font-inter text-sm text-gray-500">({product.reviews} avaliações)</span>
        <span className="text-gray-300">|</span>
        <span className="font-inter text-sm text-gray-500">Marca: <span className="text-dark font-500">{product.brand}</span></span>
      </div>

      {/* Price */}
      <div className="border-t border-b border-gray-border py-4">
        {product.oldPrice && (
          <p className="font-inter text-sm text-gray-400 line-through mb-1">
            R$ {product.oldPrice.toFixed(2).replace('.', ',')}
          </p>
        )}
        <p className="font-rajdhani font-700 text-3xl text-primary">
          R$ {product.price.toFixed(2).replace('.', ',')}
        </p>
        <p className="font-inter text-sm text-gray-500 mt-1">
          ou {product.installments}x de R$ {(product.price / product.installments).toFixed(2).replace('.', ',')} sem juros
        </p>
        <p className="font-inter text-sm text-green-600 mt-1">
          PIX: R$ {(product.price * 0.95).toFixed(2).replace('.', ',')} (5% off)
        </p>
      </div>

      {/* Stock status */}
      <p className={`font-inter text-sm font-500 ${product.inStock ? 'text-green-600' : 'text-red-500'}`}>
        {product.inStock ? '✓ Em estoque' : '✗ Produto esgotado'}
      </p>

      {/* Qty + CTA */}
      {product.inStock && (
        <>
          <div className="flex items-center gap-3">
            <span className="font-inter text-sm text-gray-600">Quantidade:</span>
            <div className="flex items-center border border-gray-border">
              <button
                onClick={() => setQty((q) => Math.max(1, q - 1))}
                className="w-10 h-10 flex items-center justify-center hover:bg-gray-light transition-colors font-rajdhani font-600"
              >
                −
              </button>
              <span className="w-12 text-center font-rajdhani font-600 text-dark">{qty}</span>
              <button
                onClick={() => setQty((q) => q + 1)}
                className="w-10 h-10 flex items-center justify-center hover:bg-gray-light transition-colors font-rajdhani font-600"
              >
                +
              </button>
            </div>
          </div>

          <div className="flex gap-3">
            <button
              onClick={handleAddToCart}
              className="flex-1 bg-dark hover:bg-primary text-white font-rajdhani font-600 py-4 tracking-wider transition-colors"
            >
              ADICIONAR AO CARRINHO
            </button>
            <button
              onClick={handleBuyNow}
              className="flex-1 bg-primary hover:bg-primary-dark text-white font-rajdhani font-600 py-4 tracking-wider transition-colors"
            >
              COMPRAR AGORA
            </button>
          </div>
        </>
      )}

      {/* Trust bullets */}
      <div className="flex flex-col gap-2 pt-2">
        {['🚚 Frete grátis acima de R$ 299', '🔒 Garantia de 12 meses', '✓ Produto original'].map((b) => (
          <p key={b} className="font-inter text-sm text-gray-600">{b}</p>
        ))}
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Create components/product/ProductTabs.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'
import type { Product } from '@/types/product'

type Tab = 'descricao' | 'specs' | 'compat' | 'avaliacoes'

export function ProductTabs({ product }: { product: Product }) {
  const [activeTab, setActiveTab] = useState<Tab>('descricao')

  const tabs: { id: Tab; label: string }[] = [
    { id: 'descricao', label: 'Descrição' },
    { id: 'specs', label: 'Especificações' },
    { id: 'compat', label: 'Compatibilidade' },
    { id: 'avaliacoes', label: 'Avaliações' },
  ]

  return (
    <div className="mt-12">
      {/* Tab nav */}
      <div className="flex border-b border-gray-border">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`font-rajdhani font-600 text-sm tracking-wider px-6 py-3 border-b-2 transition-colors ${
              activeTab === tab.id
                ? 'border-primary text-primary'
                : 'border-transparent text-gray-500 hover:text-dark'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Tab content */}
      <div className="py-8">
        {activeTab === 'descricao' && (
          <p className="font-inter text-gray-600 leading-relaxed max-w-2xl">{product.description}</p>
        )}

        {activeTab === 'specs' && (
          <table className="w-full max-w-lg">
            <tbody>
              {Object.entries(product.specs).map(([key, value], i) => (
                <tr key={key} className={i % 2 === 0 ? 'bg-gray-light' : 'bg-white'}>
                  <td className="font-inter text-sm font-500 text-dark px-4 py-3 w-40">{key}</td>
                  <td className="font-inter text-sm text-gray-600 px-4 py-3">{value}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {activeTab === 'compat' && (
          <div>
            <p className="font-inter text-sm text-gray-600 mb-4">
              Consulte a compatibilidade com o seu veículo:
            </p>
            <select className="border border-gray-border px-4 py-2 font-inter text-sm text-dark focus:outline-none focus:border-dark">
              <option>Selecione o ano</option>
              {[2024, 2023, 2022, 2021, 2020].map((y) => (
                <option key={y}>{y}</option>
              ))}
            </select>
          </div>
        )}

        {activeTab === 'avaliacoes' && (
          <div>
            <div className="flex items-center gap-4 mb-6">
              <p className="font-rajdhani font-700 text-4xl text-primary">{product.stars}.0</p>
              <div>
                <p className="text-primary text-lg">{'★'.repeat(product.stars)}</p>
                <p className="font-inter text-sm text-gray-500">{product.reviews} avaliações</p>
              </div>
            </div>
            <p className="font-inter text-sm text-gray-500">Avaliações detalhadas em breve.</p>
          </div>
        )}
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Create app/(public)/products/[id]/page.tsx**

```typescript
import { notFound } from 'next/navigation'
import { products } from '@/lib/data/products'
import { ImageGallery } from '@/components/product/ImageGallery'
import { ProductInfo } from '@/components/product/ProductInfo'
import { ProductTabs } from '@/components/product/ProductTabs'
import Link from 'next/link'

export function generateStaticParams() {
  return products.map((p) => ({ id: String(p.id) }))
}

export default function ProductPage({ params }: { params: { id: string } }) {
  const product = products.find((p) => p.id === Number(params.id))
  if (!product) notFound()

  return (
    <div className="max-w-7xl mx-auto px-6 py-8">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 font-inter text-sm text-gray-500 mb-6">
        <Link href="/" className="hover:text-primary transition-colors">Início</Link>
        <span>/</span>
        <Link href="/catalog" className="hover:text-primary transition-colors">Catálogo</Link>
        <span>/</span>
        <span className="text-dark line-clamp-1">{product.name}</span>
      </nav>

      {/* Main content */}
      <div className="grid grid-cols-2 gap-12">
        <ImageGallery images={product.images} name={product.name} />
        <ProductInfo product={product} />
      </div>

      <ProductTabs product={product} />
    </div>
  )
}
```

- [ ] **Step 5: Verify product detail**

```bash
npm run dev
```

Open `http://localhost:3000/products/1`. Confirm: breadcrumb, thumbnail gallery (4 items), product info with price/qty/CTA buttons, tabs switching between Descrição/Especificações/Compatibilidade/Avaliações. Click "ADICIONAR AO CARRINHO" — confirm cart badge in header updates.

---

### Task 2.5: Login page

**Files:**
- Create: `app/(public)/login/page.tsx`

- [ ] **Step 1: Create app/(public)/login/page.tsx**

```typescript
'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const login = useAuthStore((s) => s.login)
  const router = useRouter()

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    login()
    router.push('/account')
  }

  return (
    <div className="min-h-screen bg-gray-light flex items-center justify-center">
      <div className="bg-dark-2 p-10 w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <p className="font-rajdhani font-700 text-2xl text-white tracking-widest">AUTOHUB</p>
          <p className="font-rajdhani font-600 text-primary text-sm tracking-widest">STORE</p>
        </div>

        <h1 className="font-rajdhani font-700 text-xl text-white tracking-wider mb-6 text-center">
          ENTRAR NA CONTA
        </h1>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block font-inter text-xs text-gray-400 mb-1">E-MAIL</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="seu@email.com"
              required
              className="w-full bg-dark-3 border border-dark-border text-white font-inter text-sm px-4 py-3 placeholder:text-gray-600 focus:outline-none focus:border-primary transition-colors"
            />
          </div>

          <div>
            <label className="block font-inter text-xs text-gray-400 mb-1">SENHA</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              className="w-full bg-dark-3 border border-dark-border text-white font-inter text-sm px-4 py-3 placeholder:text-gray-600 focus:outline-none focus:border-primary transition-colors"
            />
          </div>

          <button
            type="submit"
            className="w-full bg-primary hover:bg-primary-dark text-white font-rajdhani font-700 py-4 tracking-widest mt-2 transition-colors"
          >
            ENTRAR
          </button>
        </form>

        <p className="font-inter text-sm text-gray-500 text-center mt-6">
          Não tem conta?{' '}
          <a href="/login" className="text-primary hover:text-primary-dark transition-colors">
            Criar conta grátis
          </a>
        </p>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Verify login**

```bash
npm run dev
```

Open `http://localhost:3000/login`. Confirm dark card renders on light background. Fill any email+password, click "ENTRAR" — confirm redirect to `/account` and header shows "Olá, Carlos".

---

### Task 2.6: Search page (placeholder)

**Files:**
- Create: `app/(public)/search/page.tsx`

- [ ] **Step 1: Create app/(public)/search/page.tsx**

```typescript
import { products } from '@/lib/data/products'
import { ProductCard } from '@/components/catalog/ProductCard'
import Link from 'next/link'

export default function SearchPage({
  searchParams,
}: {
  searchParams: { q?: string }
}) {
  const query = searchParams.q ?? ''
  const results = products.filter(
    (p) =>
      p.name.toLowerCase().includes(query.toLowerCase()) ||
      p.brand.toLowerCase().includes(query.toLowerCase()) ||
      p.category.toLowerCase().includes(query.toLowerCase())
  )

  return (
    <div className="max-w-7xl mx-auto px-6 py-8">
      <nav className="flex items-center gap-2 font-inter text-sm text-gray-500 mb-6">
        <Link href="/" className="hover:text-primary transition-colors">Início</Link>
        <span>/</span>
        <span className="text-dark">Busca</span>
      </nav>

      <h1 className="font-rajdhani font-700 text-2xl text-dark tracking-wider mb-2">
        RESULTADOS DA BUSCA
      </h1>
      <p className="font-inter text-sm text-gray-500 mb-8">
        {results.length} resultado{results.length !== 1 ? 's' : ''} para &ldquo;{query}&rdquo;
      </p>

      {results.length === 0 ? (
        <div className="py-20 text-center">
          <p className="font-inter text-gray-500 mb-4">Nenhum produto encontrado para &ldquo;{query}&rdquo;.</p>
          <Link href="/catalog" className="font-rajdhani font-600 text-primary hover:text-primary-dark tracking-wider">
            VER CATÁLOGO COMPLETO →
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-4 gap-6">
          {results.map((p) => (
            <ProductCard key={p.id} product={p} />
          ))}
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Verify search**

```bash
npm run dev
```

Open `http://localhost:3000/search?q=brembo`. Confirm: 2 Brembo products display. Try `/search?q=xyz` — confirm "Nenhum produto" message and catalog link.

---

## Verification

After Plan 2 is complete:

```bash
npm run build    # No TS errors, all pages statically generated
npm run dev      # Dev server starts
```

Manual checks:
- `/` — all 8 sections render, product cards link to detail pages
- `/catalog` — filter sidebar works, sort updates order, pagination shows pages
- `/products/1` — gallery thumbnails switch image, "Adicionar ao Carrinho" updates header badge
- `/login` — submit redirects to `/account`, header shows logged-in state
- `/search?q=michelin` — finds Michelin product
- Header cart badge updates in real time after adding products

**Next:** Execute Plan 3 (Customer Area).
