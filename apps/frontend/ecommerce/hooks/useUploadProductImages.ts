import { useMutation, useQueryClient } from '@tanstack/react-query'
import { uploadProductImages } from '@/lib/api/adminCatalog'

export function useUploadProductImages(productId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (files: File[]) => uploadProductImages(productId, files),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['products', productId] })
        },
    })
}
