'use client'

import { useState } from 'react'
import { useCreateProduct } from '@/hooks/useCreateProduct'
import { Button } from '../Button/Button'
import { ProductImageUpload } from '../ProductImageUpload/ProductImageUpload'
import styles from './ProductForm.module.css'

export function ProductForm({ onClose }: { onClose: () => void }) {
    const [name, setName] = useState('')
    const [description, setDescription] = useState('')
    const [price, setPrice] = useState('')
    const [stockQuantity, setStockQuantity] = useState('')
    const [categoryId, setCategoryId] = useState('')
    const [createdProductId, setCreatedProductId] = useState<string | null>(null)
    const createProductMutation = useCreateProduct()

    function handleSubmit(e: React.FormEvent) {
        e.preventDefault()
        createProductMutation.mutate(
            { name, description, price: Number(price), stockQuantity: Number(stockQuantity), categoryId },
            { onSuccess: (result) => setCreatedProductId(result.id) }
        )
    }

    if (createdProductId) {
        return <ProductImageUpload productId={createdProductId} onDone={onClose} />
    }

    return (
        <form onSubmit={handleSubmit} className={styles.form}>
            <label className={styles.field}>
                Nome
                <input value={name} onChange={(e) => setName(e.target.value)} required />
            </label>
            <label className={styles.field}>
                Descrição
                <textarea value={description} onChange={(e) => setDescription(e.target.value)} />
            </label>
            <label className={styles.field}>
                Preço
                <input type="number" step="0.01" value={price} onChange={(e) => setPrice(e.target.value)} required />
            </label>
            <label className={styles.field}>
                Estoque
                <input type="number" value={stockQuantity} onChange={(e) => setStockQuantity(e.target.value)} required />
            </label>
            <label className={styles.field}>
                Categoria (ID)
                <input value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required />
            </label>
            {createProductMutation.isError && <p className={styles.error}>Falha ao criar produto.</p>}
            <Button type="submit" disabled={createProductMutation.isPending} fullWidth>
                {createProductMutation.isPending ? 'CRIANDO...' : 'CRIAR PRODUTO'}
            </Button>
        </form>
    )
}
