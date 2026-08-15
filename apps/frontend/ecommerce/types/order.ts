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

export type MockUser = {
    name: string
    email: string
    initials: string
    phone: string
    cpf: string
}
