'use client'

import { useState } from 'react'
import { adminUsers } from '@/lib/data/admin'
import type { AdminUser } from '@/types/order'
import styles from './UsersTable.module.css'

export function UsersTable() {
    const [users, setUsers] = useState<AdminUser[]>(adminUsers)

    function toggleActive(id: number) {
        setUsers((prev) => prev.map((u) => (u.id === id ? { ...u, active: !u.active } : u)))
    }

    return (
        <div className={styles.wrapper}>
            <div className={styles.tableHeader}>
                {['', 'NOME', 'E-MAIL', 'CADASTRO', 'STATUS', 'ATIVO'].map((h) => (
                    <span key={h} className={styles.headerCell}>
                        {h}
                    </span>
                ))}
            </div>

            {users.map((user) => (
                <div key={user.id} className={styles.row}>
                    <div className={styles.avatar}>{user.initials}</div>

                    <span className={styles.name}>{user.name}</span>
                    <span className={styles.email}>{user.email}</span>
                    <span className={styles.registeredAt}>{user.registeredAt}</span>

                    <span
                        className={`${styles.statusBadge} ${user.active ? styles.statusActive : styles.statusInactive}`}
                    >
                        {user.active ? 'Ativo' : 'Inativo'}
                    </span>

                    <button
                        onClick={() => toggleActive(user.id)}
                        className={`${styles.toggle} ${user.active ? styles.toggleOn : styles.toggleOff}`}
                        aria-label={user.active ? 'Desativar usuário' : 'Ativar usuário'}
                    >
                        <span
                            className={`${styles.toggleKnob} ${user.active ? styles.toggleKnobOn : styles.toggleKnobOff}`}
                        />
                    </button>
                </div>
            ))}
        </div>
    )
}
