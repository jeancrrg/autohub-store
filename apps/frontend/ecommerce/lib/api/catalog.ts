import { apiClient } from './client'
import type { Product } from '@/types/product'
import type { AdminCategory } from '@/types/category'

type ProductImageResponse = {
    id: string
    url: string
    primary: boolean
}

type ProductResponse = {
    id: string
    sku: string
    slug: string
    name: string
    description: string
    price: number
    stockQuantity: number
    categoryId: string
    categoryName: string
    status: 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK'
    images: ProductImageResponse[]
}

type PageResponse<T> = {
    content: T[]
    totalElements: number
    totalPages: number
    number: number
    size: number
}

const DEFAULT_PAGE_SIZE = 100

function toProduct(response: ProductResponse): Product {
    return {
        id: response.id,
        sku: response.sku,
        slug: response.slug,
        name: response.name,
        brand: response.categoryName,
        price: response.price,
        tagColor: '',
        stars: 5,
        reviews: 0,
        installments: 1,
        inStock: response.status === 'ACTIVE' && response.stockQuantity > 0,
        description: response.description,
        specs: {},
        category: response.categoryName,
        images: response.images.map((img) => img.url),
    }
}

export async function fetchProducts(): Promise<Product[]> {
    const { data } = await apiClient.get<PageResponse<ProductResponse>>('/api/v1/catalog/products', {
        params: { size: DEFAULT_PAGE_SIZE },
    })
    return data.content.map(toProduct)
}

export async function fetchProduct(id: string): Promise<Product> {
    const { data } = await apiClient.get<ProductResponse>(`/api/v1/catalog/products/${id}`)
    return toProduct(data)
}

export async function fetchProductBySlug(slug: string): Promise<Product> {
    const { data } = await apiClient.get<ProductResponse>(`/api/v1/catalog/products/slug/${slug}`)
    return toProduct(data)
}

type CategoryResponse = {
    id: string
    name: string
    slug: string
    productCount: number
    createdAt: string
}

export async function fetchCategories(): Promise<AdminCategory[]> {
    const { data } = await apiClient.get<CategoryResponse[]>('/api/v1/catalog/categories')
    return data
}
