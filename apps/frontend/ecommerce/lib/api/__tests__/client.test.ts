import { describe, it, expect, beforeEach } from 'vitest'
import MockAdapter from 'axios-mock-adapter'
import { apiClient } from '../client'

describe('apiClient 401 refresh interceptor', () => {
    let mock: MockAdapter

    beforeEach(() => {
        mock = new MockAdapter(apiClient)
    })

    it('retries the original request once after a successful refresh', async () => {
        mock
            .onGet('/api/v1/users/me')
            .replyOnce(401)
            .onPost('/api/v1/auth/refresh')
            .replyOnce(200)
            .onGet('/api/v1/users/me')
            .replyOnce(200, { id: 'u1', email: 'user@email.com' })

        const response = await apiClient.get('/api/v1/users/me')

        expect(response.status).toBe(200)
        expect(response.data.id).toBe('u1')
    })

    it('propagates the error when refresh also fails', async () => {
        mock.onGet('/api/v1/users/me').reply(401)
        mock.onPost('/api/v1/auth/refresh').reply(401)

        await expect(apiClient.get('/api/v1/users/me')).rejects.toMatchObject({
            response: { status: 401 },
        })
    })
})
