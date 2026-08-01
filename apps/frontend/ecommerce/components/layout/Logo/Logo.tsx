import Link from 'next/link'
import styles from './Logo.module.css'

type LogoProps = {
    size?: 'md' | 'sm'
    href?: string
    className?: string
}

export function Logo({ size = 'md', href = '/', className }: LogoProps) {
    return (
        <Link href={href} className={`${styles.logo} ${className ?? ''}`}>
            <div className={`${styles.logoName} ${size === 'sm' ? styles.logoNameSm : ''}`}>
                AUTO<span className={styles.logoAccent}>HUB</span>
            </div>
            <div className={`${styles.logoSub} ${size === 'sm' ? styles.logoSubSm : ''}`}>
                {'STORE'.split('').map((c, i) => (
                    <span key={i}>{c}</span>
                ))}
            </div>
        </Link>
    )
}
