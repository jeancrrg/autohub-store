type SparklineProps = {
    data: number[]
    color: string
    width?: number
    height?: number
}

export function Sparkline({ data, color, width = 120, height = 36 }: SparklineProps) {
    const max = Math.max(...data)
    const min = Math.min(...data)
    const points = data.map((value, i) => {
        const x = (i / (data.length - 1)) * width
        const y = height - ((value - min) / (max - min || 1)) * (height - 4) - 2
        return [x, y] as const
    })
    const line = points.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)} ${y.toFixed(1)}`).join(' ')
    const area = `${line} L${width} ${height} L0 ${height} Z`
    const gradientId = `spark-${color.replace(/[^a-zA-Z0-9]/g, '')}`

    return (
        <svg width={width} height={height}>
            <defs>
                <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={color} stopOpacity="0.35" />
                    <stop offset="100%" stopColor={color} stopOpacity="0" />
                </linearGradient>
            </defs>
            <path d={area} fill={`url(#${gradientId})`} />
            <path d={line} fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
    )
}
