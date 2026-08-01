import type { TopProduct } from '@/lib/data/admin'
import styles from './TopProductsList.module.css'

export function TopProductsList({ products }: { products: TopProduct[] }) {
    return (
        <div className={styles.list}>
            {products.map((product, i) => (
                <div key={product.rank}>
                    <div className={styles.row}>
                        <span className={styles.name}>{product.name}</span>
                        <span className={styles.sold}>{product.sold}</span>
                    </div>
                    <div className={styles.track}>
                        <div
                            className={`${styles.fill} ${i === 0 ? styles.fillFirst : styles.fillRest}`}
                            style={{ width: `${product.pct}%` }}
                        />
                    </div>
                </div>
            ))}
        </div>
    )
}
