import styles from './Card.module.css'

type CardProps = {
    title?: string
    action?: React.ReactNode
    flush?: boolean
    children: React.ReactNode
}

export function Card({ title, action, flush = false, children }: CardProps) {
    return (
        <div className={styles.card}>
            {title && (
                <div className={styles.header}>
                    <h3 className={styles.title}>{title}</h3>
                    {action}
                </div>
            )}
            <div className={flush ? styles.bodyFlush : styles.body}>{children}</div>
        </div>
    )
}
