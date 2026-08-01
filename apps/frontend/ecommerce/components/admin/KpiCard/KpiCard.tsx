import type { Kpi } from '@/lib/data/admin'
import { BagIcon, ChartIcon, DownIcon, UpIcon, UsersIcon } from '../icons/Icons'
import { Sparkline } from './Sparkline'
import styles from './KpiCard.module.css'

const ICON_MAP = {
    dollar: ChartIcon,
    bag: BagIcon,
    users: UsersIcon,
    chart: ChartIcon,
}

export function KpiCard({ label, value, delta, up, accent, spark, icon }: Kpi) {
    const Glyph = ICON_MAP[icon]

    return (
        <div className={styles.card}>
            <span className={styles.accentBar} style={{ background: accent }} />
            <div className={styles.top}>
                <span className={styles.label}>{label}</span>
                <span className={styles.iconBadge} style={{ background: `color-mix(in srgb, ${accent} 16%, transparent)`, color: accent }}>
                    <Glyph size={18} />
                </span>
            </div>
            <div className={styles.value}>{value}</div>
            <div className={styles.bottom}>
                <span className={`${styles.delta} ${up ? styles.deltaUp : styles.deltaDown}`}>
                    {up ? <UpIcon size={14} /> : <DownIcon size={14} />} {delta}
                </span>
                <Sparkline data={spark} color={accent} />
            </div>
        </div>
    )
}
