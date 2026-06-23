import { ProductsTable } from '@/components/admin/ProductsTable/ProductsTable'
import styles from './page.module.css'

export default function AdminProductsPage() {
    return (
        <div>
            <div className={styles.header}>
                <h1 className={styles.title}>PRODUTOS</h1>
                <button className={styles.addButton}>+ NOVO PRODUTO</button>
            </div>
            <ProductsTable />
        </div>
    )
}
