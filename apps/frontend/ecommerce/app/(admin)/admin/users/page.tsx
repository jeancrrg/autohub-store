import { Topbar } from '@/components/admin/Topbar/Topbar'
import { UsersTable } from '@/components/admin/UsersTable/UsersTable'
import styles from './page.module.css'

export default function AdminUsersPage() {
    return (
        <>
            <Topbar title="Usuários" subtitle="Clientes registrados na loja" />
            <div className={styles.page}>
                <UsersTable />
            </div>
        </>
    )
}
