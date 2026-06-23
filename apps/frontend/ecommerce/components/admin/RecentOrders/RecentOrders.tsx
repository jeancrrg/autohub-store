import { adminOrders } from '@/lib/data/orders'
import styles from './RecentOrders.module.css'

const STATUS_CLASSES: Record<string, string> = {
    Entregue: styles.badgeEntregue,
    'Em trânsito': styles.badgeTransito,
    Processando: styles.badgeProcessando,
    Cancelado: styles.badgeCancelado,
    'Aguardando pagamento': styles.badgeAguardando,
}

export function RecentOrders() {
    const recent = adminOrders.slice(0, 4)

    return (
        <div className={styles.wrapper}>
            <h3 className={styles.title}>PEDIDOS RECENTES</h3>
            <div className={styles.list}>
                {recent.map((order) => (
                    <div key={order.id} className={styles.row}>
                        <span className={styles.orderId}>{order.id}</span>
                        <span className={styles.client}>{order.client}</span>
                        <span
                            className={`${styles.badge} ${STATUS_CLASSES[order.status] ?? styles.badgeDefault}`}
                        >
                            {order.status}
                        </span>
                        <span className={styles.total}>{order.total}</span>
                    </div>
                ))}
            </div>
        </div>
    )
}
