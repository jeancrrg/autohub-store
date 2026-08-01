'use client'

import { useCategories } from '@/hooks/useCategories'
import type { AdminCategory } from '@/types/category'
import { DataTable, type DataTableColumn } from '../DataTable/DataTable'
import styles from './CategoriesTable.module.css'

export function CategoriesTable() {
    const { data: categories, isLoading, isError } = useCategories()

    if (isLoading) {
        return <p className={styles.state}>Carregando categorias...</p>
    }

    if (isError) {
        return <p className={styles.state}>Não foi possível carregar as categorias.</p>
    }

    const rows = categories ?? []
    const nameById = new Map(rows.map((category) => [category.id, category.name]))

    const columns: Array<DataTableColumn<AdminCategory>> = [
        {
            key: 'name',
            label: 'Nome',
            render: (row) => <span className={styles.name}>{row.name}</span>,
        },
        {
            key: 'slug',
            label: 'Slug',
            mono: true,
            render: (row) => <span className={styles.slug}>{row.slug}</span>,
        },
        {
            key: 'parentId',
            label: 'Categoria pai',
            render: (row) => {
                const parentName = row.parentId ? nameById.get(row.parentId) : null
                return (
                    <span className={parentName ? styles.parent : styles.parentEmpty}>
                        {parentName ?? 'Raiz'}
                    </span>
                )
            },
        },
        { key: 'createdAt', label: 'Criado em', align: 'right', mono: true, width: 110 },
    ]

    return (
        <DataTable
            columns={columns}
            rows={rows}
            rowKey={(row) => row.id}
            emptyMessage="Nenhuma categoria cadastrada."
        />
    )
}
