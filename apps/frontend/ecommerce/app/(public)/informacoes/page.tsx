import Link from 'next/link'
import styles from './page.module.css'

const SECTIONS = [
    {
        id: 'sobre-nos',
        title: 'Sobre Nós',
        paragraphs: [
            'A AutoHubStore é um e-commerce especializado em peças e acessórios automotivos de alta performance, conectando entusiastas e profissionais às melhores marcas do mercado.',
            'Nosso catálogo é pensado para atender desde a manutenção do dia a dia até projetos de performance, sempre com foco em qualidade, compatibilidade e agilidade na entrega.',
        ],
    },
    {
        id: 'politica-de-frete',
        title: 'Política de Frete',
        paragraphs: [
            'O prazo de entrega varia conforme a região de destino e a modalidade de frete escolhida no fechamento do pedido.',
            'O valor do frete é calculado automaticamente no carrinho com base no CEP informado, considerando peso e dimensões dos itens selecionados.',
        ],
    },
    {
        id: 'trocas-e-devolucoes',
        title: 'Trocas e Devoluções',
        paragraphs: [
            'Você tem até 7 dias corridos após o recebimento para solicitar troca ou devolução, conforme o Código de Defesa do Consumidor.',
            'O produto deve ser devolvido na embalagem original, sem sinais de uso, acompanhado da nota fiscal de compra.',
        ],
    },
    {
        id: 'contato',
        title: 'Contato',
        paragraphs: [
            'E-mail: contato@autohubstore.com',
            'Telefone/WhatsApp: (34) 99582-7133',
            'Horário de atendimento: Segunda a sexta, das 9h às 18h.',
        ],
    },
]

export default function InformacoesPage() {
    return (
        <div className={styles.container}>
            <nav className={styles.breadcrumb}>
                <Link href="/" className={styles.breadcrumbLink}>
                    Início
                </Link>
                <span>/</span>
                <span className={styles.breadcrumbCurrent}>Informações</span>
            </nav>

            <h1 className={styles.title}>INFORMAÇÕES</h1>

            <nav className={styles.nav}>
                {SECTIONS.map((section) => (
                    <a key={section.id} href={`#${section.id}`} className={styles.navLink}>
                        {section.title}
                    </a>
                ))}
            </nav>

            {SECTIONS.map((section) => (
                <section key={section.id} id={section.id} className={styles.section}>
                    <h2 className={styles.sectionTitle}>{section.title}</h2>
                    {section.paragraphs.map((paragraph) => (
                        <p key={paragraph} className={styles.sectionText}>
                            {paragraph}
                        </p>
                    ))}
                </section>
            ))}
        </div>
    )
}
