import { products } from '@/lib/data/products'
import { HeroBanner } from '@/components/home/HeroBanner/HeroBanner'
import { TrustBadges } from '@/components/home/TrustBadges/TrustBadges'
import { CategoryGrid } from '@/components/home/CategoryGrid/CategoryGrid'
import { ProductSection } from '@/components/home/ProductSection/ProductSection'
import { PromoBanner } from '@/components/home/PromoBanner/PromoBanner'
import { BrandsGrid } from '@/components/home/BrandsGrid/BrandsGrid'
import { Newsletter } from '@/components/home/Newsletter/Newsletter'

export default function HomePage() {
    const bestSellers = products.slice(0, 4)

    return (
        <>
            <HeroBanner />
            <TrustBadges />
            <CategoryGrid />
            <ProductSection
                title="Produtos em Destaque"
                subtitle="Curadoria"
                products={products}
                bgGray
            />
            <PromoBanner />
            <ProductSection
                title="Mais Vendidos"
                subtitle="Top Sellers"
                products={bestSellers}
                showRank
            />
            <BrandsGrid />
            <Newsletter />
        </>
    )
}
