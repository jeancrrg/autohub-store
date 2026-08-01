import { apiClient } from './client'

export type SessionUser = {
    id: string
    name: string
    email: string
    phone: string
    cpf: string
}

type UserResponse = {
    id: string
    email: string
    fullName: string
    status: 'ACTIVE' | 'INACTIVE' | 'BLOCKED'
    role: 'CUSTOMER' | 'ADMIN'
}

function toSessionUser(response: UserResponse): SessionUser {
    return {
        id: response.id,
        name: response.fullName,
        email: response.email,
        phone: '',
        cpf: '',
    }
}

export async function fetchSession(): Promise<SessionUser | null> {
    try {
        const { data } = await apiClient.get<UserResponse>('/api/v1/users/me')
        return toSessionUser(data)
    } catch (error: any) {
        if (error.response?.status === 401) {
            return null
        }
        throw error
    }
}

export async function login(email: string, password: string): Promise<void> {
    await apiClient.post('/api/v1/auth/login', { email, password })
}

export async function logout(): Promise<void> {
    await apiClient.post('/api/v1/auth/logout')
}
