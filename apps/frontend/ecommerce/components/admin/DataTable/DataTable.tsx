import styles from './DataTable.module.css'

export type DataTableColumn<T> = {
    key: string
    label: string
    align?: 'left' | 'center' | 'right'
    width?: number
    mono?: boolean
    render?: (row: T) => React.ReactNode
}

type DataTableProps<T> = {
    columns: Array<DataTableColumn<T>>
    rows: T[]
    rowKey: (row: T) => string | number
    onRowClick?: (row: T) => void
    emptyMessage?: string
}

export function DataTable<T extends Record<string, unknown>>({
    columns,
    rows,
    rowKey,
    onRowClick,
    emptyMessage = 'Nenhum registro encontrado.',
}: DataTableProps<T>) {
    return (
        <div className={styles.wrapper}>
            <table className={styles.table}>
                <thead>
                    <tr className={styles.headRow}>
                        {columns.map((column) => (
                            <th
                                key={column.key}
                                className={styles.th}
                                style={{ textAlign: column.align ?? 'left', width: column.width }}
                            >
                                {column.label}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {rows.map((row) => (
                        <tr
                            key={rowKey(row)}
                            className={`${styles.row} ${onRowClick ? styles.rowClickable : ''}`}
                            onClick={onRowClick ? () => onRowClick(row) : undefined}
                        >
                            {columns.map((column) => (
                                <td
                                    key={column.key}
                                    className={`${styles.td} ${column.mono ? styles.mono : ''}`}
                                    style={{ textAlign: column.align ?? 'left' }}
                                >
                                    {column.render ? column.render(row) : (row[column.key] as React.ReactNode)}
                                </td>
                            ))}
                        </tr>
                    ))}
                </tbody>
            </table>
            {rows.length === 0 && <p className={styles.empty}>{emptyMessage}</p>}
        </div>
    )
}
