import { apiClient } from './client'

export type CreateProductInput = {
    name: string
    description: string
    price: number
    stockQuantity: number
    categoryId: string
}

export type ProductImageResponse = {
    id: string
    url: string
    primary: boolean
}

export async function createProduct(input: CreateProductInput): Promise<{ id: string }> {
    const { data } = await apiClient.post<{ id: string }>('/api/v1/catalog/products', input)
    return data
}

export async function uploadProductImages(productId: string, files: File[]): Promise<ProductImageResponse[]> {
    const formData = new FormData()
    files.forEach((file) => formData.append('files', file))

    const { data } = await apiClient.post<ProductImageResponse[]>(
        `/api/v1/catalog/products/${productId}/images`,
        formData,
        { headers: { 'Content-Type': 'multipart/form-data' } }
    )
    return data
}

export async function deleteProduct(productId: string): Promise<void> {
    await apiClient.delete(`/api/v1/catalog/products/${productId}`)
}
