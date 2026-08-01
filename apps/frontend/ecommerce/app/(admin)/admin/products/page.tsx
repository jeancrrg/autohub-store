'use client'

import { useState } from 'react'
import { Button } from '@/components/admin/Button/Button'
import { PlusIcon } from '@/components/admin/icons/Icons'
import { ProductForm } from '@/components/admin/ProductForm/ProductForm'
import { ProductsTable } from '@/components/admin/ProductsTable/ProductsTable'
import { Topbar } from '@/components/admin/Topbar/Topbar'
import styles from './page.module.css'

export default function AdminProductsPage() {
    const [isCreating, setIsCreating] = useState(false)

    return (
        <>
            <Topbar
                title="Produtos"
                subtitle="Catálogo da loja"
                action={
                    <Button iconLeft={<PlusIcon size={16} />} onClick={() => setIsCreating(true)}>
                        Novo Produto
                    </Button>
                }
            />
            <div className={styles.page}>
                {isCreating && <ProductForm onClose={() => setIsCreating(false)} />}
                <ProductsTable />
            </div>
        </>
    )
}
