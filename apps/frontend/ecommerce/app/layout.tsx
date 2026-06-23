import type { Metadata } from 'next'
import { Rajdhani, Exo_2, Inter } from 'next/font/google'
import { StoreProvider } from '@/components/providers/StoreProvider'
import './globals.css'

const rajdhani = Rajdhani({
    subsets: ['latin'],
    weight: ['500', '600', '700'],
    variable: '--font-rajdhani',
    display: 'swap',
})

const exo2 = Exo_2({
    subsets: ['latin'],
    weight: ['400', '500', '600', '700', '800'],
    variable: '--font-exo2',
    display: 'swap',
})

const inter = Inter({
    subsets: ['latin'],
    weight: ['400', '500', '600'],
    variable: '--font-inter',
    display: 'swap',
})

export const metadata: Metadata = {
    title: 'AutoHubStore',
    description: 'E-commerce automotivo',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
    return (
        <html lang="pt-BR" className={`${rajdhani.variable} ${exo2.variable} ${inter.variable}`}>
            <body className="font-inter antialiased" suppressHydrationWarning>
                <StoreProvider>{children}</StoreProvider>
            </body>
        </html>
    )
}
