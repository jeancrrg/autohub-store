import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '@/lib/api/client'
import { useCategories } from '../useCategories'

function wrapper({ children }: { children: React.ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useCategories', () => {
    it('retorna a lista de categorias com productCount e sem parentId', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onGet('/api/v1/catalog/categories').reply(200, [
            {
                id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
                name: 'Rodas',
                slug: 'rodas',
                productCount: 12,
                createdAt: '2026-08-07T12:00:00Z',
            },
            {
                id: '4fa85f64-5717-4562-b3fc-2c963f66afa7',
                name: 'Ofertas',
                slug: 'ofertas',
                productCount: 0,
                createdAt: '2026-08-07T12:00:00Z',
            },
        ])

        const { result } = renderHook(() => useCategories(), { wrapper })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data).toHaveLength(2)
        expect(result.current.data?.[0].slug).toBe('rodas')
        expect(result.current.data?.[0].productCount).toBe(12)
        expect(result.current.data?.[0]).not.toHaveProperty('parentId')
    })
})
