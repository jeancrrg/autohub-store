'use client'

import { useState } from 'react'
import styles from './DeliveryStep.module.css'

type DeliveryStepProps = {
    onNext: () => void
}

const SHIPPING_OPTIONS = [
    { id: 'sedex', label: 'SEDEX', eta: '1–2 dias úteis', price: 'R$ 29,90' },
    { id: 'pac', label: 'PAC', eta: '5–8 dias úteis', price: 'R$ 14,90' },
]

export function DeliveryStep({ onNext }: DeliveryStepProps) {
    const [selected, setSelected] = useState('sedex')

    return (
        <div className={styles.wrapper}>
            <h3 className={styles.title}>OPÇÕES DE ENTREGA</h3>

            <div className={styles.optionList}>
                {SHIPPING_OPTIONS.map((opt) => (
                    <label
                        key={opt.id}
                        className={`${styles.option} ${selected === opt.id ? styles.optionSelected : styles.optionUnselected}`}
                    >
                        <input
                            type="radio"
                            name="shipping"
                            value={opt.id}
                            checked={selected === opt.id}
                            onChange={() => setSelected(opt.id)}
                            style={{ accentColor: 'var(--color-primary)' }}
                        />
                        <div className={styles.optionInfo}>
                            <p className={styles.optionLabel}>{opt.label}</p>
                            <p className={styles.optionEta}>{opt.eta}</p>
                        </div>
                        <span className={styles.optionPrice}>{opt.price}</span>
                    </label>
                ))}
            </div>

            <button onClick={onNext} className={styles.nextBtn}>
                CONTINUAR →
            </button>
        </div>
    )
}
