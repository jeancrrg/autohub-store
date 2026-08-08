import { useQuery } from '@tanstack/react-query'
import { fetchBrands } from '@/lib/api/catalog'

export function useBrands() {
    return useQuery({
        queryKey: ['brands'],
        queryFn: fetchBrands,
    })
}
