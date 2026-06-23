'use client'

import styles from './Pagination.module.css'

type PaginationProps = {
    currentPage: number
    totalPages: number
    onPageChange: (page: number) => void
}

export function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
    const pages = Array.from({ length: Math.min(totalPages, 5) }, (_, i) => i + 1)

    return (
        <div className={styles.wrapper}>
            <button
                onClick={() => onPageChange(currentPage - 1)}
                disabled={currentPage === 1}
                className={styles.pageBtn}
            >
                ‹
            </button>

            {pages.map((p) => (
                <button
                    key={p}
                    onClick={() => onPageChange(p)}
                    className={`${styles.pageBtn} ${p === currentPage ? styles.pageBtnActive : ''}`}
                >
                    {p}
                </button>
            ))}

            {totalPages > 5 && (
                <>
                    <span className={styles.ellipsis}>…</span>
                    <button onClick={() => onPageChange(totalPages)} className={styles.pageBtn}>
                        {totalPages}
                    </button>
                </>
            )}

            <button
                onClick={() => onPageChange(currentPage + 1)}
                disabled={currentPage === totalPages}
                className={styles.pageBtn}
            >
                ›
            </button>
        </div>
    )
}
