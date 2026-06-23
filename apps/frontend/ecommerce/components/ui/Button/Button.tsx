import { cn } from '@/lib/utils'
import styles from './Button.module.css'

type ButtonVariant = 'primary' | 'outline' | 'ghost'
type ButtonSize = 'sm' | 'md' | 'lg'

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: ButtonVariant
    size?: ButtonSize
}

const variantClass: Record<ButtonVariant, string> = {
    primary: styles.variantPrimary,
    outline: styles.variantOutline,
    ghost: styles.variantGhost,
}

const sizeClass: Record<ButtonSize, string> = {
    sm: styles.sizeSm,
    md: styles.sizeMd,
    lg: styles.sizeLg,
}

export function Button({
    variant = 'primary',
    size = 'md',
    className,
    children,
    ...props
}: ButtonProps) {
    return (
        <button
            className={cn(styles.base, variantClass[variant], sizeClass[size], className)}
            {...props}
        >
            {children}
        </button>
    )
}
