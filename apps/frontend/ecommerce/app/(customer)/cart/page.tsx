import { CartTable } from '@/components/cart/CartTable/CartTable'
import { OrderSummary } from '@/components/cart/OrderSummary/OrderSummary'
import Link from 'next/link'
import styles from './page.module.css'

export default function CartPage() {
    return (
        <div className={styles.container}>
            <nav className={styles.breadcrumb}>
                <Link href="/" className={styles.breadcrumbLink}>
                    Início
                </Link>
                <span>/</span>
                <span className={styles.breadcrumbCurrent}>Carrinho</span>
            </nav>

            <h1 className={styles.title}>MEU CARRINHO</h1>

            <div className={styles.layout}>
                <div className={styles.tableWrapper}>
                    <CartTable />
                </div>
                <OrderSummary />
            </div>
        </div>
    )
}
