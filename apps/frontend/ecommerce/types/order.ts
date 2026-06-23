export type OrderStatus =
    | 'Entregue'
    | 'Em trânsito'
    | 'Processando'
    | 'Cancelado'
    | 'Aguardando pagamento'

export type Order = {
    id: string
    date: string
    status: OrderStatus
    items: string
    total: string
}

export type AdminOrder = Order & {
    client: string
    statusColor: string
    statusBg: string
}

export type MockUser = {
    name: string
    email: string
    initials: string
    phone: string
    cpf: string
}

export type AdminUser = {
    id: number
    name: string
    email: string
    initials: string
    registeredAt: string
    active: boolean
}
