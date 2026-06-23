import type { Order, AdminOrder } from '@/types/order'

export const customerOrders: Order[] = [
    {
        id: '#AH-2024-001',
        date: '15/01/2024',
        status: 'Entregue',
        items: 'Rodas BBS RS 18" x2',
        total: 'R$ 9.799,80',
    },
    {
        id: '#AH-2024-002',
        date: '28/02/2024',
        status: 'Em trânsito',
        items: 'Pneu Michelin PS4 x4',
        total: 'R$ 3.599,60',
    },
    {
        id: '#AH-2024-003',
        date: '10/03/2024',
        status: 'Processando',
        items: 'Kit Freios Brembo GT x1',
        total: 'R$ 3.299,90',
    },
]

export const adminOrders: AdminOrder[] = [
    {
        id: '#AH-2024-001',
        client: 'Carlos Silva',
        date: '15/01/2024',
        status: 'Entregue',
        items: 'Rodas BBS RS 18"',
        total: 'R$ 9.799,80',
        statusColor: 'text-green-400',
        statusBg: 'bg-green-400/10',
    },
    {
        id: '#AH-2024-002',
        client: 'Ana Souza',
        date: '28/02/2024',
        status: 'Em trânsito',
        items: 'Pneu Michelin PS4',
        total: 'R$ 3.599,60',
        statusColor: 'text-blue-400',
        statusBg: 'bg-blue-400/10',
    },
    {
        id: '#AH-2024-003',
        client: 'Pedro Costa',
        date: '10/03/2024',
        status: 'Processando',
        items: 'Kit Freios Brembo GT',
        total: 'R$ 3.299,90',
        statusColor: 'text-yellow-400',
        statusBg: 'bg-yellow-400/10',
    },
    {
        id: '#AH-2024-004',
        client: 'Mariana Lima',
        date: '22/03/2024',
        status: 'Cancelado',
        items: 'Amortecedor Bilstein B6',
        total: 'R$ 1.299,90',
        statusColor: 'text-red-400',
        statusBg: 'bg-red-400/10',
    },
    {
        id: '#AH-2024-005',
        client: 'Rafael Oliveira',
        date: '05/04/2024',
        status: 'Aguardando pagamento',
        items: 'Suspensão KW V3',
        total: 'R$ 8.499,90',
        statusColor: 'text-orange-400',
        statusBg: 'bg-orange-400/10',
    },
]
