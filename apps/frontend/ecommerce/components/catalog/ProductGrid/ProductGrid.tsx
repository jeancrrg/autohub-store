'use client'

import { useState } from 'react'
import type { Product } from '@/types/product'
import { ProductCard } from '../ProductCard/ProductCard'
import { FilterSidebar } from '../FilterSidebar/FilterSidebar'
import { SortBar } from '../SortBar/SortBar'
import { Pagination } from '../Pagination/Pagination'
import styles from './ProductGrid.module.css'

const ITEMS_PER_PAGE = 6

type Filters = {
    category: string
    brands: string[]
    minPrice: number
    maxPrice: number
    inStock: boolean
}

const DEFAULT_FILTERS: Filters = {
    category: '',
    brands: [],
    minPrice: 0,
    maxPrice: 999999,
    inStock: false,
}

export function ProductGrid({ allProducts }: { allProducts: Product[] }) {
    const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS)
    const [sortBy, setSortBy] = useState('relevance')
    const [page, setPage] = useState(1)

    const filtered = allProducts
        .filter((p) => !filters.category || p.category === filters.category)
        .filter(
            (p) =>
                filters.brands.length === 0 ||
                filters.brands.includes(p.brand.toLowerCase().replace(/[^a-z0-9]/g, ''))
        )
        .filter(
            (p) =>
                p.price >= filters.minPrice &&
                (filters.maxPrice === 999999 || p.price <= filters.maxPrice)
        )
        .filter((p) => !filters.inStock || p.inStock)

    const sorted = [...filtered].sort((a, b) => {
        if (sortBy === 'price-asc') return a.price - b.price
        if (sortBy === 'price-desc') return b.price - a.price
        if (sortBy === 'reviews') return b.reviews - a.reviews
        return 0
    })

    const totalPages = Math.max(1, Math.ceil(sorted.length / ITEMS_PER_PAGE))
    const paginated = sorted.slice((page - 1) * ITEMS_PER_PAGE, page * ITEMS_PER_PAGE)

    function handleFilterChange(next: Filters) {
        setFilters(next)
        setPage(1)
    }

    return (
        <div className={styles.wrapper}>
            <FilterSidebar filters={filters} onChange={handleFilterChange} />

            <div className={styles.main}>
                <SortBar count={sorted.length} sortBy={sortBy} onSortChange={setSortBy} />

                {paginated.length === 0 ? (
                    <div className={styles.empty}>
                        <p className={styles.emptyText}>
                            Nenhum produto encontrado com esses filtros.
                        </p>
                    </div>
                ) : (
                    <div className={styles.grid}>
                        {paginated.map((p) => (
                            <ProductCard key={p.id} product={p} />
                        ))}
                    </div>
                )}

                <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
            </div>
        </div>
    )
}
