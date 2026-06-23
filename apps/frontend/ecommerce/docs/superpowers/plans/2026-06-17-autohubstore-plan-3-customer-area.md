# AutoHubStore — Plan 3: Customer Area

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Prerequisite:** Plans 1 (Foundation) and 2 (Public Store) must be complete.

**Goal:** Build the customer-facing pages — Cart, Checkout (4-step form), Account (with tabs), and Orders list.

**Architecture:** Route group `(customer)` reuses the same Header+Footer layout as `(public)`. State comes from `cartStore` and `authStore` (Zustand). Checkout form uses React Hook Form + Zod validation. All data is mock/static.

**Tech Stack:** Next.js 14 App Router, Zustand, React Hook Form 7+, Zod 3+, Tailwind CSS

---

### Task 3.1: (customer) layout

**Files:**
- Create: `app/(customer)/layout.tsx`

- [ ] **Step 1: Create app/(customer)/layout.tsx**

```typescript
import { Header } from '@/components/layout/Header'
import { Footer } from '@/components/layout/Footer'

export default function CustomerLayout({
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

---

### Task 3.2: Cart page

**Files:**
- Create: `components/cart/CartTable.tsx`
- Create: `components/cart/OrderSummary.tsx`
- Create: `app/(customer)/cart/page.tsx`

- [ ] **Step 1: Create components/cart/CartTable.tsx (Client)**

```typescript
'use client'

import Link from 'next/link'
import { useCartStore } from '@/store/cartStore'

export function CartTable() {
  const { items, removeItem, updateQty } = useCartStore()

  if (items.length === 0) {
    return (
      <div className="py-20 text-center">
        <p className="font-inter text-gray-500 mb-4">Seu carrinho está vazio.</p>
        <Link
          href="/catalog"
          className="font-rajdhani font-600 text-primary hover:text-primary-dark tracking-wider transition-colors"
        >
          CONTINUAR COMPRANDO →
        </Link>
      </div>
    )
  }

  return (
    <div>
      {/* Table header */}
      <div className="grid grid-cols-[2fr_1fr_1fr_1fr_auto] gap-4 px-4 py-3 border-b border-gray-border">
        {['PRODUTO', 'PREÇO', 'QUANTIDADE', 'SUBTOTAL', ''].map((h) => (
          <span key={h} className="font-rajdhani font-600 text-xs text-gray-500 tracking-wider">{h}</span>
        ))}
      </div>

      {items.map(({ product, qty }) => (
        <div
          key={product.id}
          className="grid grid-cols-[2fr_1fr_1fr_1fr_auto] gap-4 px-4 py-6 border-b border-gray-border items-center"
        >
          {/* Product */}
          <div className="flex items-center gap-4">
            <div className="w-20 h-20 bg-gray-light border border-gray-border flex items-center justify-center flex-shrink-0">
              <span className="font-inter text-xs text-gray-400">{product.brand}</span>
            </div>
            <div>
              <p className="font-inter text-xs text-gray-500 mb-1">{product.brand}</p>
              <Link href={`/products/${product.id}`}>
                <p className="font-exo2 font-600 text-sm text-dark hover:text-primary transition-colors">
                  {product.name}
                </p>
              </Link>
            </div>
          </div>

          {/* Unit price */}
          <span className="font-rajdhani font-600 text-dark">
            R$ {product.price.toFixed(2).replace('.', ',')}
          </span>

          {/* Qty controls */}
          <div className="flex items-center border border-gray-border w-fit">
            <button
              onClick={() => qty > 1 ? updateQty(product.id, qty - 1) : removeItem(product.id)}
              className="w-9 h-9 flex items-center justify-center hover:bg-gray-light transition-colors font-rajdhani font-600"
            >
              −
            </button>
            <span className="w-10 text-center font-rajdhani font-600 text-dark text-sm">{qty}</span>
            <button
              onClick={() => updateQty(product.id, qty + 1)}
              className="w-9 h-9 flex items-center justify-center hover:bg-gray-light transition-colors font-rajdhani font-600"
            >
              +
            </button>
          </div>

          {/* Subtotal */}
          <span className="font-rajdhani font-700 text-primary">
            R$ {(product.price * qty).toFixed(2).replace('.', ',')}
          </span>

          {/* Remove */}
          <button
            onClick={() => removeItem(product.id)}
            className="font-inter text-xs text-gray-400 hover:text-red-500 transition-colors"
            aria-label="Remover produto"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  )
}
```

- [ ] **Step 2: Create components/cart/OrderSummary.tsx (Client)**

```typescript
'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useCartStore } from '@/store/cartStore'

const PROMO_CODE = 'PROMO10'
const SHIPPING_THRESHOLD = 299
const SHIPPING_COST = 29.9

export function OrderSummary() {
  const { total, items } = useCartStore()
  const [coupon, setCoupon] = useState('')
  const [discount, setDiscount] = useState(0)
  const [couponError, setCouponError] = useState('')

  const shipping = total >= SHIPPING_THRESHOLD ? 0 : SHIPPING_COST
  const finalTotal = total - discount + shipping

  function applyCoupon() {
    if (coupon.toUpperCase() === PROMO_CODE) {
      setDiscount(total * 0.1)
      setCouponError('')
    } else {
      setDiscount(0)
      setCouponError('Cupom inválido.')
    }
  }

  return (
    <div className="bg-gray-light border border-gray-border p-6 w-80">
      <h2 className="font-rajdhani font-700 text-lg text-dark tracking-wider mb-6">RESUMO DO PEDIDO</h2>

      {/* Coupon */}
      <div className="mb-6">
        <div className="flex gap-2">
          <input
            type="text"
            value={coupon}
            onChange={(e) => setCoupon(e.target.value)}
            placeholder="Cupom de desconto"
            className="flex-1 border border-gray-border px-3 py-2 font-inter text-sm focus:outline-none focus:border-dark"
          />
          <button
            onClick={applyCoupon}
            className="bg-dark hover:bg-primary text-white font-rajdhani font-600 px-4 text-sm tracking-wider transition-colors"
          >
            APLICAR
          </button>
        </div>
        {couponError && <p className="font-inter text-xs text-primary mt-1">{couponError}</p>}
        {discount > 0 && (
          <p className="font-inter text-xs text-green-600 mt-1">Cupom PROMO10 aplicado!</p>
        )}
      </div>

      {/* Totals */}
      <div className="space-y-3 mb-6">
        <div className="flex justify-between">
          <span className="font-inter text-sm text-gray-600">Subtotal</span>
          <span className="font-rajdhani font-600 text-dark">
            R$ {total.toFixed(2).replace('.', ',')}
          </span>
        </div>

        <div className="flex justify-between">
          <span className="font-inter text-sm text-gray-600">Frete</span>
          <span className="font-rajdhani font-600 text-dark">
            {shipping === 0 ? (
              <span className="text-green-600">GRÁTIS</span>
            ) : (
              `R$ ${shipping.toFixed(2).replace('.', ',')}`
            )}
          </span>
        </div>

        {discount > 0 && (
          <div className="flex justify-between">
            <span className="font-inter text-sm text-green-600">Desconto (PROMO10)</span>
            <span className="font-rajdhani font-600 text-green-600">
              − R$ {discount.toFixed(2).replace('.', ',')}
            </span>
          </div>
        )}

        <div className="border-t border-gray-border pt-3 flex justify-between">
          <span className="font-rajdhani font-700 text-dark">TOTAL</span>
          <div className="text-right">
            <p className="font-rajdhani font-700 text-xl text-primary">
              R$ {finalTotal.toFixed(2).replace('.', ',')}
            </p>
            <p className="font-inter text-xs text-gray-500">
              ou 12x de R$ {(finalTotal / 12).toFixed(2).replace('.', ',')}
            </p>
          </div>
        </div>
      </div>

      {items.length > 0 && (
        <Link
          href="/checkout"
          className="block w-full bg-primary hover:bg-primary-dark text-white font-rajdhani font-700 text-center py-4 tracking-wider transition-colors mb-3"
        >
          FINALIZAR PEDIDO
        </Link>
      )}

      <Link
        href="/catalog"
        className="block w-full border border-gray-border hover:border-dark text-dark font-rajdhani font-600 text-center py-3 tracking-wider transition-colors text-sm"
      >
        CONTINUAR COMPRANDO
      </Link>
    </div>
  )
}
```

- [ ] **Step 3: Create app/(customer)/cart/page.tsx**

```typescript
import { CartTable } from '@/components/cart/CartTable'
import { OrderSummary } from '@/components/cart/OrderSummary'
import Link from 'next/link'

export default function CartPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8">
      <nav className="flex items-center gap-2 font-inter text-sm text-gray-500 mb-6">
        <Link href="/" className="hover:text-primary transition-colors">Início</Link>
        <span>/</span>
        <span className="text-dark">Carrinho</span>
      </nav>

      <h1 className="font-rajdhani font-700 text-2xl text-dark tracking-wider mb-8">MEU CARRINHO</h1>

      <div className="flex gap-8 items-start">
        <div className="flex-1">
          <CartTable />
        </div>
        <OrderSummary />
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Verify cart page**

```bash
npm run dev
```

1. Open `http://localhost:3000/products/1` → click "ADICIONAR AO CARRINHO"
2. Confirm redirect to `/cart`
3. Confirm product row shows with editable qty and subtotal
4. Click + button → subtotal and header badge update
5. Enter "PROMO10" coupon → discount applies
6. Click ✕ → item removed, empty state shows

---

### Task 3.3: Checkout — 4-step form

**Files:**
- Create: `components/checkout/AddressForm.tsx`
- Create: `components/checkout/DeliveryStep.tsx`
- Create: `components/checkout/PaymentStep.tsx`
- Create: `components/checkout/ConfirmationStep.tsx`
- Create: `components/checkout/CheckoutStepper.tsx`
- Create: `app/(customer)/checkout/page.tsx`

- [ ] **Step 1: Create components/checkout/AddressForm.tsx**

```typescript
'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Input } from '@/components/ui/Input'

const schema = z.object({
  nome: z.string().min(3, 'Nome obrigatório'),
  cpf: z.string().min(11, 'CPF inválido'),
  cep: z.string().length(8, 'CEP deve ter 8 dígitos'),
  logradouro: z.string().min(3, 'Endereço obrigatório'),
  numero: z.string().min(1, 'Número obrigatório'),
  complemento: z.string().optional(),
  bairro: z.string().min(2, 'Bairro obrigatório'),
  cidade: z.string().min(2, 'Cidade obrigatória'),
  uf: z.string().length(2, 'UF deve ter 2 letras'),
})

export type AddressFormData = z.infer<typeof schema>

type AddressFormProps = {
  onSubmit: (data: AddressFormData) => void
}

export function AddressForm({ onSubmit }: AddressFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AddressFormData>({ resolver: zodResolver(schema) })

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
      <div className="grid grid-cols-2 gap-4">
        <div className="col-span-2">
          <Input
            label="Nome completo"
            placeholder="Carlos Silva"
            error={errors.nome?.message}
            {...register('nome')}
          />
        </div>
        <Input
          label="CPF"
          placeholder="000.000.000-00"
          error={errors.cpf?.message}
          {...register('cpf')}
        />
        <Input
          label="CEP"
          placeholder="00000-000"
          error={errors.cep?.message}
          {...register('cep')}
        />
        <div className="col-span-2">
          <Input
            label="Logradouro"
            placeholder="Rua das Peças"
            error={errors.logradouro?.message}
            {...register('logradouro')}
          />
        </div>
        <Input
          label="Número"
          placeholder="123"
          error={errors.numero?.message}
          {...register('numero')}
        />
        <Input
          label="Complemento"
          placeholder="Apto 45 (opcional)"
          {...register('complemento')}
        />
        <Input
          label="Bairro"
          placeholder="Centro"
          error={errors.bairro?.message}
          {...register('bairro')}
        />
        <Input
          label="Cidade"
          placeholder="São Paulo"
          error={errors.cidade?.message}
          {...register('cidade')}
        />
        <div>
          <label className="block font-inter text-sm font-500 text-dark mb-1">UF</label>
          <select
            className="w-full border border-gray-border px-4 py-3 font-inter text-sm text-dark focus:outline-none focus:border-dark"
            {...register('uf')}
          >
            <option value="">UF</option>
            {['SP', 'RJ', 'MG', 'RS', 'PR', 'SC', 'BA', 'CE', 'PE', 'GO'].map((uf) => (
              <option key={uf} value={uf}>{uf}</option>
            ))}
          </select>
          {errors.uf && <p className="font-inter text-xs text-primary mt-1">{errors.uf.message}</p>}
        </div>
      </div>

      <button
        type="submit"
        className="bg-primary hover:bg-primary-dark text-white font-rajdhani font-700 py-4 tracking-wider transition-colors mt-2"
      >
        CONTINUAR →
      </button>
    </form>
  )
}
```

- [ ] **Step 2: Create components/checkout/DeliveryStep.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'

type DeliveryStepProps = {
  onNext: () => void
}

const SHIPPING_OPTIONS = [
  { id: 'sedex', label: 'SEDEX', eta: '1–2 dias úteis', price: 'R$ 29,90' },
  { id: 'pac', label: 'PAC', eta: '5–8 dias úteis', price: 'R$ 14,90' },
]

export function DeliveryStep({ onNext }: DeliveryStepProps) {
  const [selected, setSelected] = useState('sedex')

  return (
    <div className="flex flex-col gap-6">
      <h3 className="font-rajdhani font-600 text-dark tracking-wider">OPÇÕES DE ENTREGA</h3>

      <div className="space-y-3">
        {SHIPPING_OPTIONS.map((opt) => (
          <label
            key={opt.id}
            className={`flex items-center gap-4 border p-4 cursor-pointer transition-colors ${
              selected === opt.id ? 'border-primary' : 'border-gray-border hover:border-dark'
            }`}
          >
            <input
              type="radio"
              name="shipping"
              value={opt.id}
              checked={selected === opt.id}
              onChange={() => setSelected(opt.id)}
              className="accent-primary"
            />
            <div className="flex-1">
              <p className="font-rajdhani font-600 text-dark tracking-wide">{opt.label}</p>
              <p className="font-inter text-sm text-gray-500">{opt.eta}</p>
            </div>
            <span className="font-rajdhani font-700 text-dark">{opt.price}</span>
          </label>
        ))}
      </div>

      <button
        onClick={onNext}
        className="bg-primary hover:bg-primary-dark text-white font-rajdhani font-700 py-4 tracking-wider transition-colors"
      >
        CONTINUAR →
      </button>
    </div>
  )
}
```

- [ ] **Step 3: Create components/checkout/PaymentStep.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'

type PaymentStepProps = {
  onNext: () => void
}

type PaymentMethod = 'card' | 'pix'

export function PaymentStep({ onNext }: PaymentStepProps) {
  const [method, setMethod] = useState<PaymentMethod>('card')
  const [cardNumber, setCardNumber] = useState('')
  const [cardName, setCardName] = useState('')
  const [cardExpiry, setCardExpiry] = useState('')
  const [cardCvv, setCardCvv] = useState('')

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    onNext()
  }

  return (
    <div className="flex flex-col gap-6">
      <h3 className="font-rajdhani font-600 text-dark tracking-wider">FORMA DE PAGAMENTO</h3>

      {/* Method tabs */}
      <div className="flex border-b border-gray-border">
        {(['card', 'pix'] as PaymentMethod[]).map((m) => (
          <button
            key={m}
            type="button"
            onClick={() => setMethod(m)}
            className={`font-rajdhani font-600 text-sm tracking-wider px-6 py-3 border-b-2 transition-colors ${
              method === m
                ? 'border-primary text-primary'
                : 'border-transparent text-gray-500 hover:text-dark'
            }`}
          >
            {m === 'card' ? 'CARTÃO DE CRÉDITO' : 'PIX'}
          </button>
        ))}
      </div>

      {method === 'card' && (
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <label className="block font-inter text-sm font-500 text-dark mb-1">Número do cartão</label>
            <input
              type="text"
              value={cardNumber}
              onChange={(e) => setCardNumber(e.target.value)}
              placeholder="0000 0000 0000 0000"
              maxLength={19}
              required
              className="w-full border border-gray-border px-4 py-3 font-inter text-sm text-dark placeholder:text-gray-400 focus:outline-none focus:border-dark"
            />
          </div>
          <div>
            <label className="block font-inter text-sm font-500 text-dark mb-1">Nome no cartão</label>
            <input
              type="text"
              value={cardName}
              onChange={(e) => setCardName(e.target.value)}
              placeholder="CARLOS SILVA"
              required
              className="w-full border border-gray-border px-4 py-3 font-inter text-sm text-dark placeholder:text-gray-400 focus:outline-none focus:border-dark uppercase"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block font-inter text-sm font-500 text-dark mb-1">Validade</label>
              <input
                type="text"
                value={cardExpiry}
                onChange={(e) => setCardExpiry(e.target.value)}
                placeholder="MM/AA"
                maxLength={5}
                required
                className="w-full border border-gray-border px-4 py-3 font-inter text-sm text-dark placeholder:text-gray-400 focus:outline-none focus:border-dark"
              />
            </div>
            <div>
              <label className="block font-inter text-sm font-500 text-dark mb-1">CVV</label>
              <input
                type="text"
                value={cardCvv}
                onChange={(e) => setCardCvv(e.target.value)}
                placeholder="000"
                maxLength={4}
                required
                className="w-full border border-gray-border px-4 py-3 font-inter text-sm text-dark placeholder:text-gray-400 focus:outline-none focus:border-dark"
              />
            </div>
          </div>
          <button
            type="submit"
            className="bg-primary hover:bg-primary-dark text-white font-rajdhani font-700 py-4 tracking-wider transition-colors mt-2"
          >
            CONFIRMAR PEDIDO →
          </button>
        </form>
      )}

      {method === 'pix' && (
        <div className="flex flex-col items-center gap-4">
          <div className="w-48 h-48 bg-gray-light border border-gray-border flex items-center justify-center">
            <p className="font-inter text-xs text-gray-500 text-center px-4">QR Code PIX<br />placeholder</p>
          </div>
          <p className="font-inter text-sm text-gray-600 text-center max-w-sm">
            Escaneie o QR Code com o app do seu banco para pagar via PIX. Aprovação em até 30 segundos.
          </p>
          <button
            onClick={onNext}
            className="bg-primary hover:bg-primary-dark text-white font-rajdhani font-700 px-10 py-4 tracking-wider transition-colors"
          >
            PAGAMENTO REALIZADO →
          </button>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 4: Create components/checkout/ConfirmationStep.tsx (Client)**

```typescript
'use client'

import { useEffect } from 'react'
import Link from 'next/link'
import { useCartStore } from '@/store/cartStore'

export function ConfirmationStep() {
  const clearCart = useCartStore((s) => s.clearCart)

  useEffect(() => {
    clearCart()
  }, [clearCart])

  const orderNumber = `AH-2024-${String(Math.floor(Math.random() * 9000) + 1000)}`

  return (
    <div className="text-center py-12">
      <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
        <span className="text-4xl">✓</span>
      </div>

      <h2 className="font-rajdhani font-700 text-2xl text-dark tracking-wider mb-2">
        PEDIDO CONFIRMADO!
      </h2>
      <p className="font-inter text-gray-500 mb-2">Pedido #{orderNumber}</p>
      <p className="font-inter text-sm text-gray-500 mb-8">
        Você receberá um e-mail com os detalhes do pedido em breve.
      </p>

      <div className="bg-gray-light border border-gray-border p-6 max-w-sm mx-auto mb-8 text-left">
        <p className="font-rajdhani font-600 text-sm text-dark tracking-wider mb-3">RESUMO</p>
        <div className="space-y-2">
          <div className="flex justify-between">
            <span className="font-inter text-sm text-gray-600">Pedido</span>
            <span className="font-inter text-sm text-dark">#{orderNumber}</span>
          </div>
          <div className="flex justify-between">
            <span className="font-inter text-sm text-gray-600">Status</span>
            <span className="font-inter text-sm text-green-600">Confirmado</span>
          </div>
          <div className="flex justify-between">
            <span className="font-inter text-sm text-gray-600">Previsão</span>
            <span className="font-inter text-sm text-dark">3–5 dias úteis</span>
          </div>
        </div>
      </div>

      <Link
        href="/orders"
        className="inline-block bg-primary hover:bg-primary-dark text-white font-rajdhani font-700 px-10 py-4 tracking-wider transition-colors mr-4"
      >
        VER MEUS PEDIDOS
      </Link>
      <Link
        href="/catalog"
        className="inline-block border border-gray-border hover:border-dark text-dark font-rajdhani font-600 px-10 py-4 tracking-wider transition-colors"
      >
        CONTINUAR COMPRANDO
      </Link>
    </div>
  )
}
```

- [ ] **Step 5: Create components/checkout/CheckoutStepper.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'
import { AddressForm, type AddressFormData } from './AddressForm'
import { DeliveryStep } from './DeliveryStep'
import { PaymentStep } from './PaymentStep'
import { ConfirmationStep } from './ConfirmationStep'
import { useCartStore } from '@/store/cartStore'

const STEPS = [
  { number: 1, label: 'Endereço' },
  { number: 2, label: 'Entrega' },
  { number: 3, label: 'Pagamento' },
  { number: 4, label: 'Confirmação' },
]

export function CheckoutStepper() {
  const [step, setStep] = useState(1)
  const { items, total } = useCartStore()

  const shipping = total >= 299 ? 0 : 29.9
  const finalTotal = total + shipping

  return (
    <div className="flex gap-12 items-start">
      {/* Main stepper */}
      <div className="flex-1">
        {/* Step indicators */}
        <div className="flex items-center mb-10">
          {STEPS.map((s, i) => (
            <div key={s.number} className="flex items-center">
              <div className="flex flex-col items-center">
                <div
                  className={`w-9 h-9 rounded-full flex items-center justify-center font-rajdhani font-700 text-sm transition-colors ${
                    step > s.number
                      ? 'bg-green-500 text-white'
                      : step === s.number
                      ? 'bg-primary text-white'
                      : 'bg-gray-light border border-gray-border text-gray-400'
                  }`}
                >
                  {step > s.number ? '✓' : s.number}
                </div>
                <span className={`font-inter text-xs mt-1 ${step === s.number ? 'text-primary' : 'text-gray-400'}`}>
                  {s.label}
                </span>
              </div>
              {i < STEPS.length - 1 && (
                <div className={`h-px w-20 mx-2 mb-4 transition-colors ${step > s.number ? 'bg-green-500' : 'bg-gray-border'}`} />
              )}
            </div>
          ))}
        </div>

        {/* Step content */}
        {step === 1 && (
          <AddressForm onSubmit={(_data: AddressFormData) => setStep(2)} />
        )}
        {step === 2 && <DeliveryStep onNext={() => setStep(3)} />}
        {step === 3 && <PaymentStep onNext={() => setStep(4)} />}
        {step === 4 && <ConfirmationStep />}
      </div>

      {/* Order summary sidebar — hidden on step 4 */}
      {step < 4 && (
        <div className="w-72 bg-gray-light border border-gray-border p-6">
          <h3 className="font-rajdhani font-700 text-dark tracking-wider mb-4">RESUMO</h3>
          <div className="space-y-2 mb-4">
            {items.map(({ product, qty }) => (
              <div key={product.id} className="flex justify-between">
                <span className="font-inter text-xs text-gray-600 flex-1 line-clamp-1">{product.name} x{qty}</span>
                <span className="font-rajdhani font-600 text-dark text-sm ml-2">
                  R$ {(product.price * qty).toFixed(2).replace('.', ',')}
                </span>
              </div>
            ))}
          </div>
          <div className="border-t border-gray-border pt-3 space-y-2">
            <div className="flex justify-between">
              <span className="font-inter text-sm text-gray-600">Subtotal</span>
              <span className="font-rajdhani font-600 text-dark">
                R$ {total.toFixed(2).replace('.', ',')}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="font-inter text-sm text-gray-600">Frete</span>
              <span className="font-rajdhani font-600 text-dark">
                {shipping === 0 ? <span className="text-green-600">GRÁTIS</span> : `R$ ${shipping.toFixed(2).replace('.', ',')}`}
              </span>
            </div>
            <div className="flex justify-between pt-2 border-t border-gray-border">
              <span className="font-rajdhani font-700 text-dark">TOTAL</span>
              <span className="font-rajdhani font-700 text-primary">
                R$ {finalTotal.toFixed(2).replace('.', ',')}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 6: Create app/(customer)/checkout/page.tsx**

```typescript
import { CheckoutStepper } from '@/components/checkout/CheckoutStepper'
import Link from 'next/link'

export default function CheckoutPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8">
      <nav className="flex items-center gap-2 font-inter text-sm text-gray-500 mb-6">
        <Link href="/" className="hover:text-primary transition-colors">Início</Link>
        <span>/</span>
        <Link href="/cart" className="hover:text-primary transition-colors">Carrinho</Link>
        <span>/</span>
        <span className="text-dark">Checkout</span>
      </nav>

      <h1 className="font-rajdhani font-700 text-2xl text-dark tracking-wider mb-8">CHECKOUT</h1>

      <CheckoutStepper />
    </div>
  )
}
```

- [ ] **Step 7: Verify checkout flow**

```bash
npm run dev
```

1. Add a product to cart, navigate to `/checkout`
2. Confirm step indicator shows Step 1 active and order summary sidebar visible
3. Fill address form with invalid data → confirm Zod error messages appear
4. Fill valid data → click "CONTINUAR" → Step 2 (delivery options)
5. Select PAC → click "CONTINUAR" → Step 3 (payment)
6. Fill card form → "CONFIRMAR PEDIDO" → Step 4 (confirmation)
7. Confirm cart badge in header resets to 0 after confirmation

---

### Task 3.4: Account page

**Files:**
- Create: `components/account/AccountSidebar.tsx`
- Create: `components/account/OrdersTab.tsx`
- Create: `components/account/ProfileTab.tsx`
- Create: `app/(customer)/account/page.tsx`

- [ ] **Step 1: Create components/account/AccountSidebar.tsx (Client)**

```typescript
'use client'

import { useAuthStore } from '@/store/authStore'
import { useRouter } from 'next/navigation'

type Tab = 'pedidos' | 'dados' | 'enderecos' | 'seguranca'

type AccountSidebarProps = {
  activeTab: Tab
  onTabChange: (tab: Tab) => void
}

const NAV_ITEMS: { id: Tab; label: string }[] = [
  { id: 'pedidos', label: 'Meus Pedidos' },
  { id: 'dados', label: 'Dados Pessoais' },
  { id: 'enderecos', label: 'Endereços' },
  { id: 'seguranca', label: 'Segurança' },
]

export function AccountSidebar({ activeTab, onTabChange }: AccountSidebarProps) {
  const { user, logout } = useAuthStore()
  const router = useRouter()

  function handleLogout() {
    logout()
    router.push('/login')
  }

  return (
    <aside className="w-60 flex-shrink-0">
      {/* User info */}
      <div className="flex items-center gap-3 mb-6">
        <div className="w-12 h-12 bg-primary rounded-full flex items-center justify-center text-white font-rajdhani font-700">
          {user?.initials ?? 'U'}
        </div>
        <div>
          <p className="font-inter text-sm font-500 text-dark">{user?.name ?? 'Usuário'}</p>
          <p className="font-inter text-xs text-gray-500">{user?.email ?? ''}</p>
        </div>
      </div>

      {/* Nav */}
      <nav className="space-y-1 mb-6">
        {NAV_ITEMS.map((item) => (
          <button
            key={item.id}
            onClick={() => onTabChange(item.id)}
            className={`block w-full text-left font-inter text-sm px-4 py-3 transition-colors ${
              activeTab === item.id
                ? 'bg-primary text-white font-500'
                : 'text-gray-600 hover:text-dark hover:bg-gray-light'
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

      <button
        onClick={handleLogout}
        className="font-inter text-sm text-gray-500 hover:text-red-500 transition-colors px-4"
      >
        Sair da conta
      </button>
    </aside>
  )
}
```

- [ ] **Step 2: Create components/account/OrdersTab.tsx**

```typescript
import { customerOrders } from '@/lib/data/orders'

const STATUS_STYLES: Record<string, { color: string; bg: string }> = {
  'Entregue': { color: 'text-green-700', bg: 'bg-green-100' },
  'Em trânsito': { color: 'text-blue-700', bg: 'bg-blue-100' },
  'Processando': { color: 'text-yellow-700', bg: 'bg-yellow-100' },
  'Cancelado': { color: 'text-red-700', bg: 'bg-red-100' },
  'Aguardando pagamento': { color: 'text-orange-700', bg: 'bg-orange-100' },
}

export function OrdersTab() {
  return (
    <div>
      <h2 className="font-rajdhani font-700 text-lg text-dark tracking-wider mb-6">MEUS PEDIDOS</h2>

      {/* Table header */}
      <div className="grid grid-cols-[1fr_1fr_2fr_1fr_auto] gap-4 px-4 py-3 border-b border-gray-border">
        {['PEDIDO', 'DATA', 'ITENS', 'TOTAL', 'STATUS'].map((h) => (
          <span key={h} className="font-rajdhani font-600 text-xs text-gray-500 tracking-wider">{h}</span>
        ))}
      </div>

      {customerOrders.map((order) => {
        const style = STATUS_STYLES[order.status] ?? { color: 'text-gray-700', bg: 'bg-gray-100' }
        return (
          <div
            key={order.id}
            className="grid grid-cols-[1fr_1fr_2fr_1fr_auto] gap-4 px-4 py-4 border-b border-gray-border items-center"
          >
            <span className="font-rajdhani font-600 text-dark text-sm">{order.id}</span>
            <span className="font-inter text-sm text-gray-500">{order.date}</span>
            <span className="font-inter text-sm text-gray-600">{order.items}</span>
            <span className="font-rajdhani font-700 text-dark">{order.total}</span>
            <span className={`font-inter text-xs px-2 py-1 ${style.bg} ${style.color} whitespace-nowrap`}>
              {order.status}
            </span>
          </div>
        )
      })}
    </div>
  )
}
```

- [ ] **Step 3: Create components/account/ProfileTab.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import { Input } from '@/components/ui/Input'

export function ProfileTab() {
  const { user } = useAuthStore()
  const [saved, setSaved] = useState(false)

  function handleSave(e: React.FormEvent) {
    e.preventDefault()
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <div>
      <h2 className="font-rajdhani font-700 text-lg text-dark tracking-wider mb-6">DADOS PESSOAIS</h2>
      <form onSubmit={handleSave} className="flex flex-col gap-4 max-w-md">
        <Input label="Nome completo" defaultValue={user?.name ?? ''} />
        <Input label="E-mail" type="email" defaultValue={user?.email ?? ''} />
        <Input label="Telefone" defaultValue={user?.phone ?? ''} />
        <Input label="CPF" defaultValue={user?.cpf ?? ''} disabled />

        <button
          type="submit"
          className="bg-primary hover:bg-primary-dark text-white font-rajdhani font-700 py-3 tracking-wider transition-colors mt-2"
        >
          {saved ? 'SALVO ✓' : 'SALVAR ALTERAÇÕES'}
        </button>
      </form>
    </div>
  )
}
```

- [ ] **Step 4: Create app/(customer)/account/page.tsx (Client)**

```typescript
'use client'

import { useState } from 'react'
import { AccountSidebar } from '@/components/account/AccountSidebar'
import { OrdersTab } from '@/components/account/OrdersTab'
import { ProfileTab } from '@/components/account/ProfileTab'

type Tab = 'pedidos' | 'dados' | 'enderecos' | 'seguranca'

export default function AccountPage() {
  const [activeTab, setActiveTab] = useState<Tab>('pedidos')

  return (
    <div className="max-w-7xl mx-auto px-6 py-8">
      <h1 className="font-rajdhani font-700 text-2xl text-dark tracking-wider mb-8">MINHA CONTA</h1>

      <div className="flex gap-10">
        <AccountSidebar activeTab={activeTab} onTabChange={setActiveTab} />

        <div className="flex-1">
          {activeTab === 'pedidos' && <OrdersTab />}
          {activeTab === 'dados' && <ProfileTab />}
          {activeTab === 'enderecos' && (
            <div>
              <h2 className="font-rajdhani font-700 text-lg text-dark tracking-wider mb-4">ENDEREÇOS</h2>
              <p className="font-inter text-sm text-gray-500">Nenhum endereço cadastrado.</p>
            </div>
          )}
          {activeTab === 'seguranca' && (
            <div>
              <h2 className="font-rajdhani font-700 text-lg text-dark tracking-wider mb-4">SEGURANÇA</h2>
              <p className="font-inter text-sm text-gray-500">Gerenciamento de senha em breve.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 5: Verify account page**

```bash
npm run dev
```

1. Navigate to `/login` → submit → confirm redirect to `/account`
2. Confirm sidebar shows "Carlos Silva / carlos@email.com" with avatar "CS"
3. "Meus Pedidos" tab shows 3 order rows with colored status badges
4. Switch to "Dados Pessoais" → form prefilled with mock user data
5. Click "SALVAR ALTERAÇÕES" → button shows "SALVO ✓" for 2 seconds
6. Click "Sair da conta" → redirect to `/login`, header shows "Entrar"

---

### Task 3.5: Orders list page

**Files:**
- Create: `app/(customer)/orders/page.tsx`

- [ ] **Step 1: Create app/(customer)/orders/page.tsx**

```typescript
import { customerOrders } from '@/lib/data/orders'
import Link from 'next/link'

const STATUS_STYLES: Record<string, { color: string; bg: string }> = {
  'Entregue': { color: 'text-green-700', bg: 'bg-green-100' },
  'Em trânsito': { color: 'text-blue-700', bg: 'bg-blue-100' },
  'Processando': { color: 'text-yellow-700', bg: 'bg-yellow-100' },
  'Cancelado': { color: 'text-red-700', bg: 'bg-red-100' },
  'Aguardando pagamento': { color: 'text-orange-700', bg: 'bg-orange-100' },
}

export default function OrdersPage() {
  return (
    <div className="max-w-7xl mx-auto px-6 py-8">
      <nav className="flex items-center gap-2 font-inter text-sm text-gray-500 mb-6">
        <Link href="/" className="hover:text-primary transition-colors">Início</Link>
        <span>/</span>
        <span className="text-dark">Meus Pedidos</span>
      </nav>

      <h1 className="font-rajdhani font-700 text-2xl text-dark tracking-wider mb-8">MEUS PEDIDOS</h1>

      <div className="border border-gray-border">
        <div className="grid grid-cols-[1fr_1fr_2fr_1fr_1fr_auto] gap-4 px-6 py-3 border-b border-gray-border bg-gray-light">
          {['PEDIDO', 'DATA', 'ITENS', 'TOTAL', 'STATUS', ''].map((h) => (
            <span key={h} className="font-rajdhani font-600 text-xs text-gray-500 tracking-wider">{h}</span>
          ))}
        </div>

        {customerOrders.map((order) => {
          const style = STATUS_STYLES[order.status] ?? { color: 'text-gray-700', bg: 'bg-gray-100' }
          return (
            <div
              key={order.id}
              className="grid grid-cols-[1fr_1fr_2fr_1fr_1fr_auto] gap-4 px-6 py-5 border-b border-gray-border items-center last:border-0"
            >
              <span className="font-rajdhani font-600 text-dark text-sm">{order.id}</span>
              <span className="font-inter text-sm text-gray-500">{order.date}</span>
              <span className="font-inter text-sm text-gray-600">{order.items}</span>
              <span className="font-rajdhani font-700 text-dark">{order.total}</span>
              <span className={`font-inter text-xs px-2 py-1 w-fit ${style.bg} ${style.color}`}>
                {order.status}
              </span>
              <button className="font-rajdhani font-600 text-xs text-primary hover:text-primary-dark tracking-wider transition-colors">
                VER DETALHES
              </button>
            </div>
          )
        })}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Verify orders page**

```bash
npm run dev
```

Open `http://localhost:3000/orders`. Confirm 3 orders with status badges (green for Entregue, blue for Em trânsito, yellow for Processando).

---

## Verification

After Plan 3 is complete:

```bash
npm run build    # No TS errors
npm run dev
```

Full customer flow:
1. `/login` → submit → `/account` (logged in)
2. `/catalog` → product → "Adicionar ao Carrinho" → `/cart`
3. Cart: edit qty, apply coupon "PROMO10" → "FINALIZAR PEDIDO" → `/checkout`
4. Complete all 4 steps → confirmation page, cart resets to 0
5. `/orders` → 3 mock orders with colored status badges
6. `/account` → tab switching works, logout redirects to `/login`

**Next:** Execute Plan 4 (Admin Panel).
