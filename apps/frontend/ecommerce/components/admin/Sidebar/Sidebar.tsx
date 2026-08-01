'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { BagIcon, BoltIcon, BoxIcon, ChartIcon, GridIcon, LogoutIcon, UsersIcon } from '../icons/Icons'
import styles from './Sidebar.module.css'

const NAV_ITEMS = [
    { href: '/admin/dashboard', label: 'Dashboard', Icon: GridIcon },
    { href: '/admin/products', label: 'Produtos', Icon: BoxIcon },
    { href: '/admin/orders', label: 'Pedidos', Icon: BagIcon },
    { href: '/admin/users', label: 'Usuários', Icon: UsersIcon },
    { href: '/admin/analytics', label: 'Analytics', Icon: ChartIcon },
]

export function Sidebar() {
    const pathname = usePathname()

    return (
        <aside className={styles.sidebar}>
            <div className={styles.brand}>
                <span className={styles.brandMark}>
                    <BoltIcon size={18} stroke="#fff" />
                </span>
                <span className={styles.brandName}>
                    Auto<span className={styles.brandAccent}>Hub</span>
                </span>
                <span className={styles.brandTag}>ADMIN</span>
            </div>

            <nav className={styles.nav}>
                <div className={styles.navLabel}>Gerenciar</div>
                {NAV_ITEMS.map(({ href, label, Icon }) => {
                    const active = pathname === href
                    return (
                        <Link
                            key={href}
                            href={href}
                            className={`${styles.navItem} ${active ? styles.navItemActive : ''}`}
                        >
                            <span className={styles.navIcon}>
                                <Icon size={19} />
                            </span>
                            {label}
                        </Link>
                    )
                })}
            </nav>

            <div className={styles.profile}>
                <div className={styles.profileInner}>
                    <span className={styles.avatar}>AD</span>
                    <div className={styles.profileText}>
                        <div className={styles.profileName}>Admin AutoHub</div>
                        <div className={styles.profileRole}>Store Admin</div>
                    </div>
                    <Link href="/" className={styles.logoutLink} aria-label="Voltar para a loja">
                        <LogoutIcon size={17} />
                    </Link>
                </div>
            </div>
        </aside>
    )
}
