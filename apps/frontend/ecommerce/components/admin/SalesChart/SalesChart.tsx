import type { SalesBar } from '@/lib/data/admin'

const WIDTH = 760
const HEIGHT = 240
const PADDING = 28

export function SalesChart({ bars }: { bars: SalesBar[] }) {
    const values = bars.map((bar) => bar.value)
    const max = Math.max(...values) * 1.1
    const points = values.map((value, i) => {
        const x = PADDING + (i / (values.length - 1)) * (WIDTH - PADDING * 2)
        const y = HEIGHT - PADDING - (value / max) * (HEIGHT - PADDING * 2)
        return [x, y] as const
    })
    const line = points.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)} ${y.toFixed(1)}`).join(' ')
    const [lastX] = points[points.length - 1]
    const [firstX] = points[0]
    const area = `${line} L${lastX} ${HEIGHT - PADDING} L${firstX} ${HEIGHT - PADDING} Z`

    return (
        <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} style={{ width: '100%', height: 'auto', display: 'block' }}>
            <defs>
                <linearGradient id="salesGrad" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.28" />
                    <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
                </linearGradient>
            </defs>
            {[0, 0.25, 0.5, 0.75, 1].map((g) => (
                <line
                    key={g}
                    x1={PADDING}
                    x2={WIDTH - PADDING}
                    y1={PADDING + g * (HEIGHT - PADDING * 2)}
                    y2={PADDING + g * (HEIGHT - PADDING * 2)}
                    stroke="var(--border-subtle)"
                    strokeWidth="1"
                />
            ))}
            <path d={area} fill="url(#salesGrad)" />
            <path d={line} fill="none" stroke="var(--accent)" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
            {points.map(([x, y], i) => (
                <circle
                    key={bars[i].month}
                    cx={x}
                    cy={y}
                    r={i === points.length - 1 ? 4 : 0}
                    fill="var(--accent)"
                    stroke="var(--bg-surface)"
                    strokeWidth="2"
                />
            ))}
            {bars.map((bar, i) => (
                <text
                    key={bar.month}
                    x={PADDING + (i / (bars.length - 1)) * (WIDTH - PADDING * 2)}
                    y={HEIGHT - 8}
                    fill="var(--text-muted)"
                    fontSize="10"
                    fontFamily="var(--font-mono)"
                    textAnchor="middle"
                >
                    {bar.month}
                </text>
            ))}
        </svg>
    )
}
