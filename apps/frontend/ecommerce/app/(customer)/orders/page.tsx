import { customerOrders } from '@/lib/data/orders'
import Link from 'next/link'
import styles from './page.module.css'

const STATUS_STYLES: Record<string, { color: string; background: string }> = {
    Entregue: { color: '#15803d', background: '#dcfce7' },
    'Em trânsito': { color: '#1d4ed8', background: '#dbeafe' },
    Processando: { color: '#a16207', background: '#fef9c3' },
    Cancelado: { color: '#b91c1c', background: '#fee2e2' },
    'Aguardando pagamento': { color: '#c2410c', background: '#ffedd5' },
}

export default function OrdersPage() {
    return (
        <div className={styles.container}>
            <nav className={styles.breadcrumb}>
                <Link href="/" className={styles.breadcrumbLink}>
                    Início
                </Link>
                <span>/</span>
                <span className={styles.breadcrumbCurrent}>Meus Pedidos</span>
            </nav>

            <h1 className={styles.title}>MEUS PEDIDOS</h1>

            <div className={styles.table}>
                <div className={styles.tableHeader}>
                    {['PEDIDO', 'DATA', 'ITENS', 'TOTAL', 'STATUS', ''].map((h) => (
                        <span key={h} className={styles.tableHeaderCell}>
                            {h}
                        </span>
                    ))}
                </div>

                {customerOrders.map((order) => {
                    const s = STATUS_STYLES[order.status] ?? {
                        color: '#374151',
                        background: '#f3f4f6',
                    }
                    return (
                        <div key={order.id} className={styles.tableRow}>
                            <span className={styles.orderId}>{order.id}</span>
                            <span className={styles.orderDate}>{order.date}</span>
                            <span className={styles.orderItems}>{order.items}</span>
                            <span className={styles.orderTotal}>{order.total}</span>
                            <span
                                className={styles.statusBadge}
                                style={{ color: s.color, backgroundColor: s.background }}
                            >
                                {order.status}
                            </span>
                            <button className={styles.detailsButton}>VER DETALHES</button>
                        </div>
                    )
                })}
            </div>
        </div>
    )
}
