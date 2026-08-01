import { act, renderHook } from '@testing-library/react'
import { useCartStore } from '../cartStore'
import type { Product } from '@/types/product'

const mockProduct: Product = {
    id: '11111111-1111-1111-1111-111111111111',
    name: 'Test Product',
    brand: 'Brand',
    price: 100,
    tagColor: '',
    stars: 5,
    reviews: 10,
    installments: 3,
    inStock: true,
    description: '',
    specs: {},
    category: 'Rodas',
    images: [],
}

describe('cartStore', () => {
    beforeEach(() => {
        useCartStore.setState({ items: [], total: 0, count: 0 })
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
        act(() => result.current.removeItem('11111111-1111-1111-1111-111111111111'))
        expect(result.current.items).toHaveLength(0)
    })

    it('updateQty updates item quantity', () => {
        const { result } = renderHook(() => useCartStore())
        act(() => result.current.addItem(mockProduct, 1))
        act(() => result.current.updateQty('11111111-1111-1111-1111-111111111111', 5))
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
