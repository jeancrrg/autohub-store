'use client'

import { useState } from 'react'
import { Button } from '@/components/admin/Button/Button'
import { CategoriesTable } from '@/components/admin/CategoriesTable/CategoriesTable'
import { CategoryForm } from '@/components/admin/CategoryForm/CategoryForm'
import { PlusIcon } from '@/components/admin/icons/Icons'
import { Topbar } from '@/components/admin/Topbar/Topbar'
import styles from './page.module.css'

export default function AdminCategoriesPage() {
    const [isCreating, setIsCreating] = useState(false)

    return (
        <>
            <Topbar
                title="Categorias"
                subtitle="Organização do catálogo"
                action={
                    <Button iconLeft={<PlusIcon size={16} />} onClick={() => setIsCreating(true)}>
                        Nova Categoria
                    </Button>
                }
            />
            <div className={styles.page}>
                {isCreating && <CategoryForm onClose={() => setIsCreating(false)} />}
                <CategoriesTable />
            </div>
        </>
    )
}
