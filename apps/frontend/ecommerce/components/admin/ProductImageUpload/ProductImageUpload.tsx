'use client'

import { useState } from 'react'
import { useUploadProductImages } from '@/hooks/useUploadProductImages'
import { Button } from '../Button/Button'
import { UploadIcon } from '../icons/Icons'
import styles from './ProductImageUpload.module.css'

const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024

export function ProductImageUpload({ productId, onDone }: { productId: string; onDone: () => void }) {
    const [files, setFiles] = useState<File[]>([])
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const uploadMutation = useUploadProductImages(productId)

    function handleFilesSelected(e: React.ChangeEvent<HTMLInputElement>) {
        const selected = Array.from(e.target.files ?? [])
        const oversized = selected.find((file) => file.size > MAX_FILE_SIZE_BYTES)
        if (oversized) {
            setErrorMessage(`Arquivo "${oversized.name}" excede 5MB.`)
            return
        }
        setErrorMessage(null)
        setFiles(selected)
    }

    function handleUpload() {
        uploadMutation.mutate(files, { onSuccess: onDone })
    }

    return (
        <div className={styles.wrapper}>
            <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                onChange={handleFilesSelected}
                className={styles.fileInput}
            />
            {errorMessage && <p className={styles.error}>{errorMessage}</p>}
            <Button
                onClick={handleUpload}
                disabled={files.length === 0 || uploadMutation.isPending}
                iconLeft={<UploadIcon size={16} />}
            >
                {uploadMutation.isPending ? 'ENVIANDO...' : 'ENVIAR IMAGENS'}
            </Button>
            <button onClick={onDone} className={styles.skipButton}>
                Pular por enquanto
            </button>
        </div>
    )
}
