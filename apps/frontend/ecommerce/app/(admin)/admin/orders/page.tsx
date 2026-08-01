import { OrdersTable } from '@/components/admin/OrdersTable/OrdersTable'
import { Topbar } from '@/components/admin/Topbar/Topbar'
import styles from './page.module.css'

export default function AdminOrdersPage() {
    return (
        <>
            <Topbar title="Pedidos" subtitle="Acompanhamento de pedidos da loja" />
            <div className={styles.page}>
                <OrdersTable />
            </div>
        </>
    )
}
