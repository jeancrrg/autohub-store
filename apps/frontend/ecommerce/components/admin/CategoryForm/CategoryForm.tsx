'use client'

import { useState } from 'react'
import { useCreateCategory } from '@/hooks/useCreateCategory'
import { Button } from '../Button/Button'
import styles from './CategoryForm.module.css'

export function CategoryForm({ onClose }: { onClose: () => void }) {
    const [name, setName] = useState('')
    const [slug, setSlug] = useState('')
    const createCategoryMutation = useCreateCategory()

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        createCategoryMutation.mutate({ name, slug }, { onSuccess: onClose })
    }

    return (
        <form onSubmit={handleSubmit} className={styles.form}>
            <label className={styles.field}>
                Nome
                <input value={name} onChange={(e) => setName(e.target.value)} required />
            </label>
            <label className={styles.field}>
                Slug
                <input
                    value={slug}
                    onChange={(e) => setSlug(e.target.value)}
                    placeholder="ex: rodas-esportivas"
                    required
                />
            </label>
            {createCategoryMutation.isError && <p className={styles.error}>Falha ao criar categoria.</p>}
            <Button type="submit" disabled={createCategoryMutation.isPending} fullWidth>
                {createCategoryMutation.isPending ? 'CRIANDO...' : 'CRIAR CATEGORIA'}
            </Button>
        </form>
    )
}
