'use client'

import { useParams, notFound } from 'next/navigation'
import { useProduct } from '@/hooks/useProduct'
import { ImageGallery } from '@/components/product/ImageGallery/ImageGallery'
import { ProductInfo } from '@/components/product/ProductInfo/ProductInfo'
import { ProductTabs } from '@/components/product/ProductTabs/ProductTabs'
import Link from 'next/link'
import styles from './page.module.css'

export default function ProductPage() {
    const params = useParams<{ id: string }>()
    const { data: product, isLoading, isError } = useProduct(params.id)

    if (isError) notFound()
    if (isLoading || !product) return <p>Carregando produto...</p>

    return (
        <div className={styles.container}>
            <nav className={styles.breadcrumb}>
                <Link href="/" className={styles.breadcrumbLink}>
                    Início
                </Link>
                <span>/</span>
                <Link href="/catalog" className={styles.breadcrumbLink}>
                    Catálogo
                </Link>
                <span>/</span>
                <span className={styles.breadcrumbCurrent}>{product.name}</span>
            </nav>

            <div className={styles.productGrid}>
                <ImageGallery images={product.images} name={product.name} />
                <ProductInfo product={product} />
            </div>

            <ProductTabs product={product} />
        </div>
    )
}
