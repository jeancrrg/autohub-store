'use client'

import { useState } from 'react'
import { useAuthStore } from '@/store/authStore'
import { Input } from '@/components/ui/Input/Input'
import styles from './ProfileTab.module.css'

export function ProfileTab() {
    const { user } = useAuthStore()
    const [saved, setSaved] = useState(false)

    function handleSave(e: React.FormEvent) {
        e.preventDefault()
        setSaved(true)
        setTimeout(() => setSaved(false), 2000)
    }

    return (
        <div className={styles.wrapper}>
            <h2 className={styles.title}>DADOS PESSOAIS</h2>
            <form onSubmit={handleSave} className={styles.form}>
                <Input label="Nome completo" defaultValue={user?.name ?? ''} />
                <Input label="E-mail" type="email" defaultValue={user?.email ?? ''} />
                <Input label="Telefone" defaultValue={user?.phone ?? ''} />
                <Input label="CPF" defaultValue={user?.cpf ?? ''} disabled />

                <button type="submit" className={styles.submitBtn}>
                    {saved ? 'SALVO ✓' : 'SALVAR ALTERAÇÕES'}
                </button>
            </form>
        </div>
    )
}
