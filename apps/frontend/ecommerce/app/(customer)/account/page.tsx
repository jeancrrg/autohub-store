'use client'

import { useState } from 'react'
import { AccountSidebar } from '@/components/account/AccountSideBar/AccountSidebar'
import { OrdersTab } from '@/components/account/OrdersTab/OrdersTab'
import { ProfileTab } from '@/components/account/ProfileTab/ProfileTab'
import styles from './page.module.css'

type Tab = 'pedidos' | 'dados' | 'enderecos' | 'seguranca'

export default function AccountPage() {
    const [activeTab, setActiveTab] = useState<Tab>('pedidos')

    return (
        <div className={styles.container}>
            <h1 className={styles.title}>MINHA CONTA</h1>

            <div className={styles.layout}>
                <AccountSidebar activeTab={activeTab} onTabChange={setActiveTab} />

                <div className={styles.content}>
                    {activeTab === 'pedidos' && <OrdersTab />}
                    {activeTab === 'dados' && <ProfileTab />}
                    {activeTab === 'enderecos' && (
                        <div>
                            <h2 className={styles.tabTitle}>ENDEREÇOS</h2>
                            <p className={styles.tabText}>Nenhum endereço cadastrado.</p>
                        </div>
                    )}
                    {activeTab === 'seguranca' && (
                        <div>
                            <h2 className={styles.tabTitle}>SEGURANÇA</h2>
                            <p className={styles.tabText}>Gerenciamento de senha em breve.</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}
