import { BellIcon, SearchIcon } from '../icons/Icons'
import styles from './Topbar.module.css'

type TopbarProps = {
    title: string
    subtitle?: string
    action?: React.ReactNode
}

export function Topbar({ title, subtitle, action }: TopbarProps) {
    return (
        <div className={styles.topbar}>
            <div>
                <h1 className={styles.title}>{title}</h1>
                {subtitle && <div className={styles.subtitle}>{subtitle}</div>}
            </div>
            <div className={styles.actions}>
                <div className={styles.search}>
                    <SearchIcon size={16} stroke="var(--text-muted)" />
                    <input placeholder="Buscar…" />
                </div>
                <button className={styles.iconButton} aria-label="Notificações">
                    <BellIcon size={18} />
                    <span className={styles.dot} />
                </button>
                {action}
            </div>
        </div>
    )
}
