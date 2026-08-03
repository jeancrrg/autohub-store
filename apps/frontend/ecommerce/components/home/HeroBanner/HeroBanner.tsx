import Link from 'next/link'
import Image from 'next/image'
import styles from './HeroBanner.module.css'

export function HeroBanner() {
    return (
        <section className={styles.section}>
            <div className={styles.gridPattern} />
            <div className={styles.diagonalLine} />

            <div className={styles.rightPanel}>
                <div className={styles.carImageWrapper}>
                    <div className={styles.carImage}>
                        <Image
                            src="/images/hero-car.jpg"
                            alt="Honda Civic Type R"
                            fill
                            sizes="(max-width: 768px) 100vw, 55vw"
                            style={{ objectFit: 'cover', objectPosition: 'center right' }}
                            priority
                        />
                    </div>
                </div>
                <div className={styles.leftGradient} />
                <div className={styles.accentLine1} />
                <div className={styles.accentLine2} />
            </div>

            <div className={styles.content}>
                <div className={styles.labelRow}>
                    <div className={styles.labelLine} />
                    <span className={styles.labelText}>Performance Premium</span>
                </div>

                <h1 className={styles.heading}>
                    PERFORMANCE
                    <br />
                    <span className={styles.headingAccent}>SEM</span>
                    <br />
                    LIMITES
                </h1>

                <p className={styles.description}>
                    As melhores peças automotivas, rodas de alta performance e acessórios premium
                    para o seu veículo.
                </p>

                <div className={styles.btnRow}>
                    <Link href="/catalog" className={styles.btnPrimary}>
                        COMPRAR AGORA
                    </Link>
                    <Link href="/catalog" className={styles.btnOutline}>
                        VER CATÁLOGO
                    </Link>
                </div>

                <div className={styles.stats}>
                    <div>
                        <div className={styles.statValue}>50k+</div>
                        <div className={styles.statLabel}>Produtos</div>
                    </div>
                    <div className={styles.statDivider} />
                    <div>
                        <div className={styles.statValue}>200+</div>
                        <div className={styles.statLabel}>Marcas</div>
                    </div>
                    <div className={styles.statDivider} />
                    <div>
                        <div className={styles.statValue}>24/7</div>
                        <div className={styles.statLabel}>Suporte</div>
                    </div>
                </div>
            </div>
        </section>
    )
}
