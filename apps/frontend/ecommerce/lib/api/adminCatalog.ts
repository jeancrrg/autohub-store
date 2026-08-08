import { apiClient } from './client'
import type { AdminCategory } from '@/types/category'

export type CreateCategoryInput = {
    name: string
    slug: string
}

export async function createCategory(input: CreateCategoryInput): Promise<AdminCategory> {
    const { data } = await apiClient.post<AdminCategory>('/api/v1/catalog/categories', input)
    return data
}

export type CreateProductInput = {
    name: string
    sku?: string
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
