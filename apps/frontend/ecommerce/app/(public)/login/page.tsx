'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import styles from './page.module.css'

export default function LoginPage() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const login = useAuthStore((s) => s.login)
    const router = useRouter()

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        login()
        router.push('/account')
    }

    return (
        <div className={styles.pageWrapper}>
            <div className={styles.formCard}>
                <div className={styles.logoArea}>
                    <p className={styles.logoTitle}>AUTOHUB</p>
                    <p className={styles.logoSubtitle}>STORE</p>
                </div>

                <h1 className={styles.heading}>ENTRAR NA CONTA</h1>

                <form onSubmit={handleSubmit} className={styles.form}>
                    <div>
                        <label className={styles.fieldLabel}>E-MAIL</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="seu@email.com"
                            required
                            className={styles.fieldInput}
                        />
                    </div>

                    <div>
                        <label className={styles.fieldLabel}>SENHA</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••"
                            required
                            className={styles.fieldInput}
                        />
                    </div>

                    <button type="submit" className={styles.submitButton}>
                        ENTRAR
                    </button>
                </form>

                <p className={styles.registerText}>
                    Não tem conta?{' '}
                    <a href="/login" className={styles.registerLink}>
                        Criar conta grátis
                    </a>
                </p>
            </div>
        </div>
    )
}
