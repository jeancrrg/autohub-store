import Link from 'next/link'
import styles from './Footer.module.css'

export function Footer() {
    return (
        <footer className={styles.footer}>
            <div className={styles.footerGrid}>
                <div>
                    <p className={styles.brandName}>AUTOHUB</p>
                    <p className={styles.brandAccent}>STORE</p>
                    <p className={styles.brandDesc}>
                        O melhor em peças e acessórios automotivos de alta performance.
                    </p>
                </div>

                <div>
                    <p className={styles.colTitle}>CATEGORIAS</p>
                    {['Rodas', 'Pneus', 'Suspensão', 'Freios', 'Performance', 'Motor'].map(
                        (cat) => (
                            <Link key={cat} href="/catalog" className={styles.footerLink}>
                                {cat}
                            </Link>
                        )
                    )}
                </div>

                <div>
                    <p className={styles.colTitle}>INFORMAÇÕES</p>
                    {['Sobre Nós', 'Política de Frete', 'Trocas e Devoluções', 'Contato'].map(
                        (link) => (
                            <Link key={link} href="#" className={styles.footerLink}>
                                {link}
                            </Link>
                        )
                    )}
                </div>

                <div>
                    <p className={styles.colTitle}>CONTATO</p>
                    <p className={styles.contactText}>contato@autohubstore.com</p>
                    <p className={styles.contactText}>(34) 99582-7133</p>
                    <p className={styles.contactText}>Seg–Sex 9h–18h</p>
                </div>
            </div>

            <div className={styles.footerBottom}>
                <div className={styles.footerBottomInner}>
                    <p className={styles.footerCopy}>
                        © 2026 AutoHubStore. Todos os direitos reservados.
                    </p>
                    <p className={styles.footerCopy}>CNPJ: 25.855.456/0001-77</p>
                </div>
            </div>
        </footer>
    )
}
