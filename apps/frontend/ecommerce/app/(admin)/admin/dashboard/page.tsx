import { MetricsCards } from '@/components/admin/MetricsCards/MetricsCards'
import { RevenueChart } from '@/components/admin/RevenueChart/RevenueChart'
import { TopProducts } from '@/components/admin/TopProducts/TopProducts'
import { RecentOrders } from '@/components/admin/RecentOrders/RecentOrders'
import styles from './page.module.css'

export default function DashboardPage() {
    return (
        <div>
            <h1 className={styles.title}>DASHBOARD</h1>
            <MetricsCards />
            <RevenueChart />
            <div className={styles.bottomGrid}>
                <TopProducts />
                <RecentOrders />
            </div>
        </div>
    )
}
