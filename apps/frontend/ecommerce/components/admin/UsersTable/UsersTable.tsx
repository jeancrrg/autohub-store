'use client'

import { useState } from 'react'
import { adminUsers } from '@/lib/data/admin'
import type { AdminUser } from '@/types/order'
import { DataTable, type DataTableColumn } from '../DataTable/DataTable'
import styles from './UsersTable.module.css'

export function UsersTable() {
    const [users, setUsers] = useState<AdminUser[]>(adminUsers)

    function toggleActive(id: number) {
        setUsers((prev) => prev.map((u) => (u.id === id ? { ...u, active: !u.active } : u)))
    }

    const columns: Array<DataTableColumn<AdminUser>> = [
        {
            key: 'name',
            label: 'Cliente',
            render: (row) => (
                <div className={styles.customer}>
                    <span className={styles.avatar}>{row.initials}</span>
                    <div>
                        <div className={styles.name}>{row.name}</div>
                        <div className={styles.email}>{row.email}</div>
                    </div>
                </div>
            ),
        },
        { key: 'registeredAt', label: 'Cadastro', align: 'center', mono: true, width: 100 },
        {
            key: 'active',
            label: 'Status',
            render: (row) => (
                <button
                    onClick={() => toggleActive(row.id)}
                    className={`${styles.toggle} ${row.active ? styles.toggleOn : styles.toggleOff}`}
                    aria-label={row.active ? 'Desativar usuário' : 'Ativar usuário'}
                >
                    <span className={styles.toggleKnob} />
                </button>
            ),
        },
    ]

    return <DataTable columns={columns} rows={users} rowKey={(row) => row.id} />
}
