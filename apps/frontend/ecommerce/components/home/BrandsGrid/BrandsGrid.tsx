import { brands } from '@/lib/data/brands'
import styles from './BrandsGrid.module.css'

export function BrandsGrid() {
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
                    {brands.map((brand) => (
                        <div key={brand.id} className={styles.brandCard}>
                            <span className={styles.brandName}>{brand.name}</span>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    )
}
