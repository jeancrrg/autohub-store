'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import styles from './AdminSidebar.module.css'

const NAV_ITEMS = [
    { href: '/admin/dashboard', icon: '📊', label: 'Dashboard' },
    { href: '/admin/products', icon: '📦', label: 'Produtos' },
    { href: '/admin/orders', icon: '🛒', label: 'Pedidos' },
    { href: '/admin/users', icon: '👥', label: 'Usuários' },
]

export function AdminSidebar() {
    const pathname = usePathname()

    return (
        <aside className={styles.sidebar}>
            <div className={styles.logoArea}>
                <p className={styles.logoTitle}>AUTOHUB</p>
                <p className={styles.logoSub}>ADMIN PANEL</p>
            </div>

            <nav className={styles.nav}>
                {NAV_ITEMS.map((item) => (
                    <Link
                        key={item.href}
                        href={item.href}
                        className={`${styles.navItem} ${pathname === item.href ? styles.navItemActive : styles.navItemInactive}`}
                    >
                        <span>{item.icon}</span>
                        {item.label}
                    </Link>
                ))}
            </nav>

            <div className={styles.footerArea}>
                <Link href="/" className={styles.backLink}>
                    ← VER LOJA
                </Link>
            </div>
        </aside>
    )
}
