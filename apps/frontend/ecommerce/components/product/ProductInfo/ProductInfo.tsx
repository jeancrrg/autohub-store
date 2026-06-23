'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useCartStore } from '@/store/cartStore'
import type { Product } from '@/types/product'
import styles from './ProductInfo.module.css'

export function ProductInfo({ product }: { product: Product }) {
    const [qty, setQty] = useState(1)
    const addItem = useCartStore((s) => s.addItem)
    const router = useRouter()

    function handleAddToCart() {
        addItem(product, qty)
        router.push('/cart')
    }

    function handleBuyNow() {
        addItem(product, qty)
        router.push('/checkout')
    }

    return (
        <div className={styles.wrapper}>
            {product.tag && (
                <span className={`${styles.tagBadge} ${product.tagColor}`}>{product.tag}</span>
            )}

            <h1 className={styles.productName}>{product.name}</h1>

            <div className={styles.metaRow}>
                <span className={styles.stars}>
                    {'★'.repeat(product.stars)}
                    {'☆'.repeat(5 - product.stars)}
                </span>
                <span className={styles.metaText}>({product.reviews} avaliações)</span>
                <span className={styles.metaDivider}>|</span>
                <span className={styles.metaText}>
                    Marca: <span className={styles.brandName}>{product.brand}</span>
                </span>
            </div>

            <div className={styles.priceBox}>
                {product.oldPrice && (
                    <p className={styles.oldPrice}>
                        R$ {product.oldPrice.toFixed(2).replace('.', ',')}
                    </p>
                )}
                <p className={styles.price}>R$ {product.price.toFixed(2).replace('.', ',')}</p>
                <p className={styles.installments}>
                    ou {product.installments}x de R${' '}
                    {(product.price / product.installments).toFixed(2).replace('.', ',')} sem juros
                </p>
                <p className={styles.pixPrice}>
                    PIX: R$ {(product.price * 0.95).toFixed(2).replace('.', ',')} (5% off)
                </p>
            </div>

            <p className={product.inStock ? styles.inStock : styles.outOfStock}>
                {product.inStock ? '✓ Em estoque' : '✗ Produto esgotado'}
            </p>

            {product.inStock && (
                <>
                    <div className={styles.qtyRow}>
                        <span className={styles.qtyLabel}>Quantidade:</span>
                        <div className={styles.qtyControl}>
                            <button
                                onClick={() => setQty((q) => Math.max(1, q - 1))}
                                className={styles.qtyBtn}
                            >
                                −
                            </button>
                            <span className={styles.qtyValue}>{qty}</span>
                            <button onClick={() => setQty((q) => q + 1)} className={styles.qtyBtn}>
                                +
                            </button>
                        </div>
                    </div>

                    <div className={styles.btnRow}>
                        <button onClick={handleAddToCart} className={styles.btnCart}>
                            ADICIONAR AO CARRINHO
                        </button>
                        <button onClick={handleBuyNow} className={styles.btnBuy}>
                            COMPRAR AGORA
                        </button>
                    </div>
                </>
            )}

            <div className={styles.benefitList}>
                {[
                    '🚚 Frete grátis acima de R$ 299',
                    '🔒 Garantia de 12 meses',
                    '✓ Produto original',
                ].map((b) => (
                    <p key={b} className={styles.benefitItem}>
                        {b}
                    </p>
                ))}
            </div>
        </div>
    )
}
