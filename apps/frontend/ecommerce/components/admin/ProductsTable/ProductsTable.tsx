'use client'

import { useState } from 'react'
import { useDeleteProduct } from '@/hooks/useDeleteProduct'
import { useProducts } from '@/hooks/useProducts'
import type { Product } from '@/types/product'
import { Badge } from '../Badge/Badge'
import { DataTable, type DataTableColumn } from '../DataTable/DataTable'
import { Dialog } from '../Dialog/Dialog'
import { EditIcon, TrashIcon } from '../icons/Icons'
import styles from './ProductsTable.module.css'

type StockFilter = 'all' | 'in-stock' | 'out-of-stock'

const FILTERS: Array<{ id: StockFilter; label: string }> = [
    { id: 'all', label: 'Todos' },
    { id: 'in-stock', label: 'Em estoque' },
    { id: 'out-of-stock', label: 'Esgotado' },
]

export function ProductsTable() {
    const { data: products, isLoading, isError } = useProducts()
    const deleteProductMutation = useDeleteProduct()
    const [pendingDelete, setPendingDelete] = useState<Product | null>(null)
    const [filter, setFilter] = useState<StockFilter>('all')

    function confirmDelete() {
        if (!pendingDelete) {
            return
        }
        deleteProductMutation.mutate(pendingDelete.id)
        setPendingDelete(null)
    }

    if (isLoading) {
        return <p className={styles.state}>Carregando produtos...</p>
    }

    if (isError) {
        return <p className={styles.state}>Não foi possível carregar os produtos.</p>
    }

    const allRows = products ?? []
    const rows = allRows.filter((product) => {
        if (filter === 'in-stock') return product.inStock
        if (filter === 'out-of-stock') return !product.inStock
        return true
    })

    const columns: Array<DataTableColumn<Product>> = [
        {
            key: 'img',
            label: '',
            width: 60,
            render: () => <div className={styles.imgPlaceholder}>🔧</div>,
        },
        {
            key: 'name',
            label: 'Produto',
            render: (row) => (
                <div>
                    <p className={styles.productName}>{row.name}</p>
                    <p className={styles.productBrand}>{row.brand}</p>
                </div>
            ),
        },
        { key: 'category', label: 'Categoria' },
        {
            key: 'price',
            label: 'Preço',
            align: 'right',
            mono: true,
            render: (row) => `R$ ${row.price.toFixed(2).replace('.', ',')}`,
        },
        {
            key: 'stock',
            label: 'Estoque',
            align: 'center',
            render: (row) => <Badge status={row.inStock ? 'in-stock' : 'out-of-stock'} size="sm" />,
        },
        {
            key: 'actions',
            label: '',
            align: 'right',
            width: 90,
            render: (row) => (
                <div className={styles.actions}>
                    <button className={styles.rowBtn} aria-label="Editar produto">
                        <EditIcon size={16} />
                    </button>
                    <button
                        className={`${styles.rowBtn} ${styles.rowBtnDanger}`}
                        onClick={(e) => {
                            e.stopPropagation()
                            setPendingDelete(row)
                        }}
                        disabled={deleteProductMutation.isPending}
                        aria-label="Excluir produto"
                    >
                        <TrashIcon size={16} />
                    </button>
                </div>
            ),
        },
    ]

    return (
        <>
            <div className={styles.pills}>
                {FILTERS.map(({ id, label }) => (
                    <button
                        key={id}
                        onClick={() => setFilter(id)}
                        className={`${styles.pill} ${filter === id ? styles.pillActive : ''}`}
                    >
                        {label}
                    </button>
                ))}
            </div>
            <DataTable
                columns={columns}
                rows={rows}
                rowKey={(row) => row.id}
                emptyMessage="Nenhum produto cadastrado."
            />
            <Dialog
                open={pendingDelete !== null}
                title="Excluir produto?"
                description={pendingDelete ? `"${pendingDelete.name}" será removido permanentemente do catálogo. Essa ação não pode ser desfeita.` : ''}
                confirmLabel="Excluir produto"
                onConfirm={confirmDelete}
                onCancel={() => setPendingDelete(null)}
            />
        </>
    )
}
