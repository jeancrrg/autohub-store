import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '@/lib/api/client'
import { useCreateProduct } from '../useCreateProduct'

function wrapper({ children }: { children: React.ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useCreateProduct', () => {
    it('posts product data and returns the created id', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onPost('/api/v1/catalog/products').reply(201, { id: 'new-product-id' })

        const { result } = renderHook(() => useCreateProduct(), { wrapper })

        result.current.mutate({
            name: 'Rodas Aro 18', description: 'desc', price: 100, stockQuantity: 5, categoryId: 'cat-1',
        })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data?.id).toBe('new-product-id')
    })
})
