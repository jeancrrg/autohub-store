import { cn } from '@/lib/utils'
import styles from './Badge.module.css'

type BadgeProps = {
    children: React.ReactNode
    className?: string
    bgClass?: string
}

export function Badge({ children, className, bgClass }: BadgeProps) {
    return <span className={cn(styles.base, bgClass, className)}>{children}</span>
}
