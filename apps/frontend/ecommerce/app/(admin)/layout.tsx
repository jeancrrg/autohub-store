import { AdminSidebar } from '@/components/layout/AdminSidebar/AdminSidebar'

export default function AdminLayout({ children }: { children: React.ReactNode }) {
    return (
        <div className="flex min-h-screen bg-dark">
            <AdminSidebar />

            <div className="flex-1 flex flex-col">
                {/* Topbar */}
                <header className="bg-dark-2 border-b border-dark-border px-8 py-4 flex items-center justify-between">
                    <div className="flex-1 max-w-xs">
                        <input
                            type="text"
                            placeholder="Buscar..."
                            className="w-full bg-dark-3 border border-dark-border text-white font-inter text-sm px-4 py-2 placeholder:text-gray-600 focus:outline-none focus:border-primary transition-colors"
                        />
                    </div>

                    {/* Admin avatar */}
                    <div className="flex items-center gap-3">
                        <div className="w-9 h-9 bg-primary rounded-full flex items-center justify-center text-white font-rajdhani font-700 text-sm">
                            AD
                        </div>
                        <span className="font-inter text-sm text-gray-400">Admin</span>
                    </div>
                </header>

                {/* Page content */}
                <main className="flex-1 p-8">{children}</main>
            </div>
        </div>
    )
}
