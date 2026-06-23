'use client'

import { useState } from 'react'
import styles from './ImageGallery.module.css'

export function ImageGallery({ images, name }: { images: string[]; name: string }) {
    const [selected, setSelected] = useState(0)

    return (
        <div className={styles.wrapper}>
            <div className={styles.thumbList}>
                {images.map((src, i) => (
                    <button
                        key={i}
                        onClick={() => setSelected(i)}
                        className={`${styles.thumb} ${i === selected ? styles.thumbActive : styles.thumbInactive}`}
                    >
                        <span className={styles.thumbLabel}>{i + 1}</span>
                    </button>
                ))}
            </div>

            <div className={styles.mainImage}>
                <p className={styles.mainLabel}>
                    {name} — Imagem {selected + 1}
                </p>
            </div>
        </div>
    )
}
