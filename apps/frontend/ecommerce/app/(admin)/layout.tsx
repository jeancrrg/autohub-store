import { Sidebar } from '@/components/admin/Sidebar/Sidebar'
import './admin-tokens.css'

export default function AdminLayout({ children }: { children: React.ReactNode }) {
    return (
        <div className="admin-root" style={{ display: 'flex', minHeight: '100vh' }}>
            <Sidebar />
            <div style={{ flex: 1, minWidth: 0 }}>{children}</div>
        </div>
    )
}
