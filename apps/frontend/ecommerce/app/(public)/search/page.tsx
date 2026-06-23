import { products } from '@/lib/data/products'
import { ProductCard } from '@/components/catalog/ProductCard/ProductCard'
import Link from 'next/link'
import styles from './page.module.css'

export default async function SearchPage({
    searchParams,
}: {
    searchParams: Promise<{ q?: string }>
}) {
    const { q } = await searchParams
    const query = q ?? ''
    const results = products.filter(
        (p) =>
            p.name.toLowerCase().includes(query.toLowerCase()) ||
            p.brand.toLowerCase().includes(query.toLowerCase()) ||
            p.category.toLowerCase().includes(query.toLowerCase())
    )

    return (
        <div className={styles.container}>
            <nav className={styles.breadcrumb}>
                <Link href="/" className={styles.breadcrumbLink}>
                    Início
                </Link>
                <span>/</span>
                <span className={styles.breadcrumbCurrent}>Busca</span>
            </nav>

            <h1 className={styles.title}>RESULTADOS DA BUSCA</h1>
            <p className={styles.subtitle}>
                {results.length} resultado{results.length !== 1 ? 's' : ''} para &ldquo;{query}
                &rdquo;
            </p>

            {results.length === 0 ? (
                <div className={styles.empty}>
                    <p className={styles.emptyText}>
                        Nenhum produto encontrado para &ldquo;{query}&rdquo;.
                    </p>
                    <Link href="/catalog" className={styles.emptyLink}>
                        VER CATÁLOGO COMPLETO →
                    </Link>
                </div>
            ) : (
                <div className={styles.resultsGrid}>
                    {results.map((p) => (
                        <ProductCard key={p.id} product={p} />
                    ))}
                </div>
            )}
        </div>
    )
}
