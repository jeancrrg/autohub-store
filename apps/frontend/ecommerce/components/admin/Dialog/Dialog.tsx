import { Button } from '../Button/Button'
import styles from './Dialog.module.css'

type DialogProps = {
    open: boolean
    title: string
    description: string
    confirmLabel: string
    onConfirm: () => void
    onCancel: () => void
}

export function Dialog({ open, title, description, confirmLabel, onConfirm, onCancel }: DialogProps) {
    if (!open) {
        return null
    }

    return (
        <div className={styles.scrim} onClick={onCancel}>
            <div className={styles.dialog} onClick={(e) => e.stopPropagation()}>
                <span className={styles.accentDanger} />
                <h2 className={styles.title}>{title}</h2>
                <p className={styles.description}>{description}</p>
                <div className={styles.actions}>
                    <Button variant="outline" onClick={onCancel}>
                        Cancelar
                    </Button>
                    <Button variant="danger" onClick={onConfirm}>
                        {confirmLabel}
                    </Button>
                </div>
            </div>
        </div>
    )
}
