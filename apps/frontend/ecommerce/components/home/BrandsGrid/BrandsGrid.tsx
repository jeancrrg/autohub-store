'use client'

import { useState } from 'react'
import { useBrands } from '@/hooks/useBrands'
import styles from './BrandsGrid.module.css'

const VISIBLE_BRANDS_LIMIT = 12

export function BrandsGrid() {
    const { data: allBrands } = useBrands()
    const [expanded, setExpanded] = useState(false)

    const brands = allBrands ?? []
    const visibleBrands = expanded ? brands : brands.slice(0, VISIBLE_BRANDS_LIMIT)
    const hiddenCount = brands.length - VISIBLE_BRANDS_LIMIT

    return (
        <section className={styles.section}>
            <div className={styles.inner}>
                <div className={styles.headerCenter}>
                    <div className={styles.labelRow}>
                        <div className={styles.labelBar} />
                        <span className={styles.labelText}>Parceiros</span>
                        <div className={styles.labelBar} />
                    </div>
                    <h2 className={styles.sectionTitle}>Marcas Oficiais</h2>
                </div>
                <div className={styles.grid}>
                    {visibleBrands.map((brand) => (
                        <div key={brand.id} className={styles.brandCard}>
                            <span className={styles.brandName}>{brand.name}</span>
                        </div>
                    ))}
                </div>
                {hiddenCount > 0 && (
                    <div className={styles.expandRow}>
                        <button
                            type="button"
                            onClick={() => setExpanded((current) => !current)}
                            className={styles.expandBtn}
                        >
                            {expanded ? 'Ver menos' : `Ver mais (+${hiddenCount})`}
                        </button>
                    </div>
                )}
            </div>
        </section>
    )
}
