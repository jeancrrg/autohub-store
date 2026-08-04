export type ProductTag = 'OFERTA' | 'NOVO'

export type ProductImage = {
    id: string
    url: string
    isPrimary: boolean
}

export type Product = {
    id: string
    sku: string
    slug: string
    name: string
    brand: string
    price: number
    oldPrice?: number
    tag?: ProductTag
    tagColor: string
    stars: number
    reviews: number
    installments: number
    inStock: boolean
    description: string
    specs: Record<string, string>
    category: string
    images: string[]
}

export type Category = {
    id: string
    name: string
    icon: string
    count: number
}

export type Brand = {
    id: string
    name: string
}
