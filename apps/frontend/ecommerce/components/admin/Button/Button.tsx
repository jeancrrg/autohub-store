import styles from './Button.module.css'

type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger'
type ButtonSize = 'sm' | 'md' | 'lg'

const VARIANT_CLASS: Record<ButtonVariant, string> = {
    primary: styles.primary,
    secondary: styles.secondary,
    outline: styles.outline,
    ghost: styles.ghost,
    danger: styles.danger,
}

const SIZE_CLASS: Record<ButtonSize, string> = {
    sm: styles.sm,
    md: styles.md,
    lg: styles.lg,
}

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: ButtonVariant
    size?: ButtonSize
    iconLeft?: React.ReactNode
    iconRight?: React.ReactNode
    fullWidth?: boolean
}

export function Button({
    children,
    variant = 'primary',
    size = 'md',
    iconLeft,
    iconRight,
    fullWidth = false,
    className,
    type = 'button',
    ...rest
}: ButtonProps) {
    const classes = [
        styles.button,
        VARIANT_CLASS[variant],
        SIZE_CLASS[size],
        fullWidth ? styles.fullWidth : '',
        className ?? '',
    ].join(' ')

    return (
        <button type={type} className={classes} {...rest}>
            {iconLeft}
            {children}
            {iconRight}
        </button>
    )
}
