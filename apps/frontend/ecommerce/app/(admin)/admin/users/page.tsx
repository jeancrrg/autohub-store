import { UsersTable } from '@/components/admin/UsersTable/UsersTable'
import styles from './page.module.css'

export default function AdminUsersPage() {
    return (
        <div>
            <h1 className={styles.title}>USUÁRIOS</h1>
            <UsersTable />
        </div>
    )
}
