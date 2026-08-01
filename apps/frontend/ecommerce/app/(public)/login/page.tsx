'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { login } from '@/lib/api/auth'
import styles from './page.module.css'

export default function LoginPage() {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const router = useRouter()
    const queryClient = useQueryClient()

    const loginMutation = useMutation({
        mutationFn: () => login(email, password),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey: ['session'] })
            router.push('/account')
        },
        onError: (error: any) => {
            setErrorMessage(error.response?.data?.detail ?? 'Falha ao entrar. Verifique suas credenciais.')
        },
    })

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        setErrorMessage(null)
        loginMutation.mutate()
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

                    {errorMessage && <p role="alert">{errorMessage}</p>}

                    <button type="submit" className={styles.submitButton} disabled={loginMutation.isPending}>
                        {loginMutation.isPending ? 'ENTRANDO...' : 'ENTRAR'}
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
