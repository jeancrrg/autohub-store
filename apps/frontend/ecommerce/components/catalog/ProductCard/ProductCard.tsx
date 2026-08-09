import Link from 'next/link'
import type { Product } from '@/types/product'
import styles from './ProductCard.module.css'

export function ProductCard({ product }: { product: Product }) {
    return (
        <div className={styles.card}>
            <div className={styles.imageArea}>
                {product.tag && (
                    <span className={`${styles.tagBadge} ${product.tagColor}`}>{product.tag}</span>
                )}
                {!product.inStock && <span className={styles.soldOutBadge}>ESGOTADO</span>}
                {product.images[0] ? (
                    <img src={product.images[0]} alt={product.name} className={styles.image} />
                ) : (
                    <div className={styles.imagePlaceholder}>{product.brand}</div>
                )}
            </div>

            <div className={styles.body}>
                <p className={styles.brand}>{product.brand}</p>
                <Link href={`/products/${product.slug}`}>
                    <p className={styles.name}>{product.name}</p>
                </Link>

                <div className={styles.stars}>
                    {'★'.repeat(product.stars)}
                    {'☆'.repeat(5 - product.stars)}
                    <span className={styles.reviewCount}>({product.reviews})</span>
                </div>

                <div className={styles.priceArea}>
                    <p className={styles.oldPrice}>
                        {product.oldPrice
                            ? `R$ ${product.oldPrice.toFixed(2).replace('.', ',')}`
                            : ' '}
                    </p>
                    <p className={styles.price}>R$ {product.price.toFixed(2).replace('.', ',')}</p>
                    <p className={styles.installments}>
                        ou {product.installments}x de R${' '}
                        {(product.price / product.installments).toFixed(2).replace('.', ',')}
                    </p>
                </div>

                <Link href={`/products/${product.slug}`} className={styles.button}>
                    VER PRODUTO
                </Link>
            </div>
        </div>
    )
}
