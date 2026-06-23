import { salesBars, MAX_SALES_VALUE } from '@/lib/data/admin'
import styles from './RevenueChart.module.css'

export function RevenueChart() {
    return (
        <div className={styles.wrapper}>
            <h3 className={styles.title}>RECEITA POR MÊS</h3>
            <div className={styles.chartArea}>
                {salesBars.map((bar) => {
                    const heightPercent = Math.round((bar.value / MAX_SALES_VALUE) * 100)
                    return (
                        <div key={bar.month} className={styles.barCol}>
                            <span className={styles.barLabel}>{bar.label}</span>
                            <div
                                className={styles.bar}
                                style={{ height: `${heightPercent}%` }}
                                title={`${bar.month}: ${bar.label}`}
                            />
                            <span className={styles.barMonth}>{bar.month}</span>
                        </div>
                    )
                })}
            </div>
        </div>
    )
}
