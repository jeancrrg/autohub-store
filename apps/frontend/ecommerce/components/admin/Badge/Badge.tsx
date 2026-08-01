import styles from './Badge.module.css'

export type BadgeTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info'
export type BadgeStatus =
    | 'in-stock'
    | 'low-stock'
    | 'out-of-stock'
    | 'paid'
    | 'pending'
    | 'cancelled'
    | 'shipped'
    | 'active'
    | 'inactive'

const STATUS_PRESETS: Record<BadgeStatus, { tone: BadgeTone; label: string }> = {
    'in-stock': { tone: 'success', label: 'Em estoque' },
    'low-stock': { tone: 'warning', label: 'Estoque baixo' },
    'out-of-stock': { tone: 'danger', label: 'Esgotado' },
    paid: { tone: 'success', label: 'Pago' },
    pending: { tone: 'warning', label: 'Pendente' },
    cancelled: { tone: 'danger', label: 'Cancelado' },
    shipped: { tone: 'info', label: 'Enviado' },
    active: { tone: 'success', label: 'Ativo' },
    inactive: { tone: 'neutral', label: 'Inativo' },
}

const TONE_CLASS: Record<BadgeTone, string> = {
    neutral: styles.neutral,
    success: styles.success,
    warning: styles.warning,
    danger: styles.danger,
    info: styles.info,
}

type BadgeProps = {
    children?: React.ReactNode
    tone?: BadgeTone
    status?: BadgeStatus
    dot?: boolean
    size?: 'sm' | 'md'
}

export function Badge({ children, tone, status, dot = true, size = 'md' }: BadgeProps) {
    const preset = status ? STATUS_PRESETS[status] : undefined
    const resolvedTone = tone ?? preset?.tone ?? 'neutral'
    const label = children ?? preset?.label ?? status ?? ''

    return (
        <span className={`${styles.badge} ${TONE_CLASS[resolvedTone]} ${size === 'sm' ? styles.badgeSm : ''}`}>
            {dot && <span className={styles.dot} style={{ background: 'currentColor' }} />}
            {label}
        </span>
    )
}
