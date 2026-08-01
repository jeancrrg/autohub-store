import axios from 'axios'

export const apiClient = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_URL,
    withCredentials: true,
})

let refreshPromise: Promise<void> | null = null

function refreshSession(): Promise<void> {
    if (!refreshPromise) {
        refreshPromise = apiClient
            .post('/api/v1/auth/refresh')
            .then(() => undefined)
            .finally(() => {
                refreshPromise = null
            })
    }
    return refreshPromise
}

apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config
        const isRefreshCall = originalRequest?.url === '/api/v1/auth/refresh'

        if (error.response?.status === 401 && !originalRequest._retry && !isRefreshCall) {
            originalRequest._retry = true
            try {
                await refreshSession()
                return apiClient(originalRequest)
            } catch (refreshError) {
                return Promise.reject(refreshError)
            }
        }

        return Promise.reject(error)
    }
)
