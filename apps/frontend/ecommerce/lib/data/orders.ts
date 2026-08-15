import type { Order } from '@/types/order'

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
