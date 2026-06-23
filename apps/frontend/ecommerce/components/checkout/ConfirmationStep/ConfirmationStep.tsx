'use client'

import { useEffect } from 'react'
import Link from 'next/link'
import { useCartStore } from '@/store/cartStore'
import styles from './ConfirmationStep.module.css'

export function ConfirmationStep() {
    const clearCart = useCartStore((s) => s.clearCart)

    useEffect(() => {
        clearCart()
    }, [clearCart])

    const orderNumber = `AH-2024-${String(Math.floor(Math.random() * 9000) + 1000)}`

    return (
        <div className={styles.wrapper}>
            <div className={styles.successCircle}>
                <span className={styles.successIcon}>✓</span>
            </div>

            <h2 className={styles.title}>PEDIDO CONFIRMADO!</h2>
            <p className={styles.orderNum}>Pedido #{orderNumber}</p>
            <p className={styles.subtitle}>
                Você receberá um e-mail com os detalhes do pedido em breve.
            </p>

            <div className={styles.summaryCard}>
                <p className={styles.summaryLabel}>RESUMO</p>
                <div className={styles.summaryRows}>
                    <div className={styles.summaryRow}>
                        <span className={styles.rowKey}>Pedido</span>
                        <span className={styles.rowVal}>#{orderNumber}</span>
                    </div>
                    <div className={styles.summaryRow}>
                        <span className={styles.rowKey}>Status</span>
                        <span className={styles.rowValSuccess}>Confirmado</span>
                    </div>
                    <div className={styles.summaryRow}>
                        <span className={styles.rowKey}>Previsão</span>
                        <span className={styles.rowVal}>3–5 dias úteis</span>
                    </div>
                </div>
            </div>

            <div className={styles.actions}>
                <Link href="/orders" className={styles.primaryBtn}>
                    VER MEUS PEDIDOS
                </Link>
                <Link href="/catalog" className={styles.secondaryBtn}>
                    CONTINUAR COMPRANDO
                </Link>
            </div>
        </div>
    )
}
