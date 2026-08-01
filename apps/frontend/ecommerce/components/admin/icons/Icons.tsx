type IconProps = {
    size?: number
    stroke?: string
    strokeWidth?: number
}

function Icon({ size = 20, stroke = 'currentColor', strokeWidth = 1.75, children }: IconProps & { children: React.ReactNode }) {
    return (
        <svg
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke={stroke}
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeLinejoin="round"
        >
            {children}
        </svg>
    )
}

export function GridIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <rect x="3" y="3" width="7" height="7" rx="1" />
            <rect x="14" y="3" width="7" height="7" rx="1" />
            <rect x="3" y="14" width="7" height="7" rx="1" />
            <rect x="14" y="14" width="7" height="7" rx="1" />
        </Icon>
    )
}

export function BoxIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M21 8 12 3 3 8v8l9 5 9-5z" />
            <path d="M3 8l9 5 9-5M12 13v8" />
        </Icon>
    )
}

export function BagIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M6 8h12l-1 12H7z" />
            <path d="M9 8a3 3 0 0 1 6 0" />
        </Icon>
    )
}

export function UsersIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <circle cx="9" cy="8" r="3" />
            <path d="M3 20a6 6 0 0 1 12 0" />
            <path d="M16 5.5a3 3 0 0 1 0 5M21 20a6 6 0 0 0-4-5.6" />
        </Icon>
    )
}

export function ChartIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M3 3v18h18" />
            <path d="M7 14l3-4 3 3 5-7" />
        </Icon>
    )
}

export function BoltIcon({ size = 20, stroke = 'currentColor' }: IconProps) {
    return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill={stroke}>
            <path d="M13 2 4 14h6l-1 8 9-12h-6z" />
        </svg>
    )
}

export function SearchIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <circle cx="11" cy="11" r="7" />
            <path d="m21 21-4.3-4.3" />
        </Icon>
    )
}

export function BellIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" />
            <path d="M10 19a2 2 0 0 0 4 0" />
        </Icon>
    )
}

export function UpIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M7 14l5-5 5 5" />
        </Icon>
    )
}

export function DownIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M7 10l5 5 5-5" />
        </Icon>
    )
}

export function MoreIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <circle cx="5" cy="12" r="1.4" />
            <circle cx="12" cy="12" r="1.4" />
            <circle cx="19" cy="12" r="1.4" />
        </Icon>
    )
}

export function EditIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M4 20h4L18 10l-4-4L4 16z" />
            <path d="M13 7l4 4" />
        </Icon>
    )
}

export function TrashIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13" />
        </Icon>
    )
}

export function PlusIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M12 5v14M5 12h14" />
        </Icon>
    )
}

export function FilterIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M3 5h18l-7 8v6l-4-2v-4z" />
        </Icon>
    )
}

export function LogoutIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M14 4h4a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-4M9 12h11M16 8l4 4-4 4" />
        </Icon>
    )
}

export function UploadIcon(p: IconProps) {
    return (
        <Icon {...p}>
            <path d="M12 16V4M7 9l5-5 5 5" />
            <path d="M4 16v3a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-3" />
        </Icon>
    )
}
