import { create } from 'zustand'
import type { Product } from '@/types/product'
import type { CartItem } from '@/types/cart'

type CartStore = {
    items: CartItem[]
    total: number
    count: number
    addItem: (product: Product, qty: number) => void
    removeItem: (id: string) => void
    updateQty: (id: string, qty: number) => void
    clearCart: () => void
}

function derive(items: CartItem[]) {
    return {
        total: items.reduce((sum, i) => sum + i.product.price * i.qty, 0),
        count: items.reduce((sum, i) => sum + i.qty, 0),
    }
}

export const useCartStore = create<CartStore>((set) => ({
    items: [],
    total: 0,
    count: 0,

    addItem: (product, qty) =>
        set((s) => {
            const existing = s.items.find((i) => i.product.id === product.id)
            const items = existing
                ? s.items.map((i) => (i.product.id === product.id ? { ...i, qty: i.qty + qty } : i))
                : [...s.items, { product, qty }]
            return { items, ...derive(items) }
        }),

    removeItem: (id) =>
        set((s) => {
            const items = s.items.filter((i) => i.product.id !== id)
            return { items, ...derive(items) }
        }),

    updateQty: (id, qty) =>
        set((s) => {
            const items = s.items.map((i) => (i.product.id === id ? { ...i, qty } : i))
            return { items, ...derive(items) }
        }),

    clearCart: () => set({ items: [], total: 0, count: 0 }),
}))
