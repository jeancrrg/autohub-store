import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '@/lib/api/client'
import { useProducts } from '../useProducts'

function wrapper({ children }: { children: React.ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useProducts', () => {
    it('maps a Page<ProductResponse> into Product[]', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onGet('/api/v1/catalog/products', { params: { size: 100 } }).reply(200, {
            content: [
                {
                    id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
                    name: 'Filtro de Ar K&N',
                    description: 'Filtro esportivo',
                    price: 299.9,
                    stockQuantity: 10,
                    categoryId: 'cat-1',
                    categoryName: 'Filtros',
                    status: 'ACTIVE',
                    images: [{ id: 'img-1', url: '/catalog-images/x.jpg', primary: true }],
                },
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 100,
        })

        const { result } = renderHook(() => useProducts(), { wrapper })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data).toHaveLength(1)
        expect(result.current.data?.[0].id).toBe('3fa85f64-5717-4562-b3fc-2c963f66afa6')
        expect(result.current.data?.[0].inStock).toBe(true)
    })
})
