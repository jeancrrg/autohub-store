import { notFound } from 'next/navigation'
import { products } from '@/lib/data/products'
import { ImageGallery } from '@/components/product/ImageGallery/ImageGallery'
import { ProductInfo } from '@/components/product/ProductInfo/ProductInfo'
import { ProductTabs } from '@/components/product/ProductTabs/ProductTabs'
import Link from 'next/link'
import styles from './page.module.css'

export function generateStaticParams() {
    return products.map((p) => ({ id: String(p.id) }))
}

export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
    const { id } = await params
    const product = products.find((p) => p.id === Number(id))
    if (!product) notFound()

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
