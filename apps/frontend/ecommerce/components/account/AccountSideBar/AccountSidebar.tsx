'use client'

import { useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { useSession } from '@/hooks/useSession'
import { logout } from '@/lib/api/auth'
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
    const { data: user } = useSession()
    const router = useRouter()
    const queryClient = useQueryClient()

    const initials = user?.name
        ? user.name
              .split(' ')
              .map((part) => part[0])
              .slice(0, 2)
              .join('')
              .toUpperCase()
        : 'U'

    async function handleLogout() {
        await logout()
        await queryClient.invalidateQueries({ queryKey: ['session'] })
        router.push('/login')
    }

    return (
        <aside className={styles.sidebar}>
            <div className={styles.userInfo}>
                <div className={styles.avatar}>{initials}</div>
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
