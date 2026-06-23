'use client'

import { useState } from 'react'
import styles from './Newsletter.module.css'

export function Newsletter() {
    const [email, setEmail] = useState('')
    const [submitted, setSubmitted] = useState(false)

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        if (email.trim()) {
            setSubmitted(true)
            setEmail('')
        }
    }

    return (
        <section className={styles.section}>
            <div className={styles.bgPattern} />
            <div className={styles.inner}>
                <div className={styles.badge}>
                    <span className={styles.badgeText}>Newsletter</span>
                </div>
                <h2 className={styles.heading}>
                    FIQUE POR DENTRO
                    <br />
                    DAS NOVIDADES
                </h2>
                <p className={styles.description}>
                    Receba ofertas exclusivas, lançamentos e dicas de performance direto no seu
                    email.
                </p>

                {submitted ? (
                    <p className={styles.successMsg}>E-mail cadastrado com sucesso!</p>
                ) : (
                    <form onSubmit={handleSubmit} className={styles.form}>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="seu@email.com"
                            required
                            className={styles.emailInput}
                        />
                        <button type="submit" className={styles.subscribeBtn}>
                            ASSINAR AGORA
                        </button>
                    </form>
                )}
                <div className={styles.disclaimer}>Sem spam. Cancele quando quiser.</div>
            </div>
        </section>
    )
}
