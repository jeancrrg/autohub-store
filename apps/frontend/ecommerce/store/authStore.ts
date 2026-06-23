import { create } from 'zustand'
import type { MockUser } from '@/types/order'

const MOCK_USER: MockUser = {
    name: 'Carlos Silva',
    email: 'carlos@email.com',
    initials: 'CS',
    phone: '(11) 99999-9999',
    cpf: '123.456.789-00',
}

type AuthStore = {
    isLoggedIn: boolean
    user: MockUser | null
    login: () => void
    logout: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
    isLoggedIn: false,
    user: null,
    login: () => set({ isLoggedIn: true, user: MOCK_USER }),
    logout: () => set({ isLoggedIn: false, user: null }),
}))
