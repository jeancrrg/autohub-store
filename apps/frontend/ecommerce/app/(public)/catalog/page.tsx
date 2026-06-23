import { products } from '@/lib/data/products'
import { ProductGrid } from '@/components/catalog/ProductGrid/ProductGrid'
import Link from 'next/link'
import styles from './page.module.css'

export default function CatalogPage() {
    return (
        <div className={styles.container}>
            <nav className={styles.breadcrumb}>
                <Link href="/" className={styles.breadcrumbLink}>
                    Início
                </Link>
                <span>/</span>
                <span className={styles.breadcrumbCurrent}>Catálogo</span>
            </nav>

            <h1 className={styles.title}>CATÁLOGO</h1>

            <ProductGrid allProducts={products} />
        </div>
    )
}
