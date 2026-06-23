'use client'

import { useAuthStore } from '@/store/authStore'
import { useRouter } from 'next/navigation'
import styles from './AccountSidebar.module.css'

type Tab = 'pedidos' | 'dados' | 'enderecos' | 'seguranca'

type AccountSidebarProps = {
    activeTab: Tab
    onTabChange: (tab: Tab) => void
}

const NAV_ITEMS: { id: Tab; label: string }[] = [
    { id: 'pedidos', label: 'Meus Pedidos' },
    { id: 'dados', label: 'Dados Pessoais' },
    { id: 'enderecos', label: 'Endereços' },
    { id: 'seguranca', label: 'Segurança' },
]

export function AccountSidebar({ activeTab, onTabChange }: AccountSidebarProps) {
    const { user, logout } = useAuthStore()
    const router = useRouter()

    function handleLogout() {
        logout()
        router.push('/login')
    }

    return (
        <aside className={styles.sidebar}>
            <div className={styles.userInfo}>
                <div className={styles.avatar}>{user?.initials ?? 'U'}</div>
                <div>
                    <p className={styles.userName}>{user?.name ?? 'Usuário'}</p>
                    <p className={styles.userEmail}>{user?.email ?? ''}</p>
                </div>
            </div>

            <nav className={styles.nav}>
                {NAV_ITEMS.map((item) => (
                    <button
                        key={item.id}
                        onClick={() => onTabChange(item.id)}
                        className={`${styles.navBtn} ${activeTab === item.id ? styles.navBtnActive : styles.navBtnInactive}`}
                    >
                        {item.label}
                    </button>
                ))}
            </nav>

            <button onClick={handleLogout} className={styles.logoutBtn}>
                Sair da conta
            </button>
        </aside>
    )
}
