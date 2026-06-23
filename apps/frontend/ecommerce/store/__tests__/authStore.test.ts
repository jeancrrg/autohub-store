import { act, renderHook } from '@testing-library/react'
import { useAuthStore } from '../authStore'

describe('authStore', () => {
    beforeEach(() => {
        useAuthStore.setState({ isLoggedIn: false, user: null })
    })

    it('starts logged out', () => {
        const { result } = renderHook(() => useAuthStore())
        expect(result.current.isLoggedIn).toBe(false)
        expect(result.current.user).toBeNull()
    })

    it('login sets isLoggedIn and user', () => {
        const { result } = renderHook(() => useAuthStore())
        act(() => result.current.login())
        expect(result.current.isLoggedIn).toBe(true)
        expect(result.current.user?.name).toBe('Carlos Silva')
    })

    it('logout clears state', () => {
        const { result } = renderHook(() => useAuthStore())
        act(() => result.current.login())
        act(() => result.current.logout())
        expect(result.current.isLoggedIn).toBe(false)
        expect(result.current.user).toBeNull()
    })
})
