'use client'

import Link from 'next/link'
import { useCartStore } from '@/store/cartStore'
import styles from './CartTable.module.css'

export function CartTable() {
    const { items, removeItem, updateQty } = useCartStore()

    if (items.length === 0) {
        return (
            <div className={styles.emptyState}>
                <p className={styles.emptyText}>Seu carrinho está vazio.</p>
                <Link href="/catalog" className={styles.continueShopping}>
                    CONTINUAR COMPRANDO →
                </Link>
            </div>
        )
    }

    return (
        <div>
            <div className={styles.tableHeader}>
                {['PRODUTO', 'PREÇO', 'QUANTIDADE', 'SUBTOTAL', ''].map((h) => (
                    <span key={h} className={styles.headerCell}>
                        {h}
                    </span>
                ))}
            </div>

            {items.map(({ product, qty }) => (
                <div key={product.id} className={styles.row}>
                    <div className={styles.productCell}>
                        <div className={styles.productImage}>
                            <span className={styles.productImageText}>{product.brand}</span>
                        </div>
                        <div>
                            <p className={styles.productBrand}>{product.brand}</p>
                            <Link href={`/products/${product.slug}`} className={styles.productName}>
                                {product.name}
                            </Link>
                        </div>
                    </div>

                    <span className={styles.unitPrice}>
                        R$ {product.price.toFixed(2).replace('.', ',')}
                    </span>

                    <div className={styles.qtyControl}>
                        <button
                            onClick={() =>
                                qty > 1 ? updateQty(product.id, qty - 1) : removeItem(product.id)
                            }
                            className={styles.qtyBtn}
                        >
                            −
                        </button>
                        <span className={styles.qtyValue}>{qty}</span>
                        <button
                            onClick={() => updateQty(product.id, qty + 1)}
                            className={styles.qtyBtn}
                        >
                            +
                        </button>
                    </div>

                    <span className={styles.subtotal}>
                        R$ {(product.price * qty).toFixed(2).replace('.', ',')}
                    </span>

                    <button
                        onClick={() => removeItem(product.id)}
                        className={styles.removeBtn}
                        aria-label="Remover produto"
                    >
                        ✕
                    </button>
                </div>
            ))}
        </div>
    )
}
