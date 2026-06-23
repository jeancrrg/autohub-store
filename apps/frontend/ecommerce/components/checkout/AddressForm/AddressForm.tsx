'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Input } from '@/components/ui/Input/Input'
import styles from './AddressForm.module.css'

const schema = z.object({
    nome: z.string().min(3, 'Nome obrigatório'),
    cpf: z.string().min(11, 'CPF inválido'),
    cep: z.string().length(8, 'CEP deve ter 8 dígitos'),
    logradouro: z.string().min(3, 'Endereço obrigatório'),
    numero: z.string().min(1, 'Número obrigatório'),
    complemento: z.string().optional(),
    bairro: z.string().min(2, 'Bairro obrigatório'),
    cidade: z.string().min(2, 'Cidade obrigatória'),
    uf: z.string().length(2, 'UF deve ter 2 letras'),
})

export type AddressFormData = z.infer<typeof schema>

type AddressFormProps = {
    onSubmit: (data: AddressFormData) => void
}

export function AddressForm({ onSubmit }: AddressFormProps) {
    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm<AddressFormData>({ resolver: zodResolver(schema) })

    return (
        <form onSubmit={handleSubmit(onSubmit)} className={styles.form}>
            <div className={styles.grid2}>
                <div className={styles.colFull}>
                    <Input
                        label="Nome completo"
                        placeholder="Carlos Silva"
                        error={errors.nome?.message}
                        {...register('nome')}
                    />
                </div>
                <Input
                    label="CPF"
                    placeholder="000.000.000-00"
                    error={errors.cpf?.message}
                    {...register('cpf')}
                />
                <Input
                    label="CEP"
                    placeholder="00000-000"
                    error={errors.cep?.message}
                    {...register('cep')}
                />
                <div className={styles.colFull}>
                    <Input
                        label="Logradouro"
                        placeholder="Rua das Peças"
                        error={errors.logradouro?.message}
                        {...register('logradouro')}
                    />
                </div>
                <Input
                    label="Número"
                    placeholder="123"
                    error={errors.numero?.message}
                    {...register('numero')}
                />
                <Input
                    label="Complemento"
                    placeholder="Apto 45 (opcional)"
                    {...register('complemento')}
                />
                <Input
                    label="Bairro"
                    placeholder="Centro"
                    error={errors.bairro?.message}
                    {...register('bairro')}
                />
                <Input
                    label="Cidade"
                    placeholder="São Paulo"
                    error={errors.cidade?.message}
                    {...register('cidade')}
                />
                <div className={styles.fieldGroup}>
                    <label className={styles.label}>UF</label>
                    <select className={styles.select} {...register('uf')}>
                        <option value="">UF</option>
                        {['SP', 'RJ', 'MG', 'RS', 'PR', 'SC', 'BA', 'CE', 'PE', 'GO'].map((uf) => (
                            <option key={uf} value={uf}>
                                {uf}
                            </option>
                        ))}
                    </select>
                    {errors.uf && <p className={styles.fieldError}>{errors.uf.message}</p>}
                </div>
            </div>

            <button type="submit" className={styles.submitBtn}>
                CONTINUAR →
            </button>
        </form>
    )
}
