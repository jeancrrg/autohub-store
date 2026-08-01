'use client'

import { useState } from 'react'
import { ProductsTable } from '@/components/admin/ProductsTable/ProductsTable'
import { ProductForm } from '@/components/admin/ProductForm/ProductForm'
import styles from './page.module.css'

export default function AdminProductsPage() {
    const [isCreating, setIsCreating] = useState(false)

    return (
        <div>
            <div className={styles.header}>
                <h1 className={styles.title}>PRODUTOS</h1>
                <button className={styles.addButton} onClick={() => setIsCreating(true)}>
                    + NOVO PRODUTO
                </button>
            </div>
            {isCreating && <ProductForm onClose={() => setIsCreating(false)} />}
            <ProductsTable />
        </div>
    )
}
