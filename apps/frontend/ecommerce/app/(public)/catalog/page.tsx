'use client'

import { useProducts } from '@/hooks/useProducts'
import { ProductGrid } from '@/components/catalog/ProductGrid/ProductGrid'
import Link from 'next/link'
import styles from './page.module.css'

export default function CatalogPage() {
    const { data: products, isLoading, isError } = useProducts()

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

            {isLoading && <p>Carregando produtos...</p>}
            {isError && <p>Não foi possível carregar os produtos.</p>}
            {products && <ProductGrid allProducts={products} />}
        </div>
    )
}
