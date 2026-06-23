'use client'

import { useState } from 'react'
import type { Product } from '@/types/product'
import styles from './ProductTabs.module.css'

type Tab = 'descricao' | 'specs' | 'compat' | 'avaliacoes'

export function ProductTabs({ product }: { product: Product }) {
    const [activeTab, setActiveTab] = useState<Tab>('descricao')

    const tabs: { id: Tab; label: string }[] = [
        { id: 'descricao', label: 'Descrição' },
        { id: 'specs', label: 'Especificações' },
        { id: 'compat', label: 'Compatibilidade' },
        { id: 'avaliacoes', label: 'Avaliações' },
    ]

    return (
        <div className={styles.wrapper}>
            <div className={styles.tabList}>
                {tabs.map((tab) => (
                    <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id)}
                        className={`${styles.tab} ${activeTab === tab.id ? styles.tabActive : styles.tabInactive}`}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            <div className={styles.content}>
                {activeTab === 'descricao' && (
                    <p className={styles.description}>{product.description}</p>
                )}

                {activeTab === 'specs' && (
                    <table className={styles.specsTable}>
                        <tbody>
                            {Object.entries(product.specs).map(([key, value], i) => (
                                <tr
                                    key={key}
                                    className={i % 2 === 0 ? styles.rowEven : styles.rowOdd}
                                >
                                    <td className={styles.specKey}>{key}</td>
                                    <td className={styles.specVal}>{value}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}

                {activeTab === 'compat' && (
                    <div>
                        <p className={styles.compatText}>
                            Consulte a compatibilidade com o seu veículo:
                        </p>
                        <select className={styles.compatSelect}>
                            <option>Selecione o ano</option>
                            {[2024, 2023, 2022, 2021, 2020].map((y) => (
                                <option key={y}>{y}</option>
                            ))}
                        </select>
                    </div>
                )}

                {activeTab === 'avaliacoes' && (
                    <div>
                        <div className={styles.ratingRow}>
                            <p className={styles.ratingScore}>{product.stars}.0</p>
                            <div>
                                <p className={styles.ratingStars}>{'★'.repeat(product.stars)}</p>
                                <p className={styles.ratingCount}>{product.reviews} avaliações</p>
                            </div>
                        </div>
                        <p className={styles.ratingNote}>Avaliações detalhadas em breve.</p>
                    </div>
                )}
            </div>
        </div>
    )
}
