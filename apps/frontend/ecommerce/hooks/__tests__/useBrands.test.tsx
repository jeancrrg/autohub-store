import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '@/lib/api/client'
import { useBrands } from '../useBrands'

function wrapper({ children }: { children: React.ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useBrands', () => {
    it('retorna a lista de marcas com id, name e slug', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onGet('/api/v1/catalog/brands').reply(200, [
            { id: '3fa85f64-5717-4562-b3fc-2c963f66afa6', name: 'Akrapovič', slug: 'akrapovic' },
            { id: '4fa85f64-5717-4562-b3fc-2c963f66afa7', name: 'BBS', slug: 'bbs' },
        ])

        const { result } = renderHook(() => useBrands(), { wrapper })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data).toHaveLength(2)
        expect(result.current.data?.[0].name).toBe('Akrapovič')
        expect(result.current.data?.[1].slug).toBe('bbs')
    })
})
