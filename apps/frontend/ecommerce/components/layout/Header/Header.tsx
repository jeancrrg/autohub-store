'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useState } from 'react'
import { useCartStore } from '@/store/cartStore'
import { useSession } from '@/hooks/useSession'
import styles from './Header.module.css'

export function Header() {
    const [searchValue, setSearchValue] = useState('')
    const router = useRouter()
    const count = useCartStore((s) => s.count)
    const { data: user } = useSession()
    const isLoggedIn = Boolean(user)

    function handleSearch(e: React.FormEvent) {
        e.preventDefault()
        if (searchValue.trim()) {
            router.push(`/search?q=${encodeURIComponent(searchValue.trim())}`)
        }
    }

    return (
        <>
            {/* Announcement bar */}
            <div className={styles.announcementBar}>
                <div className={styles.announcementContent}>
                    <span className={styles.announcementText}>
                        FRETE GRÁTIS em pedidos acima de R$ 500 — Todo o Brasil
                    </span>
                </div>
            </div>

            {/* Main header */}
            <header className={styles.mainHeader}>
                <div className={styles.headerInner}>
                    {/* Logo */}
                    <Link href="/" className={styles.logo}>
                        <div className={styles.logoName}>
                            AUTO<span className={styles.logoAccent}>HUB</span>
                        </div>
                        <div className={styles.logoSub}>
                            {'STORE'.split('').map((c, i) => (
                                <span key={i}>{c}</span>
                            ))}
                        </div>
                    </Link>

                    {/* Search */}
                    <form onSubmit={handleSearch} className={styles.searchForm}>
                        <input
                            type="text"
                            value={searchValue}
                            onChange={(e) => setSearchValue(e.target.value)}
                            placeholder="Buscar peças, marcas, modelos..."
                            className={styles.searchInput}
                        />
                        <button type="submit" className={styles.searchBtn}>
                            <svg
                                width="16"
                                height="16"
                                fill="none"
                                stroke="white"
                                strokeWidth="2.5"
                                viewBox="0 0 24 24"
                            >
                                <circle cx="11" cy="11" r="8" />
                                <path d="m21 21-4.35-4.35" />
                            </svg>
                        </button>
                    </form>

                    {/* Icons */}
                    <div className={styles.iconGroup}>
                        <Link
                            href={isLoggedIn ? '/account' : '/login'}
                            className={styles.headerIcon}
                        >
                            <svg
                                width="20"
                                height="20"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="1.5"
                                viewBox="0 0 24 24"
                            >
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                                <circle cx="12" cy="7" r="4" />
                            </svg>
                            <span className={styles.iconLabel}>
                                {isLoggedIn ? user?.name.split(' ')[0] : 'Conta'}
                            </span>
                        </Link>

                        <div className={styles.headerIcon}>
                            <svg
                                width="20"
                                height="20"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="1.5"
                                viewBox="0 0 24 24"
                            >
                                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                            </svg>
                            <span className={styles.iconLabel}>Favoritos</span>
                        </div>

                        <Link href="/cart" className={styles.headerIcon}>
                            <div className={styles.cartWrapper}>
                                <svg
                                    width="20"
                                    height="20"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="1.5"
                                    viewBox="0 0 24 24"
                                >
                                    <path d="M6 2H3a1 1 0 0 0-1 1v1h18l-1.68 8.39A2 2 0 0 1 16.36 14H7.64a2 2 0 0 1-1.96-1.61L4 4" />
                                    <path d="M16 14H7.64L6 6" />
                                    <circle cx="9" cy="20" r="1" />
                                    <circle cx="17" cy="20" r="1" />
                                </svg>
                                {count > 0 && (
                                    <span className={styles.cartBadge}>
                                        {count > 9 ? '9+' : count}
                                    </span>
                                )}
                            </div>
                            <span className={styles.iconLabel}>Carrinho</span>
                        </Link>
                    </div>
                </div>

                {/* Nav */}
                <nav className={styles.nav}>
                    <div className={styles.navInner}>
                        <div className={styles.categoriesBtn}>
                            <svg
                                width="14"
                                height="14"
                                fill="none"
                                stroke="var(--color-primary)"
                                strokeWidth="2.5"
                                viewBox="0 0 24 24"
                            >
                                <line x1="3" y1="6" x2="21" y2="6" />
                                <line x1="3" y1="12" x2="21" y2="12" />
                                <line x1="3" y1="18" x2="21" y2="18" />
                            </svg>
                            <span className={styles.categoriesLabel}>Categorias</span>
                        </div>
                        {[
                            { label: 'Rodas', href: '/catalog' },
                            { label: 'Pneus', href: '/catalog' },
                            { label: 'Suspensão', href: '/catalog' },
                            { label: 'Freios', href: '/catalog' },
                            { label: 'Performance', href: '/catalog' },
                            { label: 'Motor', href: '/catalog' },
                            { label: 'Escapamento', href: '/catalog' },
                            { label: 'Iluminação', href: '/catalog' },
                            { label: 'Acessórios', href: '/catalog' },
                            { label: 'Limpeza', href: '/catalog' },
                        ].map((item) => (
                            <Link key={item.label} href={item.href} className={styles.navLink}>
                                {item.label}
                            </Link>
                        ))}
                        <Link href="/catalog" className={styles.navLinkOffers}>
                            OFERTAS
                        </Link>
                    </div>
                </nav>
            </header>
        </>
    )
}
