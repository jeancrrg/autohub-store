import styles from './TrustBadges.module.css'

export function TrustBadges() {
    const badges = [
        {
            title: 'Frete Grátis',
            subtitle: 'Pedidos acima de R$ 500',
            icon: (
                <svg
                    width="18"
                    height="18"
                    fill="none"
                    stroke="var(--color-primary)"
                    strokeWidth="2"
                    viewBox="0 0 24 24"
                >
                    <rect x="1" y="3" width="15" height="13" rx="1" />
                    <path d="M16 8h4l3 5v3h-7V8z" />
                    <circle cx="5.5" cy="18.5" r="2.5" />
                    <circle cx="18.5" cy="18.5" r="2.5" />
                </svg>
            ),
        },
        {
            title: 'Compra Segura',
            subtitle: 'Pagamento 100% protegido',
            icon: (
                <svg
                    width="18"
                    height="18"
                    fill="none"
                    stroke="var(--color-primary)"
                    strokeWidth="2"
                    viewBox="0 0 24 24"
                >
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                </svg>
            ),
        },
        {
            title: 'Troca Fácil',
            subtitle: '30 dias para trocar',
            icon: (
                <svg
                    width="18"
                    height="18"
                    fill="none"
                    stroke="var(--color-primary)"
                    strokeWidth="2"
                    viewBox="0 0 24 24"
                >
                    <polyline points="1 4 1 10 7 10" />
                    <path d="M3.51 15a9 9 0 1 0 .49-3.51" />
                </svg>
            ),
        },
        {
            title: 'Entrega Rápida',
            subtitle: 'Envio para todo o Brasil',
            icon: (
                <svg
                    width="18"
                    height="18"
                    fill="none"
                    stroke="var(--color-primary)"
                    strokeWidth="2"
                    viewBox="0 0 24 24"
                >
                    <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
                    <polyline points="13 2 13 9 20 9" />
                    <line x1="9" y1="14" x2="15" y2="14" />
                    <line x1="9" y1="18" x2="12" y2="18" />
                </svg>
            ),
        },
        {
            title: 'Suporte 24/7',
            subtitle: 'Especialistas disponíveis',
            icon: (
                <svg
                    width="18"
                    height="18"
                    fill="none"
                    stroke="var(--color-primary)"
                    strokeWidth="2"
                    viewBox="0 0 24 24"
                >
                    <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.61 3.4 2 2 0 0 1 3.6 1.2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L7.91 9a16 16 0 0 0 6.9 6.9l.54-.54a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 18z" />
                </svg>
            ),
        },
    ]

    return (
        <section className={styles.section}>
            <div className={styles.inner}>
                {badges.map((b, i) => (
                    <div
                        key={b.title}
                        className={`${styles.badge} ${i < 4 ? styles.badgeDivider : ''}`}
                    >
                        <div className={styles.iconCircle}>{b.icon}</div>
                        <div>
                            <div className={styles.badgeTitle}>{b.title}</div>
                            <div className={styles.badgeSubtitle}>{b.subtitle}</div>
                        </div>
                    </div>
                ))}
            </div>
        </section>
    )
}
