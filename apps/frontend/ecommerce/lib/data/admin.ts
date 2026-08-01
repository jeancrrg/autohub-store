import type { AdminUser } from '@/types/order'

export type KpiIcon = 'dollar' | 'bag' | 'users' | 'chart'

export type Kpi = {
    label: string
    value: string
    delta: string
    up: boolean
    accent: string
    spark: number[]
    icon: KpiIcon
}

export type SalesBar = {
    month: string
    label: string
    value: number
}

export type TopProduct = {
    rank: number
    name: string
    sold: string
    pct: number
}

export const MAX_SALES_VALUE = 120000

export const kpis: Kpi[] = [
    {
        label: 'Receita Total',
        value: 'R$ 284.590',
        delta: '+12.5%',
        up: true,
        accent: 'var(--accent)',
        spark: [38, 52, 45, 61, 78, 92, 88, 105, 98, 115, 120, 87],
        icon: 'dollar',
    },
    {
        label: 'Pedidos',
        value: '1.284',
        delta: '+8.2%',
        up: true,
        accent: 'var(--info)',
        spark: [60, 72, 68, 80, 90, 95, 91, 100, 96, 110, 118, 105],
        icon: 'bag',
    },
    {
        label: 'Clientes',
        value: '3.847',
        delta: '+15.3%',
        up: true,
        accent: 'var(--success)',
        spark: [20, 30, 28, 40, 48, 55, 60, 70, 75, 88, 95, 102],
        icon: 'users',
    },
    {
        label: 'Ticket Médio',
        value: 'R$ 1.247',
        delta: '-2.1%',
        up: false,
        accent: 'var(--performance)',
        spark: [110, 105, 108, 100, 95, 98, 92, 90, 88, 85, 82, 80],
        icon: 'chart',
    },
]

export const salesBars: SalesBar[] = [
    { month: 'Jan', label: 'R$ 38k', value: 38000 },
    { month: 'Fev', label: 'R$ 52k', value: 52000 },
    { month: 'Mar', label: 'R$ 45k', value: 45000 },
    { month: 'Abr', label: 'R$ 61k', value: 61000 },
    { month: 'Mai', label: 'R$ 78k', value: 78000 },
    { month: 'Jun', label: 'R$ 92k', value: 92000 },
    { month: 'Jul', label: 'R$ 88k', value: 88000 },
    { month: 'Ago', label: 'R$ 105k', value: 105000 },
    { month: 'Set', label: 'R$ 98k', value: 98000 },
    { month: 'Out', label: 'R$ 115k', value: 115000 },
    { month: 'Nov', label: 'R$ 120k', value: 120000 },
    { month: 'Dez', label: 'R$ 87k', value: 87000 },
]

export const topProducts: TopProduct[] = [
    { rank: 1, name: 'Rodas BBS RS Aro 18', sold: '10 vendas', pct: 100 },
    { rank: 2, name: 'Pneu Michelin PS4', sold: '20 vendas', pct: 92 },
    { rank: 3, name: 'Kit Freios Brembo GT', sold: '9 vendas', pct: 68 },
    { rank: 4, name: 'Suspensão KW Variant 3', sold: '4 vendas', pct: 40 },
    { rank: 5, name: 'Escapamento Akrapovic', sold: '3 vendas', pct: 25 },
]

export const adminUsers: AdminUser[] = [
    {
        id: 1,
        name: 'Carlos Silva',
        email: 'carlos@email.com',
        initials: 'CS',
        registeredAt: '10/01/2024',
        active: true,
    },
    {
        id: 2,
        name: 'Ana Souza',
        email: 'ana@email.com',
        initials: 'AS',
        registeredAt: '15/01/2024',
        active: true,
    },
    {
        id: 3,
        name: 'Pedro Costa',
        email: 'pedro@email.com',
        initials: 'PC',
        registeredAt: '22/01/2024',
        active: false,
    },
    {
        id: 4,
        name: 'Mariana Lima',
        email: 'mariana@email.com',
        initials: 'ML',
        registeredAt: '05/02/2024',
        active: true,
    },
    {
        id: 5,
        name: 'Rafael Oliveira',
        email: 'rafael@email.com',
        initials: 'RO',
        registeredAt: '12/02/2024',
        active: true,
    },
]
