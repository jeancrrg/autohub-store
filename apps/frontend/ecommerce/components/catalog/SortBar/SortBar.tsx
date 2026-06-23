'use client'

import styles from './SortBar.module.css'

type SortBarProps = {
    count: number
    sortBy: string
    onSortChange: (sort: string) => void
}

export function SortBar({ count, sortBy, onSortChange }: SortBarProps) {
    return (
        <div className={styles.bar}>
            <p className={styles.count}>
                <span className={styles.countBold}>{count}</span> produtos encontrados
            </p>
            <div className={styles.sortRow}>
                <span className={styles.sortLabel}>Ordenar por:</span>
                <select
                    value={sortBy}
                    onChange={(e) => onSortChange(e.target.value)}
                    className={styles.sortSelect}
                >
                    <option value="relevance">Relevância</option>
                    <option value="price-asc">Menor preço</option>
                    <option value="price-desc">Maior preço</option>
                    <option value="reviews">Mais vendidos</option>
                </select>
            </div>
        </div>
    )
}
