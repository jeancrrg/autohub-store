'use client'

import { useState } from 'react'
import styles from './PaymentStep.module.css'

type PaymentStepProps = {
    onNext: () => void
}

type PaymentMethod = 'card' | 'pix'

export function PaymentStep({ onNext }: PaymentStepProps) {
    const [method, setMethod] = useState<PaymentMethod>('card')
    const [cardNumber, setCardNumber] = useState('')
    const [cardName, setCardName] = useState('')
    const [cardExpiry, setCardExpiry] = useState('')
    const [cardCvv, setCardCvv] = useState('')

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        onNext()
    }

    return (
        <div className={styles.wrapper}>
            <h3 className={styles.title}>FORMA DE PAGAMENTO</h3>

            <div className={styles.tabBar}>
                {(['card', 'pix'] as PaymentMethod[]).map((m) => (
                    <button
                        key={m}
                        type="button"
                        onClick={() => setMethod(m)}
                        className={`${styles.tab} ${method === m ? styles.tabActive : styles.tabInactive}`}
                    >
                        {m === 'card' ? 'CARTÃO DE CRÉDITO' : 'PIX'}
                    </button>
                ))}
            </div>

            {method === 'card' && (
                <form onSubmit={handleSubmit} className={styles.cardForm}>
                    <div className={styles.fieldGroup}>
                        <label className={styles.label}>Número do cartão</label>
                        <input
                            type="text"
                            value={cardNumber}
                            onChange={(e) => setCardNumber(e.target.value)}
                            placeholder="0000 0000 0000 0000"
                            maxLength={19}
                            required
                            className={styles.input}
                        />
                    </div>
                    <div className={styles.fieldGroup}>
                        <label className={styles.label}>Nome no cartão</label>
                        <input
                            type="text"
                            value={cardName}
                            onChange={(e) => setCardName(e.target.value)}
                            placeholder="CARLOS SILVA"
                            required
                            className={`${styles.input} ${styles.inputUpper}`}
                        />
                    </div>
                    <div className={styles.grid2}>
                        <div className={styles.fieldGroup}>
                            <label className={styles.label}>Validade</label>
                            <input
                                type="text"
                                value={cardExpiry}
                                onChange={(e) => setCardExpiry(e.target.value)}
                                placeholder="MM/AA"
                                maxLength={5}
                                required
                                className={styles.input}
                            />
                        </div>
                        <div className={styles.fieldGroup}>
                            <label className={styles.label}>CVV</label>
                            <input
                                type="text"
                                value={cardCvv}
                                onChange={(e) => setCardCvv(e.target.value)}
                                placeholder="000"
                                maxLength={4}
                                required
                                className={styles.input}
                            />
                        </div>
                    </div>
                    <button type="submit" className={styles.submitBtn}>
                        CONFIRMAR PEDIDO →
                    </button>
                </form>
            )}

            {method === 'pix' && (
                <div className={styles.pixWrapper}>
                    <div className={styles.pixQr}>
                        <p className={styles.pixQrText}>
                            QR Code PIX
                            <br />
                            placeholder
                        </p>
                    </div>
                    <p className={styles.pixDesc}>
                        Escaneie o QR Code com o app do seu banco para pagar via PIX. Aprovação em
                        até 30 segundos.
                    </p>
                    <button onClick={onNext} className={styles.pixBtn}>
                        PAGAMENTO REALIZADO →
                    </button>
                </div>
            )}
        </div>
    )
}
