import { topProducts } from '@/lib/data/admin'
import styles from './TopProducts.module.css'

export function TopProducts() {
    return (
        <div className={styles.wrapper}>
            <h3 className={styles.title}>TOP PRODUTOS</h3>
            <div className={styles.list}>
                {topProducts.map((p) => (
                    <div key={p.rank} className={styles.item}>
                        <span className={styles.rank}>#{p.rank}</span>
                        <div className={styles.info}>
                            <p className={styles.name}>{p.name}</p>
                            <p className={styles.brand}>{p.brand}</p>
                        </div>
                        <div className={styles.stats}>
                            <p className={styles.revenue}>{p.revenue}</p>
                            <p className={styles.sales}>{p.sales} vendas</p>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    )
}
