import { useQuery } from '@tanstack/react-query'
import { fetchProductBySlug } from '@/lib/api/catalog'

export function useProductBySlug(slug: string) {
    return useQuery({
        queryKey: ['products', 'slug', slug],
        queryFn: () => fetchProductBySlug(slug),
        enabled: Boolean(slug),
    })
}
