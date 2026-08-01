import { adminOrders } from '@/lib/data/orders'
import type { AdminOrder, OrderStatus } from '@/types/order'
import { Badge, type BadgeStatus } from '../Badge/Badge'
import { DataTable, type DataTableColumn } from '../DataTable/DataTable'

const STATUS_MAP: Record<OrderStatus, BadgeStatus> = {
    Entregue: 'paid',
    'Em trânsito': 'shipped',
    Processando: 'pending',
    Cancelado: 'cancelled',
    'Aguardando pagamento': 'pending',
}

const columns: Array<DataTableColumn<AdminOrder>> = [
    { key: 'id', label: 'Pedido', mono: true },
    { key: 'client', label: 'Cliente' },
    { key: 'total', label: 'Total', align: 'right', mono: true },
    {
        key: 'status',
        label: 'Status',
        render: (row) => <Badge status={STATUS_MAP[row.status]} size="sm" />,
    },
    { key: 'date', label: 'Data', align: 'right', mono: true, width: 90 },
]

export function RecentOrders() {
    return <DataTable columns={columns} rows={adminOrders.slice(0, 5)} rowKey={(row) => row.id} />
}
