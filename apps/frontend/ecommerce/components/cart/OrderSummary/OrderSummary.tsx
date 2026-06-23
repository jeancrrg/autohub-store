'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useCartStore } from '@/store/cartStore'
import styles from './OrderSummary.module.css'

const PROMO_CODE = 'PROMO10'
const SHIPPING_THRESHOLD = 299
const SHIPPING_COST = 29.9

export function OrderSummary() {
    const { total, items } = useCartStore()
    const [coupon, setCoupon] = useState('')
    const [discount, setDiscount] = useState(0)
    const [couponError, setCouponError] = useState('')

    const shipping = total >= SHIPPING_THRESHOLD ? 0 : SHIPPING_COST
    const finalTotal = total - discount + shipping

    function applyCoupon() {
        if (coupon.toUpperCase() === PROMO_CODE) {
            setDiscount(total * 0.1)
            setCouponError('')
        } else {
            setDiscount(0)
            setCouponError('Cupom inválido.')
        }
    }

    return (
        <div className={styles.wrapper}>
            <h2 className={styles.title}>RESUMO DO PEDIDO</h2>

            <div className={styles.couponGroup}>
                <div className={styles.couponRow}>
                    <input
                        type="text"
                        value={coupon}
                        onChange={(e) => setCoupon(e.target.value)}
                        placeholder="Cupom de desconto"
                        className={styles.couponInput}
                    />
                    <button onClick={applyCoupon} className={styles.couponBtn}>
                        APLICAR
                    </button>
                </div>
                {couponError && <p className={styles.couponError}>{couponError}</p>}
                {discount > 0 && <p className={styles.couponSuccess}>Cupom PROMO10 aplicado!</p>}
            </div>

            <div className={styles.totals}>
                <div className={styles.totalRow}>
                    <span className={styles.totalLabel}>Subtotal</span>
                    <span className={styles.totalValue}>
                        R$ {total.toFixed(2).replace('.', ',')}
                    </span>
                </div>

                <div className={styles.totalRow}>
                    <span className={styles.totalLabel}>Frete</span>
                    <span className={styles.totalValue}>
                        {shipping === 0 ? (
                            <span className={styles.shippingFree}>GRÁTIS</span>
                        ) : (
                            `R$ ${shipping.toFixed(2).replace('.', ',')}`
                        )}
                    </span>
                </div>

                {discount > 0 && (
                    <div className={styles.totalRow}>
                        <span className={styles.discountLabel}>Desconto (PROMO10)</span>
                        <span className={styles.discountValue}>
                            − R$ {discount.toFixed(2).replace('.', ',')}
                        </span>
                    </div>
                )}

                <div className={styles.grandTotal}>
                    <span className={styles.grandLabel}>TOTAL</span>
                    <div className={styles.grandValueBox}>
                        <p className={styles.grandValue}>
                            R$ {finalTotal.toFixed(2).replace('.', ',')}
                        </p>
                        <p className={styles.grandInstall}>
                            ou 12x de R$ {(finalTotal / 12).toFixed(2).replace('.', ',')}
                        </p>
                    </div>
                </div>
            </div>

            {items.length > 0 && (
                <Link href="/checkout" className={styles.checkoutBtn}>
                    FINALIZAR PEDIDO
                </Link>
            )}

            <Link href="/catalog" className={styles.continueBtn}>
                CONTINUAR COMPRANDO
            </Link>
        </div>
    )
}
