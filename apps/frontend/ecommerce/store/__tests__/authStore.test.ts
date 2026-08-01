import { describe, it, expect } from 'vitest'
import { useAuthStore } from '../authStore'

describe('authStore', () => {
    it('setUser updates the stored user', () => {
        const user = { id: 'u1', name: 'Carlos Silva', email: 'carlos@email.com', phone: '', cpf: '' }
        useAuthStore.getState().setUser(user)
        expect(useAuthStore.getState().user).toEqual(user)

        useAuthStore.getState().setUser(null)
        expect(useAuthStore.getState().user).toBeNull()
    })
})
