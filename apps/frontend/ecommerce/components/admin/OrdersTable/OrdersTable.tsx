import { adminOrders } from '@/lib/data/orders'
import styles from './OrdersTable.module.css'

const STATUS_CLASSES: Record<string, string> = {
    Entregue: styles.badgeEntregue,
    'Em trânsito': styles.badgeTransito,
    Processando: styles.badgeProcessando,
    Cancelado: styles.badgeCancelado,
    'Aguardando pagamento': styles.badgeAguardando,
}

export function OrdersTable() {
    return (
        <div className={styles.wrapper}>
            <div className={styles.tableHeader}>
                {['PEDIDO', 'CLIENTE', 'DATA', 'STATUS', 'TOTAL', 'AÇÃO'].map((h) => (
                    <span key={h} className={styles.headerCell}>
                        {h}
                    </span>
                ))}
            </div>

            {adminOrders.map((order) => (
                <div key={order.id} className={styles.row}>
                    <span className={styles.orderId}>{order.id}</span>
                    <span className={styles.client}>{order.client}</span>
                    <span className={styles.date}>{order.date}</span>
                    <span
                        className={`${styles.badge} ${STATUS_CLASSES[order.status] ?? styles.badgeDefault}`}
                    >
                        {order.status}
                    </span>
                    <span className={styles.total}>{order.total}</span>
                    <button className={styles.viewBtn}>Ver</button>
                </div>
            ))}
        </div>
    )
}
