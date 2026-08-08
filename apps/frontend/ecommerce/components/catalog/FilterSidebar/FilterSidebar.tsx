'use client'

import { useState } from 'react'
import { useCategories } from '@/hooks/useCategories'
import { useBrands } from '@/hooks/useBrands'
import styles from './FilterSidebar.module.css'

type Filters = {
    category: string
    brands: string[]
    minPrice: number
    maxPrice: number
    inStock: boolean
}

type FilterSidebarProps = {
    filters: Filters
    onChange: (filters: Filters) => void
}

const VISIBLE_BRANDS_LIMIT = 10

export function FilterSidebar({ filters, onChange }: FilterSidebarProps) {
    const { data: categories } = useCategories()
    const { data: allBrands } = useBrands()
    const [brandsExpanded, setBrandsExpanded] = useState(false)

    const brands = allBrands ?? []
    const visibleBrands = brandsExpanded ? brands : brands.slice(0, VISIBLE_BRANDS_LIMIT)
    const hiddenBrandsCount = brands.length - VISIBLE_BRANDS_LIMIT

    function toggleBrand(brandId: string) {
        const next = filters.brands.includes(brandId)
            ? filters.brands.filter((b) => b !== brandId)
            : [...filters.brands, brandId]
        onChange({ ...filters, brands: next })
    }

    return (
        <aside className={styles.sidebar}>
            <div className={styles.group}>
                <h3 className={styles.groupTitle}>CATEGORIA</h3>
                <div className={styles.btnList}>
                    <button
                        onClick={() => onChange({ ...filters, category: '' })}
                        className={`${styles.filterBtn} ${filters.category === '' ? styles.filterBtnActive : styles.filterBtnInactive}`}
                    >
                        Todos os produtos
                    </button>
                    {(categories ?? []).map((cat) => (
                        <button
                            key={cat.id}
                            onClick={() => onChange({ ...filters, category: cat.name })}
                            className={`${styles.filterBtn} ${filters.category === cat.name ? styles.filterBtnActive : styles.filterBtnInactive}`}
                        >
                            {cat.name}
                            <span className={styles.countLabel}>({cat.productCount})</span>
                        </button>
                    ))}
                </div>
            </div>

            <div className={styles.group}>
                <h3 className={styles.groupTitle}>FAIXA DE PREÇO</h3>
                <div className={styles.priceRow}>
                    <input
                        type="number"
                        value={filters.minPrice}
                        onChange={(e) => onChange({ ...filters, minPrice: Number(e.target.value) })}
                        placeholder="Min"
                        className={styles.priceInput}
                    />
                    <span className={styles.priceSep}>–</span>
                    <input
                        type="number"
                        value={filters.maxPrice}
                        onChange={(e) => onChange({ ...filters, maxPrice: Number(e.target.value) })}
                        placeholder="Max"
                        className={styles.priceInput}
                    />
                </div>
            </div>

            <div className={styles.group}>
                <h3 className={styles.groupTitle}>MARCAS</h3>
                <div className={styles.checkList}>
                    {visibleBrands.map((brand) => (
                        <label key={brand.id} className={styles.checkLabel}>
                            <input
                                type="checkbox"
                                checked={filters.brands.includes(brand.slug)}
                                onChange={() => toggleBrand(brand.slug)}
                                style={{ accentColor: 'var(--color-primary)' }}
                            />
                            <span className={styles.checkText}>{brand.name}</span>
                        </label>
                    ))}
                </div>
                {hiddenBrandsCount > 0 && (
                    <button
                        type="button"
                        onClick={() => setBrandsExpanded((expanded) => !expanded)}
                        className={styles.expandBtn}
                    >
                        {brandsExpanded ? 'Ver menos' : `Ver mais (+${hiddenBrandsCount})`}
                    </button>
                )}
            </div>

            <div>
                <label className={styles.checkLabel}>
                    <input
                        type="checkbox"
                        checked={filters.inStock}
                        onChange={(e) => onChange({ ...filters, inStock: e.target.checked })}
                        style={{ accentColor: 'var(--color-primary)' }}
                    />
                    <span className={styles.checkText}>Somente em estoque</span>
                </label>
            </div>
        </aside>
    )
}
