import { useQuery } from '@tanstack/react-query'
import { fetchSession } from '@/lib/api/auth'

export function useSession() {
    return useQuery({
        queryKey: ['session'],
        queryFn: fetchSession,
    })
}
