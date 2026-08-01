'use client'

import { useState } from 'react'
import { adminOrders } from '@/lib/data/orders'
import type { AdminOrder, OrderStatus } from '@/types/order'
import { Badge, type BadgeStatus } from '../Badge/Badge'
import { DataTable, type DataTableColumn } from '../DataTable/DataTable'
import { MoreIcon } from '../icons/Icons'
import styles from './OrdersTable.module.css'

const STATUS_MAP: Record<OrderStatus, BadgeStatus> = {
    Entregue: 'paid',
    'Em trânsito': 'shipped',
    Processando: 'pending',
    Cancelado: 'cancelled',
    'Aguardando pagamento': 'pending',
}

const TABS: Array<{ id: OrderStatus | 'all'; label: string }> = [
    { id: 'all', label: 'Todos' },
    { id: 'Entregue', label: 'Entregue' },
    { id: 'Em trânsito', label: 'Em trânsito' },
    { id: 'Processando', label: 'Processando' },
    { id: 'Cancelado', label: 'Cancelado' },
    { id: 'Aguardando pagamento', label: 'Aguardando' },
]

const columns: Array<DataTableColumn<AdminOrder>> = [
    { key: 'id', label: 'Pedido', mono: true },
    { key: 'client', label: 'Cliente' },
    { key: 'items', label: 'Itens' },
    { key: 'total', label: 'Total', align: 'right', mono: true },
    {
        key: 'status',
        label: 'Status',
        render: (row) => <Badge status={STATUS_MAP[row.status]} size="sm" />,
    },
    { key: 'date', label: 'Data', align: 'right', mono: true, width: 90 },
    {
        key: 'actions',
        label: '',
        align: 'right',
        width: 50,
        render: () => (
            <button className={styles.rowBtn} aria-label="Mais opções">
                <MoreIcon size={16} />
            </button>
        ),
    },
]

export function OrdersTable() {
    const [tab, setTab] = useState<OrderStatus | 'all'>('all')
    const rows = tab === 'all' ? adminOrders : adminOrders.filter((order) => order.status === tab)

    return (
        <div>
            <div className={styles.tabs}>
                {TABS.map(({ id, label }) => {
                    const count = id === 'all' ? adminOrders.length : adminOrders.filter((o) => o.status === id).length
                    return (
                        <button
                            key={id}
                            onClick={() => setTab(id)}
                            className={`${styles.tab} ${tab === id ? styles.tabActive : ''}`}
                        >
                            {label} <span className={styles.count}>{count}</span>
                        </button>
                    )
                })}
            </div>
            <DataTable columns={columns} rows={rows} rowKey={(row) => row.id} />
        </div>
    )
}
