import { customerOrders } from '@/lib/data/orders'
import styles from './OrdersTab.module.css'

const STATUS_CLASSES: Record<string, string> = {
    Entregue: styles.badgeEntregue,
    'Em trânsito': styles.badgeTransito,
    Processando: styles.badgeProcessando,
    Cancelado: styles.badgeCancelado,
    'Aguardando pagamento': styles.badgeAguardando,
}

export function OrdersTab() {
    return (
        <div className={styles.wrapper}>
            <h2 className={styles.title}>MEUS PEDIDOS</h2>

            <div className={styles.tableHeader}>
                {['PEDIDO', 'DATA', 'ITENS', 'TOTAL', 'STATUS'].map((h) => (
                    <span key={h} className={styles.headerCell}>
                        {h}
                    </span>
                ))}
            </div>

            {customerOrders.map((order) => (
                <div key={order.id} className={styles.row}>
                    <span className={styles.orderId}>{order.id}</span>
                    <span className={styles.date}>{order.date}</span>
                    <span className={styles.items}>{order.items}</span>
                    <span className={styles.total}>{order.total}</span>
                    <span
                        className={`${styles.badge} ${STATUS_CLASSES[order.status] ?? styles.badgeDefault}`}
                    >
                        {order.status}
                    </span>
                </div>
            ))}
        </div>
    )
}
