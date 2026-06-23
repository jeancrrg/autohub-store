import { metrics } from '@/lib/data/admin'
import styles from './MetricsCards.module.css'

export function MetricsCards() {
    return (
        <div className={styles.grid}>
            {metrics.map((m) => (
                <div key={m.label} className={styles.card}>
                    <div className={styles.cardTop}>
                        <span className={styles.icon}>{m.icon}</span>
                        <span
                            className={`${styles.badge} ${m.changePositive ? styles.badgePositive : styles.badgeNegative}`}
                        >
                            {m.change}
                        </span>
                    </div>
                    <p className={styles.value}>{m.value}</p>
                    <p className={styles.label}>{m.label}</p>
                </div>
            ))}
        </div>
    )
}
