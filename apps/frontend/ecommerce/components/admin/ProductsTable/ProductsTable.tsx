'use client'

import { useProducts } from '@/hooks/useProducts'
import { useDeleteProduct } from '@/hooks/useDeleteProduct'
import styles from './ProductsTable.module.css'

export function ProductsTable() {
    const { data: products, isLoading, isError } = useProducts()
    const deleteProductMutation = useDeleteProduct()

    function handleDelete(id: string) {
        deleteProductMutation.mutate(id)
    }

    if (isLoading) {
        return <p>Carregando produtos...</p>
    }

    if (isError) {
        return <p>Não foi possível carregar os produtos.</p>
    }

    const rows = products ?? []

    return (
        <div className={styles.wrapper}>
            <div className={styles.tableHeader}>
                {['IMG', 'PRODUTO', 'CATEGORIA', 'PREÇO', 'ESTOQUE', 'STATUS', 'AÇÕES'].map((h) => (
                    <span key={h} className={styles.headerCell}>
                        {h}
                    </span>
                ))}
            </div>

            {rows.map((product) => (
                <div key={product.id} className={styles.row}>
                    <div className={styles.imgPlaceholder}>🔧</div>

                    <div>
                        <p className={styles.productName}>{product.name}</p>
                        <p className={styles.productBrand}>{product.brand}</p>
                    </div>

                    <span className={styles.category}>{product.category}</span>

                    <span className={styles.price}>
                        R$ {product.price.toFixed(2).replace('.', ',')}
                    </span>

                    <span className={product.inStock ? styles.stockIn : styles.stockOut}>
                        {product.inStock ? 'Em estoque' : 'Esgotado'}
                    </span>

                    <span
                        className={`${styles.statusBadge} ${product.inStock ? styles.statusActive : styles.statusInactive}`}
                    >
                        {product.inStock ? 'ATIVO' : 'INATIVO'}
                    </span>

                    <div className={styles.actions}>
                        <button className={styles.editBtn}>Editar</button>
                        <button
                            onClick={() => handleDelete(product.id)}
                            disabled={deleteProductMutation.isPending}
                            className={styles.deleteBtn}
                        >
                            Excluir
                        </button>
                    </div>
                </div>
            ))}

            {rows.length === 0 && (
                <div className={styles.empty}>
                    <p className={styles.emptyText}>Nenhum produto cadastrado.</p>
                </div>
            )}
        </div>
    )
}
