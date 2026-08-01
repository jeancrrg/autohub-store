import { useQuery } from '@tanstack/react-query'
import { fetchProduct } from '@/lib/api/catalog'

export function useProduct(id: string) {
    return useQuery({
        queryKey: ['products', id],
        queryFn: () => fetchProduct(id),
        enabled: Boolean(id),
    })
}
