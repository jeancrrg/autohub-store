'use client'

import { useState } from 'react'
import { AddressForm, type AddressFormData } from '../AddressForm/AddressForm'
import { DeliveryStep } from '../DeliveryStep/DeliveryStep'
import { PaymentStep } from '../PaymentStep/PaymentStep'
import { ConfirmationStep } from '../ConfirmationStep/ConfirmationStep'
import { useCartStore } from '@/store/cartStore'
import styles from './CheckoutStepper.module.css'

const STEPS = [
    { number: 1, label: 'Endereço' },
    { number: 2, label: 'Entrega' },
    { number: 3, label: 'Pagamento' },
    { number: 4, label: 'Confirmação' },
]

export function CheckoutStepper() {
    const [step, setStep] = useState(1)
    const { items, total } = useCartStore()

    const shipping = total >= 299 ? 0 : 29.9
    const finalTotal = total + shipping

    return (
        <div className={styles.wrapper}>
            <div className={styles.main}>
                <div className={styles.stepIndicators}>
                    {STEPS.map((s, i) => (
                        <div key={s.number} style={{ display: 'flex', alignItems: 'center' }}>
                            <div className={styles.stepItem}>
                                <div
                                    className={`${styles.stepCircle} ${
                                        step > s.number
                                            ? styles.stepDone
                                            : step === s.number
                                              ? styles.stepActive
                                              : styles.stepPending
                                    }`}
                                >
                                    {step > s.number ? '✓' : s.number}
                                </div>
                                <span
                                    className={`${styles.stepLabel} ${step === s.number ? styles.stepLabelActive : styles.stepLabelInactive}`}
                                >
                                    {s.label}
                                </span>
                            </div>
                            {i < STEPS.length - 1 && (
                                <div
                                    className={`${styles.connector} ${step > s.number ? styles.connectorDone : styles.connectorPending}`}
                                />
                            )}
                        </div>
                    ))}
                </div>

                {step === 1 && <AddressForm onSubmit={(_data: AddressFormData) => setStep(2)} />}
                {step === 2 && <DeliveryStep onNext={() => setStep(3)} />}
                {step === 3 && <PaymentStep onNext={() => setStep(4)} />}
                {step === 4 && <ConfirmationStep />}
            </div>

            {step < 4 && (
                <div className={styles.sidebar}>
                    <h3 className={styles.sidebarTitle}>RESUMO</h3>
                    <div className={styles.sidebarItems}>
                        {items.map(({ product, qty }) => (
                            <div key={product.id} className={styles.sidebarItem}>
                                <span className={styles.sidebarItemName}>
                                    {product.name} x{qty}
                                </span>
                                <span className={styles.sidebarItemPrice}>
                                    R$ {(product.price * qty).toFixed(2).replace('.', ',')}
                                </span>
                            </div>
                        ))}
                    </div>
                    <div className={styles.sidebarTotals}>
                        <div className={styles.sidebarRow}>
                            <span className={styles.sidebarLabel}>Subtotal</span>
                            <span className={styles.sidebarValue}>
                                R$ {total.toFixed(2).replace('.', ',')}
                            </span>
                        </div>
                        <div className={styles.sidebarRow}>
                            <span className={styles.sidebarLabel}>Frete</span>
                            <span className={styles.sidebarValue}>
                                {shipping === 0 ? (
                                    <span className={styles.shippingFree}>GRÁTIS</span>
                                ) : (
                                    `R$ ${shipping.toFixed(2).replace('.', ',')}`
                                )}
                            </span>
                        </div>
                        <div className={styles.sidebarTotal}>
                            <span className={styles.sidebarTotalLabel}>TOTAL</span>
                            <span className={styles.sidebarTotalValue}>
                                R$ {finalTotal.toFixed(2).replace('.', ',')}
                            </span>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
