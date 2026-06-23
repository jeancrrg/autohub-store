import { cn } from '@/lib/utils'
import { forwardRef } from 'react'
import styles from './Input.module.css'

type InputProps = React.InputHTMLAttributes<HTMLInputElement> & {
    label?: string
    error?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
    ({ label, error, className, ...props }, ref) => {
        return (
            <div className={styles.wrapper}>
                {label && <label className={styles.label}>{label}</label>}
                <input
                    ref={ref}
                    className={cn(styles.input, error && styles.inputError, className)}
                    {...props}
                />
                {error && <span className={styles.errorMsg}>{error}</span>}
            </div>
        )
    }
)

Input.displayName = 'Input'
