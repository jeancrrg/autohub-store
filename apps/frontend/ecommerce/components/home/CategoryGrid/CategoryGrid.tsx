import Link from 'next/link'
import styles from './CategoryGrid.module.css'

const categoryItems = [
    {
        id: 'rodas',
        name: 'Rodas',
        count: '1.240 produtos',
        icon: (
            <svg
                width="40"
                height="40"
                viewBox="0 0 40 40"
                fill="none"
                stroke="var(--color-primary)"
                strokeWidth="1.5"
            >
                <circle cx="20" cy="20" r="16" />
                <circle cx="20" cy="20" r="5" />
                <line x1="20" y1="4" x2="20" y2="15" />
                <line x1="20" y1="25" x2="20" y2="36" />
                <line x1="4" y1="20" x2="15" y2="20" />
                <line x1="25" y1="20" x2="36" y2="20" />
                <line x1="7.5" y1="7.5" x2="15" y2="15" />
                <line x1="25" y1="25" x2="32.5" y2="32.5" />
                <line x1="32.5" y1="7.5" x2="25" y2="15" />
                <line x1="15" y1="25" x2="7.5" y2="32.5" />
            </svg>
        ),
    },
    {
        id: 'pneus',
        name: 'Pneus',
        count: '890 produtos',
        icon: (
            <svg
                width="40"
                height="40"
                viewBox="0 0 40 40"
                fill="none"
                stroke="var(--color-primary)"
            >
                <circle cx="20" cy="20" r="15" strokeWidth="5" />
                <circle cx="20" cy="20" r="7" strokeWidth="1.5" />
            </svg>
        ),
    },
    {
        id: 'suspensao',
        name: 'Suspensão',
        count: '567 produtos',
        icon: (
            <svg
                width="40"
                height="40"
                viewBox="0 0 40 40"
                fill="none"
                stroke="var(--color-primary)"
                strokeWidth="1.5"
            >
                <line x1="20" y1="2" x2="20" y2="7" />
                <path d="M12 7 L28 7 L28 11 L12 11 L12 15 L28 15 L28 19 L12 19 L12 23 L28 23 L28 27 L12 27" />
                <line x1="20" y1="27" x2="20" y2="38" />
            </svg>
        ),
    },
    {
        id: 'freios',
        name: 'Freios',
        count: '432 produtos',
        icon: (
            <svg
                width="40"
                height="40"
                viewBox="0 0 40 40"
                fill="none"
                stroke="var(--color-primary)"
                strokeWidth="1.5"
            >
                <circle cx="20" cy="20" r="15" />
                <circle cx="20" cy="20" r="7" />
                <circle cx="20" cy="20" r="2.5" fill="var(--color-primary)" stroke="none" />
                <path d="M30 13 Q35 17 35 20 Q35 23 30 27" strokeWidth="3" strokeLinecap="round" />
            </svg>
        ),
    },
    {
        id: 'performance',
        name: 'Performance',
        count: '789 produtos',
        icon: (
            <svg
                width="40"
                height="40"
                viewBox="0 0 40 40"
                fill="none"
                stroke="var(--color-primary)"
                strokeWidth="1.5"
            >
                <path d="M7 28 A15 15 0 0 1 33 28" />
                <path d="M9 24 A13 13 0 0 1 31 24" strokeWidth="1" strokeDasharray="2 3" />
                <line x1="20" y1="28" x2="28" y2="14" strokeWidth="2" />
                <circle cx="20" cy="28" r="2.5" fill="var(--color-primary)" stroke="none" />
                <line x1="10" y1="27" x2="13" y2="27" />
                <line x1="27" y1="27" x2="30" y2="27" />
            </svg>
        ),
    },
    {
        id: 'motor',
        name: 'Motor',
        count: '654 produtos',
        icon: (
            <svg
                width="40"
                height="40"
                viewBox="0 0 40 40"
                fill="none"
                stroke="var(--color-primary)"
                strokeWidth="1.5"
            >
                <circle cx="20" cy="20" r="7" />
                <path
                    d="M20 3v5M20 32v5M3 20h5M32 20h5M7.6 7.6l3.5 3.5M28.9 28.9l3.5 3.5M32.4 7.6l-3.5 3.5M11.1 28.9l-3.5 3.5"
                    strokeWidth="2"
                />
                <circle cx="20" cy="20" r="13" strokeDasharray="3 2.5" />
            </svg>
        ),
    },
]

export function CategoryGrid() {
    return (
        <section className={styles.section}>
            <div className={styles.inner}>
                <div className={styles.sectionHeader}>
                    <div>
                        <div className={styles.labelRow}>
                            <div className={styles.labelBar} />
                            <span className={styles.labelText}>Explore</span>
                        </div>
                        <h2 className={styles.sectionTitle}>Categorias Principais</h2>
                    </div>
                    <Link href="/catalog" className={styles.viewAll}>
                        VER TODAS →
                    </Link>
                </div>

                <div className={styles.grid}>
                    {categoryItems.map((cat) => (
                        <Link key={cat.id} href="/catalog" className={styles.categoryCard}>
                            <div className={styles.categoryTopBar} />
                            <div className={styles.categoryIcon}>{cat.icon}</div>
                            <div className={styles.categoryName}>{cat.name}</div>
                            <div className={styles.categoryCount}>{cat.count}</div>
                        </Link>
                    ))}
                </div>
            </div>
        </section>
    )
}
