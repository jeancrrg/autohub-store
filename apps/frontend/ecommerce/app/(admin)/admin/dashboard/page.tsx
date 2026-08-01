import { Card } from '@/components/admin/Card/Card'
import { KpiCard } from '@/components/admin/KpiCard/KpiCard'
import { RecentOrders } from '@/components/admin/RecentOrders/RecentOrders'
import { SalesChart } from '@/components/admin/SalesChart/SalesChart'
import { Topbar } from '@/components/admin/Topbar/Topbar'
import { TopProductsList } from '@/components/admin/TopProductsList/TopProductsList'
import { kpis, salesBars, topProducts } from '@/lib/data/admin'
import styles from './page.module.css'

export default function DashboardPage() {
    return (
        <>
            <Topbar title="Dashboard" subtitle="Visão geral da loja · atualizado agora" />
            <div className={styles.page}>
                <div className={styles.kpiGrid}>
                    {kpis.map((kpi) => (
                        <KpiCard key={kpi.label} {...kpi} />
                    ))}
                </div>
                <div className={styles.mainGrid}>
                    <Card title="Receita" action={<span className={styles.periodLabel}>Últimos 12 meses</span>}>
                        <SalesChart bars={salesBars} />
                    </Card>
                    <Card title="Mais Vendidos">
                        <TopProductsList products={topProducts} />
                    </Card>
                </div>
                <Card title="Últimos Pedidos" flush action={<span className={styles.actionLink}>Ver todos</span>}>
                    <RecentOrders />
                </Card>
            </div>
        </>
    )
}
