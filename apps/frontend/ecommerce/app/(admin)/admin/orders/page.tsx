import { OrdersTable } from '@/components/admin/OrdersTable/OrdersTable'
import styles from './page.module.css'

export default function AdminOrdersPage() {
    return (
        <div>
            <h1 className={styles.title}>PEDIDOS</h1>
            <OrdersTable />
        </div>
    )
}
