import Link from 'next/link'
import styles from './PromoBanner.module.css'

export function PromoBanner() {
    return (
        <section className={styles.section}>
            <div className={styles.bgPattern} />
            <div className={styles.leftAccent} />
            <div className={styles.inner}>
                <div>
                    <div className={styles.tagline}>Oferta Exclusiva</div>
                    <div className={styles.heading}>
                        ATÉ <span className={styles.headingAccent}>40% OFF</span>
                        <br />
                        EM SUSPENSÃO E FREIOS
                    </div>
                    <div className={styles.subtitle}>
                        Promoção válida por tempo limitado. Não perca!
                    </div>
                </div>
                <Link href="/catalog" className={styles.promoBtn}>
                    VER PROMOÇÕES
                </Link>
            </div>
        </section>
    )
}
