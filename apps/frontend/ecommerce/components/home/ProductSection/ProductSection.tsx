import Link from 'next/link'
import type { Product } from '@/types/product'
import styles from './ProductSection.module.css'

type ProductSectionProps = {
    title: string
    subtitle: string
    products: Product[]
    showRank?: boolean
    bgGray?: boolean
}

function ProductCard({ product, rank }: { product: Product; rank?: number }) {
    const price = `R$ ${product.price.toFixed(2).replace('.', ',')}`
    const oldPrice = product.oldPrice ? `R$ ${product.oldPrice.toFixed(2).replace('.', ',')}` : null
    const install = `R$ ${(product.price / product.installments).toFixed(2).replace('.', ',')}`
    const stars = '★'.repeat(product.stars) + '☆'.repeat(5 - product.stars)

    return (
        <div className={styles.productCard}>
            {rank && <div className={styles.rankBadge}>#{rank}</div>}
            {product.tag && !rank && (
                <div
                    className={`${styles.tagBadge} ${styles.tagLeft} ${product.tagColor?.includes('blue') ? styles.tagBlue : ''}`}
                >
                    {product.tag}
                </div>
            )}
            {product.tag && rank && (
                <div
                    className={`${styles.tagBadge} ${styles.tagRight} ${product.tagColor?.includes('blue') ? styles.tagBlue : ''}`}
                >
                    {product.tag}
                </div>
            )}

            <div className={styles.imageArea}>
                {product.images[0] ? (
                    <img src={product.images[0]} alt={product.name} className={styles.image} />
                ) : (
                    <>
                        <div className={styles.imagePattern} />
                        <div className={styles.imagePlaceholder}>
                            <div className={styles.imagePlaceholderText}>
                                {product.brand}
                                <br />
                                foto
                            </div>
                        </div>
                    </>
                )}
            </div>

            <div className={styles.cardBody}>
                <div className={styles.cardBrand}>{product.brand}</div>
                <div className={styles.cardName}>{product.name}</div>
                <div className={styles.cardStars}>
                    {stars} <span className={styles.cardReviews}>({product.reviews})</span>
                </div>
                <div className={styles.priceRow}>
                    <span className={styles.cardPrice}>{price}</span>
                    {oldPrice && <span className={styles.cardOldPrice}>{oldPrice}</span>}
                </div>
                <div className={styles.cardInstall}>12x de {install} sem juros</div>
                <Link href={`/products/${product.slug}`} className={styles.addBtn}>
                    ADICIONAR
                </Link>
            </div>
        </div>
    )
}

export function ProductSection({
    title,
    subtitle,
    products,
    showRank = false,
    bgGray = false,
}: ProductSectionProps) {
    return (
        <section
            className={`${styles.section} ${bgGray ? styles.sectionGray : styles.sectionWhite}`}
        >
            <div className={styles.inner}>
                <div className={styles.sectionHeader}>
                    <div>
                        <div className={styles.labelRow}>
                            <div className={styles.labelBar} />
                            <span className={styles.labelText}>{subtitle}</span>
                        </div>
                        <h2 className={styles.sectionTitle}>{title}</h2>
                    </div>
                    <Link href="/catalog" className={styles.viewLink}>
                        {showRank ? 'VER TODOS →' : 'VER CATÁLOGO →'}
                    </Link>
                </div>

                <div className={styles.grid}>
                    {products.map((product, i) => (
                        <ProductCard
                            key={product.id}
                            product={product}
                            rank={showRank ? i + 1 : undefined}
                        />
                    ))}
                </div>
            </div>
        </section>
    )
}
