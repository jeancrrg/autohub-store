import { CheckoutStepper } from '@/components/checkout/CheckoutStepper/CheckoutStepper'
import Link from 'next/link'
import styles from './page.module.css'

export default function CheckoutPage() {
    return (
        <div className={styles.container}>
            <nav className={styles.breadcrumb}>
                <Link href="/" className={styles.breadcrumbLink}>
                    Início
                </Link>
                <span>/</span>
                <Link href="/cart" className={styles.breadcrumbLink}>
                    Carrinho
                </Link>
                <span>/</span>
                <span className={styles.breadcrumbCurrent}>Checkout</span>
            </nav>

            <h1 className={styles.title}>CHECKOUT</h1>

            <CheckoutStepper />
        </div>
    )
}
