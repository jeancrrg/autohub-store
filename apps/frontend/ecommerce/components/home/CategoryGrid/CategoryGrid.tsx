'use client'

import Link from 'next/link'
import { useCategories } from '@/hooks/useCategories'
import { getCategoryIcon } from '@/lib/data/categoryIcons'
import { FEATURED_CATEGORY_SLUGS } from '@/lib/data/featuredCategorySlugs'
import styles from './CategoryGrid.module.css'

export function CategoryGrid() {
    const { data: categories, isLoading, isError } = useCategories()

    if (isLoading || isError || !categories || categories.length === 0) {
        return null
    }

    const featured = FEATURED_CATEGORY_SLUGS.map((slug) => categories.find((cat) => cat.slug === slug)).filter(
        (cat): cat is NonNullable<typeof cat> => cat !== undefined
    )

    if (featured.length === 0) {
        return null
    }

    return (
        <section className={styles.section}>
            <div className={styles.inner}>
                <div className={styles.sectionHeader}>
                    <div>
                        <div className={styles.labelRow}>
                            <div className={styles.labelBar} />
                            <span className={styles.labelText}>Explore</span>
                        </div>
                        <h2 className={styles.sectionTitle}>Categorias Principais</h2>
                    </div>
                    <Link href="/catalog" className={styles.viewAll}>
                        VER TODAS →
                    </Link>
                </div>

                <div className={styles.grid}>
                    {featured.map((cat) => (
                        <Link key={cat.id} href="/catalog" className={styles.categoryCard}>
                            <div className={styles.categoryTopBar} />
                            <div className={styles.categoryIcon}>{getCategoryIcon(cat.slug)}</div>
                            <div className={styles.categoryName}>{cat.name}</div>
                            <div className={styles.categoryCount}>{cat.productCount} produtos</div>
                        </Link>
                    ))}
                </div>
            </div>
        </section>
    )
}
