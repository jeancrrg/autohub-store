import type { AdminUser } from '@/types/order'

export type Metric = {
    label: string
    value: string
    change: string
    changePositive: boolean
    icon: string
}

export type SalesBar = {
    month: string
    label: string
    value: number
}

export type TopProduct = {
    rank: number
    name: string
    brand: string
    revenue: string
    sales: number
}

export const MAX_SALES_VALUE = 120000

export const metrics: Metric[] = [
    {
        label: 'Receita Total',
        value: 'R$ 284.590',
        change: '+12.5%',
        changePositive: true,
        icon: '💰',
    },
    {
        label: 'Pedidos',
        value: '1.284',
        change: '+8.2%',
        changePositive: true,
        icon: '📦',
    },
    {
        label: 'Clientes',
        value: '3.847',
        change: '+15.3%',
        changePositive: true,
        icon: '👥',
    },
    {
        label: 'Ticket Médio',
        value: 'R$ 1.247',
        change: '-2.1%',
        changePositive: false,
        icon: '🎯',
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
    {
        rank: 1,
        name: 'Rodas BBS RS Aro 18',
        brand: 'BBS',
        revenue: 'R$ 48.990',
        sales: 10,
    },
    {
        rank: 2,
        name: 'Suspensão KW Variant 3',
        brand: 'KW',
        revenue: 'R$ 33.999',
        sales: 4,
    },
    {
        rank: 3,
        name: 'Kit Freios Brembo GT',
        brand: 'Brembo',
        revenue: 'R$ 29.699',
        sales: 9,
    },
    {
        rank: 4,
        name: 'Escapamento Akrapovic',
        brand: 'Akrapovic',
        revenue: 'R$ 23.699',
        sales: 3,
    },
    {
        rank: 5,
        name: 'Pneu Michelin PS4',
        brand: 'Michelin',
        revenue: 'R$ 17.998',
        sales: 20,
    },
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
