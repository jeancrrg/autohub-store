import { describe, it, expect } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '@/lib/api/client'
import { useSession } from '../useSession'

function wrapper({ children }: { children: React.ReactNode }) {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    return <QueryClientProvider client={client}>{children}</QueryClientProvider>
}

describe('useSession', () => {
    it('returns the logged-in user when the session cookie is valid', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onGet('/api/v1/users/me').reply(200, {
            id: 'u1', name: 'Carlos Silva', email: 'carlos@email.com', phone: '(11) 99999-9999', cpf: '123.456.789-00',
        })

        const { result } = renderHook(() => useSession(), { wrapper })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data?.email).toBe('carlos@email.com')
    })

    it('returns null when there is no valid session (401)', async () => {
        const mock = new MockAdapter(apiClient)
        mock.onGet('/api/v1/users/me').reply(401)

        const { result } = renderHook(() => useSession(), { wrapper })

        await waitFor(() => expect(result.current.isSuccess).toBe(true))
        expect(result.current.data).toBeNull()
    })
})
